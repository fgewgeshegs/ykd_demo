package com.youkeda.exercise.claw.anime.source;

import com.youkeda.exercise.claw.anime.client.AniListClient;
import com.youkeda.exercise.claw.anime.model.Anime;
import com.youkeda.exercise.claw.anime.store.AnimeSubscriptionStore;
import com.youkeda.exercise.claw.campus.model.CampusConfig;
import com.youkeda.exercise.claw.campus.source.NotificationSource;
import com.youkeda.exercise.claw.scout.judge.Recommendation;
import com.youkeda.exercise.claw.scout.notifier.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "anime.enabled", havingValue = "true")
public class AnimeSeasonSource implements NotificationSource {

    private static final Logger log = LoggerFactory.getLogger(AnimeSeasonSource.class);

    private final AniListClient aniListClient;
    private final AnimeSubscriptionStore subscriptionStore;
    private final NotificationService notificationService;

    @Value("${anime.seasonRecommendation.enabled:true}")
    private boolean seasonRecommendationEnabled;

    public AnimeSeasonSource(AniListClient aniListClient,
                             AnimeSubscriptionStore subscriptionStore,
                             NotificationService notificationService) {
        this.aniListClient = aniListClient;
        this.subscriptionStore = subscriptionStore;
        this.notificationService = notificationService;
    }

    @Override
    public String getName() { return "ANIME_SEASON"; }

    @Override
    public boolean supports(CampusConfig config) {
        return seasonRecommendationEnabled;
    }

    @Override
    public void check() {
        try {
            log.info("===== AnimeSeasonSource 季度推荐 =====");

            // 获取当季新番
            List<Anime> seasonAnime = aniListClient.getCurrentSeasonAnime(1);
            if (seasonAnime.isEmpty()) {
                log.warn("未获取到当季新番数据");
                return;
            }

            // 阶段1：规则预筛
            List<Anime> prefiltered = seasonAnime.stream()
                .filter(a -> a.getAverageScore() >= 70 || a.getPopularity() > 50000)
                .limit(20)
                .toList();

            // 排除已订阅番剧
            List<Anime> subscriptions = subscriptionStore.listAll();
            List<Integer> subscribedIds = subscriptions.stream()
                .map(Anime::getAnilistId).toList();
            List<Anime> candidates = prefiltered.stream()
                .filter(a -> !subscribedIds.contains(a.getAnilistId()))
                .toList();

            if (candidates.isEmpty()) {
                log.info("无新番可推荐");
                return;
            }

            // 阶段2：LLM 个性化推荐（构建 prompt 调用 LLM）
            // 这里依赖现有的 LLM 调用基础设施（如 LLMClient 或 chat API）
            // 根据用户的 subscriptions 和 candidates，让 LLM 选出 top 5
            // 然后通过 notificationService.notify() 推送

            // 简化版：直接推候选列表前 5 名
            List<Anime> top5 = candidates.stream().limit(5).toList();

            // 获取当季信息
            String season = AniListClient.getCurrentSeason();
            int year = LocalDate.now(ZoneId.of("Asia/Tokyo")).getYear();

            // 构建推送内容
            String title = "📺 " + year + "年" + season + "季新番推荐";
            String content = top5.stream()
                .map(a -> "• " + a.getTitle() + (a.getGenres() != null ? " (" + String.join("/", a.getGenres()) + ")" : ""))
                .collect(Collectors.joining("\n"));

            // 通过 NotificationService 推送 (使用现有的 Recommendation 适配)
            notificationService.notify(List.of(new Recommendation(
                "anime_season_" + season,
                title,
                content,
                "",
                "以上是为你推荐的本季新番",
                "",
                1.0f,
                System.currentTimeMillis()
            )));

            log.info("季度推荐完成 | candidates={} | recommended={}",
                candidates.size(), top5.size());
        } catch (Exception e) {
            log.error("AnimeSeasonSource 异常", e);
        }
    }
}
