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

import java.util.List;

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

    @BeforeEach
    void setUp() {
        scheduler = new AnimeScheduler(animeSource, animeSeasonSource, subscriptionStore, translator);
    }

    private Anime anime(int id, String titleZh) {
        Anime a = new Anime(id, "Grand Blue Season 3", "ぐらんぶる", "", "RELEASING", 12, List.of(), 80, 100000);
        a.setTitleZh(titleZh);
        return a;
    }

    @Test
    void dailyCheckBackfillsEmptyTitleZh() {
        when(subscriptionStore.listAll()).thenReturn(List.of(
                anime(1, ""),                // 空 → 需回填
                anime(2, "碧蓝之海 第三季")  // 已填 → 跳过
        ));
        when(translator.translate(any())).thenReturn("中文名");

        scheduler.dailyCheck();

        verify(subscriptionStore).updateTitleZh(1, "中文名");
        verify(subscriptionStore, never()).updateTitleZh(eq(2), anyString());
    }

    @Test
    void dailyCheckSkipsBackfillWhenTranslationFails() {
        when(subscriptionStore.listAll()).thenReturn(List.of(anime(1, "")));
        when(translator.translate(any())).thenReturn(null);

        scheduler.dailyCheck();

        verify(subscriptionStore, never()).updateTitleZh(anyInt(), anyString());
    }
}
