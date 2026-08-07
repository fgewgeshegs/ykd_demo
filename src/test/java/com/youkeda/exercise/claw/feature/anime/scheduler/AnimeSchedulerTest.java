package com.youkeda.exercise.claw.feature.anime.scheduler;

import com.youkeda.exercise.claw.domain.anime.Anime;
import com.youkeda.exercise.claw.feature.anime.AnimeTitleTranslator;
import com.youkeda.exercise.claw.feature.anime.notification.AnimeSeasonSource;
import com.youkeda.exercise.claw.feature.anime.notification.AnimeSource;
import com.youkeda.exercise.claw.feature.anime.store.AnimeSubscriptionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnimeSchedulerTest {

    @Mock
    private AnimeSource animeSource;
    @Mock
    private AnimeSeasonSource animeSeasonSource;
    @Mock
    private AnimeSubscriptionStore subscriptionStore;
    @Mock
    private AnimeTitleTranslator translator;

    private AnimeScheduler scheduler;
    private Executor backfillExecutor;

    @BeforeEach
    void setUp() {
        // 默认同步执行：回填行为测试可直接断言（异步提交语义由专用测试覆盖）
        backfillExecutor = Runnable::run;
        scheduler = new AnimeScheduler(animeSource, animeSeasonSource, subscriptionStore, translator, backfillExecutor);
    }

    private Anime anime(int id, String titleZh) {
        Anime a = new Anime(id, "Grand Blue Season 3", "ぐらんぶる", "", "RELEASING", 12, List.of(), 80, 100000);
        a.setTitleZh(titleZh);
        return a;
    }

    @Test
    void dailyCheckBackfillsEmptyTitleZh() {
        when(subscriptionStore.listAll()).thenReturn(List.of(
                anime(1, ""),                // 空串 → 需回填
                anime(2, "碧蓝之海 第三季"),  // 已填 → 跳过
                anime(3, null)               // null → 需回填
        ));
        when(translator.translate(any())).thenReturn("中文名");

        scheduler.dailyCheck();

        verify(subscriptionStore).updateTitleZh(1, "中文名");
        verify(subscriptionStore).updateTitleZh(3, "中文名");
        verify(subscriptionStore, never()).updateTitleZh(eq(2), anyString());
    }

    @Test
    void dailyCheckSkipsBackfillWhenTranslationFails() {
        when(subscriptionStore.listAll()).thenReturn(List.of(anime(1, "")));
        when(translator.translate(any())).thenReturn(null);

        scheduler.dailyCheck();

        verify(subscriptionStore, never()).updateTitleZh(anyInt(), anyString());
    }

    @Test
    void onApplicationReadyRunsStartupCatchUp() {
        // 应用错过 08:00 窗口后启动：补跑 dailyCheck，播出检查 + 译名回填都要执行
        when(subscriptionStore.listAll()).thenReturn(List.of(anime(1, "")));
        when(translator.translate(any())).thenReturn("中文名");

        scheduler.onApplicationReady();

        verify(animeSource).check();
        verify(subscriptionStore).updateTitleZh(1, "中文名");
    }

    @Test
    void dailyCheckSubmitsBackfillToExecutorInsteadOfInline() {
        Executor mockExecutor = mock(Executor.class);
        scheduler = new AnimeScheduler(animeSource, animeSeasonSource, subscriptionStore, translator, mockExecutor);

        scheduler.dailyCheck();

        // 回填被提交给 executor 异步执行，而非在调用线程内联跑
        verify(mockExecutor).execute(any(Runnable.class));
        // 调用线程上回填逻辑并未执行（executor mock 不会真正跑任务）
        verify(subscriptionStore, never()).updateTitleZh(anyInt(), anyString());
    }

    @Test
    void dailyCheckCronIsConfigurableViaAnimeCron() throws NoSuchMethodException {
        // 防回归：dailyCheck 的 cron 必须可通过 anime.cron 配置（硬编码 08:00 会让错过窗口无法补偿测试）
        Method method = AnimeScheduler.class.getMethod("dailyCheck");
        Scheduled ann = method.getAnnotation(Scheduled.class);

        assertTrue(ann != null && ann.cron().contains("${anime.cron:"),
                "dailyCheck cron 应包含 ${anime.cron: 占位符，实际=" + (ann != null ? ann.cron() : "<无 @Scheduled>"));
    }

    @Test
    void dailyCheckDoesNotSubmitSecondBackfillWhileOneInFlight() {
        List<Runnable> submissions = new ArrayList<>();
        Executor capturing = submissions::add; // 只记录提交，不真正执行 → 第一个任务保持 in-flight
        scheduler = new AnimeScheduler(animeSource, animeSeasonSource, subscriptionStore, translator, capturing);

        scheduler.dailyCheck();
        scheduler.dailyCheck();

        // 第一次提交后回填仍在执行，第二次不应重复提交（08:00 cron 与启动补偿撞车时防重复 LLM 调用）
        assertEquals(1, submissions.size());
    }
}
