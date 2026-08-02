package com.youkeda.exercise.claw.feature.scout.notifier;

import com.youkeda.exercise.claw.feature.scout.judge.Recommendation;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.client.WechatILinkClient;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.user.WechatUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 推荐通知服务
 *
 * 将推荐结果格式化后通过微信发送给用户
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_REPORT_CHARS = 1800;
    private static final String REPORT_FOOTER = "---\n由 AI 信息猎手 Agent 自动生成";

    private final WechatILinkClient wechatClient;
    private final WechatUserManager userManager;
    private final RecommendationSummaryService summaryService;

    public NotificationService(WechatILinkClient wechatClient,
                               WechatUserManager userManager,
                               RecommendationSummaryService summaryService) {
        this.wechatClient = wechatClient;
        this.userManager = userManager;
        this.summaryService = summaryService;
    }

    /**
     * 信息猎手专用入口：先发送推荐明细，再发送本轮综合总结。
     *
     * <p>批次 3：原 {@code notify(List<Recommendation>)}（校园/番剧单条直推入口）已删除——
     * campus/anime 改走 {@code NotificationEventPublisher} 统一事件总线，
     * 本服务回归信息猎手报告专属（格式化带「🔍 信息猎手」报告头尾）。
     */
    public void notifyWithSummary(List<Recommendation> recommendations) {
        deliver(recommendations);
    }

    /** 后台信息猎手真正失败时，只发送简短且可操作的提示。 */
    public void notifyFailure() {
        String ownerUserId = userManager.getOwnerUserId();
        if (ownerUserId == null || ownerUserId.isBlank()) {
            log.error("信息猎手失败通知发送失败 | 未找到微信收件人");
            return;
        }
        try {
            if (!wechatClient.sendTextMessage(
                    ownerUserId, "信息猎手本次运行失败，请稍后重试。")) {
                log.error("信息猎手失败通知发送失败");
            }
        } catch (Exception e) {
            log.error("信息猎手失败通知发送异常", e);
        }
    }

    private void deliver(List<Recommendation> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            log.info("无推荐结果，跳过推送");
            return;
        }

        List<String> reportChunks = formatReportChunks(recommendations);
        String ownerUserId = userManager.getOwnerUserId();
        if (ownerUserId == null || ownerUserId.isBlank()) {
            log.error("推荐推送失败 | 未找到微信收件人");
            return;
        }

        try {
            for (int i = 0; i < reportChunks.size(); i++) {
                if (!wechatClient.sendTextMessage(ownerUserId, reportChunks.get(i))) {
                    log.error("推荐明细第 {}/{} 段发送失败",
                            i + 1, reportChunks.size());
                    return;
                }
            }

            String summary = summaryService.summarize(recommendations);
            if (summary != null && !summary.isBlank()
                    && !wechatClient.sendTextMessage(ownerUserId, summary)) {
                log.error("推荐明细已发送，但综合总结发送失败");
            }
            log.info("推荐推送成功 | count={} | chunks={}",
                    recommendations.size(), reportChunks.size());
        } catch (Exception e) {
            log.error("推荐推送失败", e);
        }
    }

    /**
     * 格式化推荐报告
     */
    private List<String> formatReportChunks(List<Recommendation> recs) {
        List<Recommendation> strong = recs.stream()
                .filter(rec -> rec.tier() == Recommendation.Tier.STRONG)
                .toList();
        List<Recommendation> worthScanning = recs.stream()
                .filter(rec -> rec.tier() == Recommendation.Tier.DISCOVERY)
                .toList();

        List<Recommendation> ordered = new ArrayList<>(strong.size() + worthScanning.size());
        ordered.addAll(strong);
        ordered.addAll(worthScanning);

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        current.append("🔍 信息猎手发现 ").append(recs.size())
                .append(" 条有价值的信息：\n\n");

        int index = 1;
        boolean hasEntry = false;
        Recommendation.Tier activeTier = null;

        for (Recommendation recommendation : ordered) {
            String sectionHeader = activeTier == recommendation.tier()
                    ? ""
                    : tierHeader(recommendation.tier());
            String entry = formatRecommendation(index, recommendation);

            if (hasEntry && current.length() + sectionHeader.length()
                    + entry.length() + REPORT_FOOTER.length() > MAX_REPORT_CHARS) {
                chunks.add(current.toString().stripTrailing());
                current = new StringBuilder("🔍 信息猎手推荐（续）\n\n");
                hasEntry = false;
                activeTier = null;
                sectionHeader = tierHeader(recommendation.tier());
            }

            current.append(sectionHeader).append(entry);
            activeTier = recommendation.tier();
            hasEntry = true;
            index++;
        }

        current.append(REPORT_FOOTER);
        chunks.add(current.toString());
        return chunks;
    }

    private String tierHeader(Recommendation.Tier tier) {
        return tier == Recommendation.Tier.STRONG
                ? "🔥 强推荐\n\n"
                : "👀 值得扫一眼\n\n";
    }

    private String formatRecommendation(int index, Recommendation recommendation) {
        StringBuilder sb = new StringBuilder();
        sb.append(index).append(". ").append(recommendation.title()).append("\n");
        sb.append("   📝 ").append(recommendation.summary()).append("\n");
        if (recommendation.reason() != null && !recommendation.reason().isBlank()) {
            sb.append("   💡 ").append(recommendation.reason()).append("\n");
        }
        if (recommendation.suggestion() != null && !recommendation.suggestion().isBlank()) {
            sb.append("   🎯 ").append(recommendation.suggestion()).append("\n");
        }
        if (recommendation.source() != null && !recommendation.source().isBlank()) {
            sb.append("   🔗 ").append(recommendation.source()).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }
}
