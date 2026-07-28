package com.youkeda.exercise.claw.scout.notifier;

import com.youkeda.exercise.claw.scout.ScoutProperties;
import com.youkeda.exercise.claw.scout.judge.Recommendation;
import com.youkeda.exercise.claw.scout.processor.InformationIdentity;
import com.youkeda.exercise.claw.wechat.client.WechatILinkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    private final WechatILinkClient wechatClient;
    private final ScoutDeliveryStore deliveryStore;
    private final ScoutProperties props;

    public NotificationService(WechatILinkClient wechatClient,
                               ScoutDeliveryStore deliveryStore,
                               ScoutProperties props) {
        this.wechatClient = wechatClient;
        this.deliveryStore = deliveryStore;
        this.props = props;
    }

    /**
     * 推送推荐结果给用户
     */
    public void notify(String userId, List<Recommendation> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            log.info("无推荐结果，跳过推送 | userId={}", userId);
            return;
        }

        long now = System.currentTimeMillis();
        long cooldownStart = Instant.ofEpochMilli(now)
                .minus(Math.max(1, props.getDeliveryCooldownDays()), ChronoUnit.DAYS)
                .toEpochMilli();
        List<Recommendation> eligible = new ArrayList<>();
        for (Recommendation recommendation : recommendations) {
            String itemKey = InformationIdentity.stableKey(
                    recommendation.source(), recommendation.title());
            if (!deliveryStore.wasDeliveredSince(userId, itemKey, cooldownStart)) {
                eligible.add(recommendation);
            }
        }

        if (eligible.isEmpty()) {
            log.info("推荐均在冷却期内，跳过重复推送 | userId={} | count={}",
                    userId, recommendations.size());
            return;
        }

        String report = formatReport(eligible);

        try {
            wechatClient.sendTextMessage(userId, report);
            for (Recommendation recommendation : eligible) {
                String itemKey = InformationIdentity.stableKey(
                        recommendation.source(), recommendation.title());
                deliveryStore.markDelivered(userId, itemKey, now);
            }
            log.info("推荐推送成功 | userId={} | count={} | suppressed={}",
                    userId, eligible.size(), recommendations.size() - eligible.size());
        } catch (Exception e) {
            log.error("推荐推送失败 | userId={}", userId, e);
        }
    }

    /**
     * 格式化推荐报告
     */
    private String formatReport(List<Recommendation> recs) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔍 信息猎手发现 ").append(recs.size()).append(" 条有价值的信息：\n\n");

        for (int i = 0; i < recs.size(); i++) {
            Recommendation r = recs.get(i);
            sb.append(i + 1).append(". ").append(r.title()).append("\n");
            sb.append("   📝 ").append(r.summary()).append("\n");
            if (r.reason() != null && !r.reason().isBlank()) {
                sb.append("   💡 ").append(r.reason()).append("\n");
            }
            if (r.suggestion() != null && !r.suggestion().isBlank()) {
                sb.append("   🎯 ").append(r.suggestion()).append("\n");
            }
            if (r.source() != null && !r.source().isBlank()) {
                sb.append("   🔗 ").append(r.source()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("---\n");
        sb.append("由 AI 信息猎手 Agent 自动生成");

        return sb.toString();
    }
}
