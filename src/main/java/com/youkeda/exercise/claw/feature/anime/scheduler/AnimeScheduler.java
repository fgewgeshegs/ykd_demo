package com.youkeda.exercise.claw.feature.anime.scheduler;

import com.youkeda.exercise.claw.domain.anime.Anime;
import com.youkeda.exercise.claw.feature.anime.AnimeTitleTranslator;
import com.youkeda.exercise.claw.feature.anime.notification.AnimeSource;
import com.youkeda.exercise.claw.feature.anime.notification.AnimeSeasonSource;
import com.youkeda.exercise.claw.feature.anime.store.AnimeSubscriptionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(name = "anime.enabled", havingValue = "true")
public class AnimeScheduler {

    private static final Logger log = LoggerFactory.getLogger(AnimeScheduler.class);

    private final AnimeSource animeSource;
    private final AnimeSeasonSource animeSeasonSource;
    private final AnimeSubscriptionStore subscriptionStore;
    private final AnimeTitleTranslator translator;
    private final Executor backfillExecutor;
    /** 回填在飞标志：同一时刻只允许一个回填任务（08:00 cron 与启动补偿可能先后触发，防并发重复 LLM 调用） */
    private final AtomicBoolean backfillInFlight = new AtomicBoolean(false);

    public AnimeScheduler(AnimeSource animeSource, AnimeSeasonSource animeSeasonSource,
                          AnimeSubscriptionStore subscriptionStore, AnimeTitleTranslator translator,
                          @Qualifier("animeBackfillExecutor") Executor backfillExecutor) {
        this.animeSource = animeSource;
        this.animeSeasonSource = animeSeasonSource;
        this.subscriptionStore = subscriptionStore;
        this.translator = translator;
        this.backfillExecutor = backfillExecutor;
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

        // 3. 回填空缺的中文译名（异步提交：LLM 慢调用不阻塞调度线程，见 animeBackfillExecutor）
        submitBackfill();
    }

    /**
     * 启动补偿：错过 08:00 每日检查窗口（应用宕机/重启/停机）时，应用就绪后补跑一次。
     *
     * <p>背景：{@link #dailyCheck()} 是 08:00 单窗口 cron，Spring 错过不补跑。应用
     * 08:00 不在线时，当天的播出检查（生成提醒任务）与译名回填都会静默跳过——8-7
     * 实测回填漏跑就是这个原因。就绪后补跑全程幂等（插入/回填均跳过已处理项），重复无害。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("===== AnimeScheduler 启动补偿检查 =====");
        try {
            dailyCheck();
        } catch (Exception e) {
            // 与 @Scheduled 隐含吞异常语义对齐：启动路径上不让补偿失败中断应用就绪
            log.error("AnimeScheduler 启动补偿检查异常", e);
        }
    }

    /**
     * 异步提交译名回填任务。
     *
     * <p>回填的 LLM 调用是慢阻塞操作，在独立线程执行，不占 {@code @Scheduled} 单线程
     * 调度器（避免推迟每分钟的播出提醒）也不拖慢应用就绪。in-flight 标志保证同一时刻
     * 只有一个回填任务：08:00 cron 与启动补偿先后触发时不会并发重复调用 LLM。
     */
    private void submitBackfill() {
        if (!backfillInFlight.compareAndSet(false, true)) {
            log.info("译名回填已在执行，跳过本次提交");
            return;
        }
        backfillExecutor.execute(() -> {
            try {
                backfillTitleZh();
            } finally {
                backfillInFlight.set(false);
            }
        });
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
