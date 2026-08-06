package com.youkeda.exercise.claw.feature.anime.notification;
import com.youkeda.exercise.claw.notification.NotificationSource;

import com.youkeda.exercise.claw.feature.anime.client.AniListClient;
import com.youkeda.exercise.claw.domain.anime.Anime;
import com.youkeda.exercise.claw.domain.anime.AnimeEpisode;
import com.youkeda.exercise.claw.feature.anime.store.AnimeScheduleStore;
import com.youkeda.exercise.claw.feature.anime.store.AnimeSubscriptionStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "anime.enabled", havingValue = "true")
public class AnimeSource implements NotificationSource {

    private static final Logger log = LoggerFactory.getLogger(AnimeSource.class);

    private final AniListClient aniListClient;
    private final AnimeSubscriptionStore subscriptionStore;
    private final AnimeScheduleStore scheduleStore;

    public AnimeSource(AniListClient aniListClient,
                       AnimeSubscriptionStore subscriptionStore,
                       AnimeScheduleStore scheduleStore) {
        this.aniListClient = aniListClient;
        this.subscriptionStore = subscriptionStore;
        this.scheduleStore = scheduleStore;
    }

    @Override
    public String getName() { return "ANIME"; }

    @Override
    public void check() {
        try {
            List<Anime> airingAnime = subscriptionStore.getCurrentlyAiring();
            if (airingAnime.isEmpty()) {
                log.debug("无正在播出的番剧，跳过检查");
                return;
            }

            // 阶段 1：记录每部连载番的下一集播出信息（幂等 upsert）
            for (Anime anime : airingAnime) {
                try {
                    AnimeEpisode episode = aniListClient.getAiringSchedule(anime.getAnilistId());
                    if (episode == null || episode.getEpisode() <= 0) {
                        continue;
                    }
                    scheduleStore.insertOrIgnoreEpisode(
                            anime.getAnilistId(), episode.getEpisode(), episode.getAiringAt());
                } catch (Exception e) {
                    log.warn("检查番剧失败 | id={} | title={}", anime.getAnilistId(), anime.getTitle(), e);
                }
            }

            // 阶段 2：对未来 24h 内播出且未通知的集，生成提醒任务（幂等，重复调用不重建）
            long now = System.currentTimeMillis() / 1000;
            long future24h = now + 24 * 3600;
            List<AnimeEpisode> upcoming = scheduleStore.getUpcomingEpisodes(now, future24h);
            for (AnimeEpisode episode : upcoming) {
                try {
                    long remindTime = episode.getAiringAt() - 15 * 60; // 提前 15 分钟
                    scheduleStore.createReminderTask(
                            episode.getAnilistId(), episode.getEpisode(), remindTime, episode.getAiringAt());
                    log.info("已生成提醒任务 | id={} | episode={} | remindTime={}",
                            episode.getAnilistId(), episode.getEpisode(), remindTime);
                } catch (Exception e) {
                    // 单集失败不中断其余集（与阶段 1 逐番剧 try/catch 对齐）
                    log.warn("生成提醒任务失败 | id={} | episode={}",
                            episode.getAnilistId(), episode.getEpisode(), e);
                }
            }
            log.info("AnimeSource 检查完成 | airingCount={} | planned={}", airingAnime.size(), upcoming.size());
        } catch (Exception e) {
            log.error("AnimeSource 检查异常", e);
        }
    }
}
