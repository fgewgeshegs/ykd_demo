package com.youkeda.exercise.claw.tool.anime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.runtime.ToolExecutionContext;
import com.youkeda.exercise.claw.agent.runtime.ToolRegistry;
import com.youkeda.exercise.claw.domain.anime.Anime;
import com.youkeda.exercise.claw.domain.anime.AnimeEpisode;
import com.youkeda.exercise.claw.feature.anime.AnimeTitleTranslator;
import com.youkeda.exercise.claw.feature.anime.client.AniListClient;
import com.youkeda.exercise.claw.feature.anime.store.AnimeSubscriptionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnimeSubscribeToolTest {

    @Mock
    private AniListClient aniListClient;
    @Mock
    private AnimeSubscriptionStore subscriptionStore;
    @Mock
    private AnimeTitleTranslator translator;

    private AnimeSubscribeTool tool;
    private ObjectMapper om;

    @BeforeEach
    void setUp() {
        om = new ObjectMapper();
        tool = new AnimeSubscribeTool(aniListClient, subscriptionStore, translator, new ToolRegistry(), om);
    }

    private String execute(String json) throws Exception {
        return tool.execute(json, ToolExecutionContext.EMPTY);
    }

    private Anime anime(int id) {
        return new Anime(id, "Grand Blue Season 3", "ぐらんぶる", "", "RELEASING", 12, List.of(), 80, 100000);
    }

    @Test
    @DisplayName("subscribe：翻译中文名落库并回复用中文名")
    void subscribePersistsTitleZhAndRepliesWithChineseName() throws Exception {
        when(aniListClient.getAnimeById(210031)).thenReturn(anime(210031));
        when(translator.translate(any())).thenReturn("碧蓝之海 第三季");

        String result = execute("{\"action\":\"subscribe\",\"animeId\":210031}");

        ArgumentCaptor<Anime> captor = ArgumentCaptor.forClass(Anime.class);
        verify(subscriptionStore).subscribe(captor.capture());
        assertEquals("碧蓝之海 第三季", captor.getValue().getTitleZh(), "订阅落库应带中文译名");
        assertTrue(result.contains("碧蓝之海 第三季"), "回复应使用中文译名，实际=" + result);
    }

    @Test
    @DisplayName("subscribe：翻译失败回退罗马音")
    void subscribeFallsBackToRomajiWhenTranslationFails() throws Exception {
        when(aniListClient.getAnimeById(210031)).thenReturn(anime(210031));
        when(translator.translate(any())).thenReturn(null);

        String result = execute("{\"action\":\"subscribe\",\"animeId\":210031}");

        verify(subscriptionStore).subscribe(any());
        assertTrue(result.contains("Grand Blue Season 3"), "翻译失败应回退罗马音，实际=" + result);
    }

    @Test
    @DisplayName("subscribe：translator 回显日文名（非罗马音）时不当作中文译名落库")
    void subscribeRejectsJapaneseTitleWhenTranslatorCannotTranslate() throws Exception {
        when(aniListClient.getAnimeById(210031)).thenReturn(anime(210031));
        // LLM 无法翻译时按 prompt 回显日文名（非罗马音）——不满足「中文译名」契约，不应落库
        when(translator.translate(any())).thenReturn("ぐらんぶる");

        String result = execute("{\"action\":\"subscribe\",\"animeId\":210031}");

        ArgumentCaptor<Anime> captor = ArgumentCaptor.forClass(Anime.class);
        verify(subscriptionStore).subscribe(captor.capture());
        assertNull(captor.getValue().getTitleZh(), "日文名（非罗马音回显）不应作为中文译名落库");
        assertTrue(result.contains("Grand Blue Season 3"), "应回退罗马音，实际=" + result);
    }

    @Test
    @DisplayName("list：列表输出含 title_zh")
    void handleListIncludesTitleZh() throws Exception {
        Anime a = anime(210031);
        a.setTitleZh("碧蓝之海 第三季");
        when(subscriptionStore.listAll()).thenReturn(List.of(a));

        String result = execute("{\"action\":\"list\"}");

        assertTrue(result.contains("碧蓝之海 第三季"), "列表应含 title_zh，实际=" + result);
    }

    @Test
    @DisplayName("schedule：优先用已订阅中文译名")
    void handleSchedulePrefersSubscribedTitleZh() throws Exception {
        Anime subscribed = anime(210031);
        subscribed.setTitleZh("碧蓝之海 第三季");
        when(aniListClient.getAnimeById(210031)).thenReturn(anime(210031));
        when(subscriptionStore.findByAnilistId(210031)).thenReturn(subscribed);
        when(aniListClient.getAiringSchedule(210031)).thenReturn(null); // 走「暂无排期」分支，标题仍用中文名

        String result = execute("{\"action\":\"schedule\",\"animeId\":210031}");

        assertTrue(result.contains("碧蓝之海 第三季"), "排期回复应使用已订阅中文译名，实际=" + result);
    }

    @Test
    @DisplayName("schedule：有排期时回复含中文译名与集数")
    void handleScheduleWithAiringScheduleShowsChineseTitleAndEpisode() throws Exception {
        Anime subscribed = anime(210031);
        subscribed.setTitleZh("碧蓝之海 第三季");
        when(aniListClient.getAnimeById(210031)).thenReturn(anime(210031));
        when(subscriptionStore.findByAnilistId(210031)).thenReturn(subscribed);
        // 有排期：未来 24h 播出第 5 集
        long future = System.currentTimeMillis() / 1000 + 86400;
        when(aniListClient.getAiringSchedule(210031))
                .thenReturn(new AnimeEpisode(210031, 5, future));

        String result = execute("{\"action\":\"schedule\",\"animeId\":210031}");

        assertTrue(result.contains("碧蓝之海 第三季"), "排期回复应含已订阅中文译名，实际=" + result);
        assertTrue(result.contains("第 5 集"), "排期回复应含集数，实际=" + result);
        assertTrue(result.contains("（北京时间）"), "排期回复应含播出时间，实际=" + result);
    }
}
