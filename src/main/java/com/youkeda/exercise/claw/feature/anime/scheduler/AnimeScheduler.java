package com.youkeda.exercise.claw.feature.anime.scheduler;

import com.youkeda.exercise.claw.domain.anime.Anime;
import com.youkeda.exercise.claw.feature.anime.AnimeTitleTranslator;
import com.youkeda.exercise.claw.feature.anime.notification.AnimeSource;
import com.youkeda.exercise.claw.feature.anime.notification.AnimeSeasonSource;
import com.youkeda.exercise.claw.feature.anime.store.AnimeSubscriptionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
@ConditionalOnProperty(name = "anime.enabled", havingValue = "true")
public class AnimeScheduler {

    private static final Logger log = LoggerFactory.getLogger(AnimeScheduler.class);

    private final AnimeSource animeSource;
    private final AnimeSeasonSource animeSeasonSource;
    private final AnimeSubscriptionStore subscriptionStore;
    private final AnimeTitleTranslator translator;

    public AnimeScheduler(AnimeSource animeSource, AnimeSeasonSource animeSeasonSource,
                          AnimeSubscriptionStore subscriptionStore, AnimeTitleTranslator translator) {
        this.animeSource = animeSource;
        this.animeSeasonSource = animeSeasonSource;
        this.subscriptionStore = subscriptionStore;
        this.translator = translator;
    }

    /** 每天 08:00 检查播出更新 */
    @Scheduled(cron = "0 0 8 * * *")
    public void dailyCheck() {
        log.info("===== AnimeScheduler 每日检查 =====");

        // 1. 检查播出更新（核心提醒链路，不依赖回填结果）
        animeSource.check();

        // 2. 每季第一天触发新番推荐
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Tokyo"));
        if (today.getDayOfMonth() == 1
            && (today.getMonthValue() == 1 || today.getMonthValue() == 4
                || today.getMonthValue() == 7 || today.getMonthValue() == 10)) {
            animeSeasonSource.check();
        }

        // 3. 回填空缺的中文译名（LLM 故障不拖延核心链路；失败留空，次日重试）
        backfillTitleZh();
    }

    /** 为 title_zh 为空的订阅回填中文译名（LLM 失败则留空，次日重试） */
    private void backfillTitleZh() {
        List<Anime> subscriptions = subscriptionStore.listAll();
        for (Anime anime : subscriptions) {
            if (anime.getTitleZh() != null && !anime.getTitleZh().isBlank()) {
                continue;
            }
            try {
                String titleZh = translator.translate(anime);
                if (titleZh != null && !titleZh.isBlank()) {
                    subscriptionStore.updateTitleZh(anime.getAnilistId(), titleZh);
                    log.info("回填中文译名 | title={} | titleZh={}", anime.getTitle(), titleZh);
                }
            } catch (Exception e) {
                log.warn("回填译名失败 | title={}", anime.getTitle(), e);
            }
        }
    }
}
