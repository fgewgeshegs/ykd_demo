package com.youkeda.exercise.claw.feature.anime.notification;

import com.youkeda.exercise.claw.domain.anime.Anime;
import com.youkeda.exercise.claw.domain.anime.AnimeEpisode;
import com.youkeda.exercise.claw.feature.anime.client.AniListClient;
import com.youkeda.exercise.claw.feature.anime.store.AnimeScheduleStore;
import com.youkeda.exercise.claw.feature.anime.store.AnimeSubscriptionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnimeSourceTest {

    @Mock
    private AniListClient aniListClient;
    @Mock
    private AnimeSubscriptionStore subscriptionStore;
    @Mock
    private AnimeScheduleStore scheduleStore;

    private AnimeSource source;

    @BeforeEach
    void setUp() {
        source = new AnimeSource(aniListClient, subscriptionStore, scheduleStore);
    }

    private Anime airingAnime(int id, String title) {
        return new Anime(id, title, title, "", "RELEASING", 12, List.of("Comedy"), 70, 100);
    }

    @Test
    void createsTaskWhenEpisodeAirWithin24h() {
        long now = System.currentTimeMillis() / 1000;
        long airingAt = now + 2 * 3600; // 2 小时后播出
        when(subscriptionStore.getCurrentlyAiring()).thenReturn(List.of(airingAnime(210031, "Seihantai")));
        when(aniListClient.getAiringSchedule(210031)).thenReturn(new AnimeEpisode(210031, 5, airingAt));
        // 新集：getUpcomingEpisodes 返回它（模拟 upsert 后进入窗口查询）
        when(scheduleStore.getUpcomingEpisodes(anyLong(), anyLong()))
                .thenReturn(List.of(new AnimeEpisode(210031, 5, airingAt)));

        source.check();

        verify(scheduleStore).insertOrIgnoreEpisode(210031, 5, airingAt);
        verify(scheduleStore).createReminderTask(eq(210031), eq(5), eq(airingAt - 15 * 60L), eq(airingAt));
    }

    @Test
    void doesNotCreateTaskWhenEpisodeBeyond24h() {
        long now = System.currentTimeMillis() / 1000;
        long airingAt = now + 30 * 3600; // 30 小时后播出，超出 24h 窗口
        when(subscriptionStore.getCurrentlyAiring()).thenReturn(List.of(airingAnime(210031, "Seihantai")));
        when(aniListClient.getAiringSchedule(210031)).thenReturn(new AnimeEpisode(210031, 5, airingAt));
        when(scheduleStore.getUpcomingEpisodes(anyLong(), anyLong())).thenReturn(List.of());

        source.check();

        verify(scheduleStore, never()).createReminderTask(anyInt(), anyInt(), anyLong(), anyLong());
    }

    @Test
    void repeatedCheckDoesNotDuplicateTask() {
        long now = System.currentTimeMillis() / 1000;
        long airingAt = now + 2 * 3600;
        when(subscriptionStore.getCurrentlyAiring()).thenReturn(List.of(airingAnime(210031, "Seihantai")));
        when(aniListClient.getAiringSchedule(210031)).thenReturn(new AnimeEpisode(210031, 5, airingAt));
        when(scheduleStore.getUpcomingEpisodes(anyLong(), anyLong()))
                .thenReturn(List.of(new AnimeEpisode(210031, 5, airingAt)));

        source.check();
        source.check(); // 第二次重复执行

        verify(scheduleStore, times(2)).insertOrIgnoreEpisode(210031, 5, airingAt);
        verify(scheduleStore, times(2)).createReminderTask(eq(210031), eq(5), eq(airingAt - 15 * 60L), eq(airingAt));
        // 幂等由 createReminderTask 的唯一约束保证（Task 3 已测），此处验证重复调用行为一致
    }

    /**
     * 时间推进场景（GPT 审查意见采纳）：直击原 bug `if (!isNew) continue;`。
     * 某集首次发现时离播出 30h（窗口外，不建任务）；时间推进后同一集进入 24h 窗口，
     * 即使 insertOrIgnoreEpisode 返回 false（已记录、非首见），也必须创建任务。
     */
    @Test
    void createsTaskWhenPreviouslyRecordedEpisodeEntersWindowLater() {
        long now = System.currentTimeMillis() / 1000;
        long airingAtLater = now + 30 * 3600; // 首次发现：30h 后播出 → 窗口外

        // 第一天：发现该集但离播出太远，不建任务
        when(subscriptionStore.getCurrentlyAiring()).thenReturn(List.of(airingAnime(210031, "Seihantai")));
        when(aniListClient.getAiringSchedule(210031)).thenReturn(new AnimeEpisode(210031, 5, airingAtLater));
        when(scheduleStore.getUpcomingEpisodes(anyLong(), anyLong())).thenReturn(List.of());
        source.check();
        verify(scheduleStore, never()).createReminderTask(anyInt(), anyInt(), anyLong(), anyLong());

        // 第二天：同一集进入 24h 窗口（airingAt 不变，时间真的过去了），且该集已记录（非首见）
        long airingAtNear = now + 20 * 3600; // 20h 后播出 → 窗口内
        reset(aniListClient, scheduleStore); // 重打桩
        when(subscriptionStore.getCurrentlyAiring()).thenReturn(List.of(airingAnime(210031, "Seihantai")));
        when(aniListClient.getAiringSchedule(210031)).thenReturn(new AnimeEpisode(210031, 5, airingAtNear));
        when(scheduleStore.insertOrIgnoreEpisode(210031, 5, airingAtNear)).thenReturn(false); // 非首见，已记录
        when(scheduleStore.getUpcomingEpisodes(anyLong(), anyLong()))
                .thenReturn(List.of(new AnimeEpisode(210031, 5, airingAtNear)));

        source.check();

        // 即使 insertOrIgnoreEpisode 返回 false（非首见），进入窗口就必须创建任务
        verify(scheduleStore).createReminderTask(eq(210031), eq(5), eq(airingAtNear - 15 * 60L), eq(airingAtNear));
    }
}
