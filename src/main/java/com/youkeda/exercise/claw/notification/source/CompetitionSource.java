package com.youkeda.exercise.claw.notification.source;
import com.youkeda.exercise.claw.notification.NotificationSource;

import com.youkeda.exercise.claw.campus.classifier.CompetitionClassifier;
import com.youkeda.exercise.claw.campus.collector.CompetitionCollector;
import com.youkeda.exercise.claw.campus.model.NotificationItem;
import com.youkeda.exercise.claw.campus.policy.DefaultPolicy;
import com.youkeda.exercise.claw.campus.policy.NotificationPolicy;
import com.youkeda.exercise.claw.campus.policy.rule.CompetitionRules;
import com.youkeda.exercise.claw.campus.store.CampusNotificationStore;
import com.youkeda.exercise.claw.campus.store.PendingAskStore;
import com.youkeda.exercise.claw.scout.judge.Recommendation;
import com.youkeda.exercise.claw.scout.notifier.NotificationService;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.user.WechatUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "campus.enabled", havingValue = "true")
public class CompetitionSource implements NotificationSource {

    private static final Logger log = LoggerFactory.getLogger(CompetitionSource.class);
    private static final CompetitionRules RULES = new CompetitionRules();

    private final CompetitionCollector collector;
    private final CampusNotificationStore store;
    private final CompetitionClassifier classifier;
    private final DefaultPolicy policy;
    private final PendingAskStore pendingAskStore;
    private final NotificationService notificationService;
    private final WechatUserManager userManager;

    public CompetitionSource(CompetitionCollector collector,
                             CampusNotificationStore store,
                             CompetitionClassifier classifier,
                             DefaultPolicy policy,
                             PendingAskStore pendingAskStore,
                             NotificationService notificationService,
                             WechatUserManager userManager) {
        this.collector = collector;
        this.store = store;
        this.classifier = classifier;
        this.policy = policy;
        this.pendingAskStore = pendingAskStore;
        this.notificationService = notificationService;
        this.userManager = userManager;
    }

    @Override
    public String getName() { return "COMPETITION"; }

    @Override
    public void check() {
        log.info("===== CompetitionSource 检查 =====");

        // 1. 采集
        List<NotificationItem> items = collector.collect();
        if (items.isEmpty()) return;

        // 2. 去重
        List<NotificationItem> newItems = store.deduplicate(items);
        if (newItems.isEmpty()) return;

        // 3. 逐条分类→决策→推送
        for (NotificationItem item : newItems) {
            processItem(item);
        }

        log.info("===== CompetitionSource 完成 | new={} =====", newItems.size());
    }

    private void processItem(NotificationItem item) {
        // 分类
        String type = classifier.classify(item);
        if (type == null) {
            log.debug("非比赛通知，跳过 | title={}", item.getTitle());
            return; // 不是比赛通知
        }
        item.setType(type);

        // 更新存储
        store.update(item);

        // 决策
        NotificationPolicy.Decision decision = policy.decide(item, null, RULES);

        switch (decision) {
            case NOTIFY -> notifyUser(item);
            case ASK -> askUser(item);
            case SKIP -> log.debug("用户已忽略比赛 {}，跳过", type);
            case IGNORE -> log.debug("忽略比赛通知");
        }
    }

    private void notifyUser(NotificationItem item) {
        String userId = userManager.getOwnerUserId();
        String typeDisplayName = typeDisplayName(item.getType());

        String message = "🏆 比赛提醒\n"
            + "「" + item.getTitle() + "」\n"
            + "类型: " + typeDisplayName + "\n"
            + (item.getPublishAt() != null && !item.getPublishAt().isBlank()
                ? "发布日期: " + item.getPublishAt() + "\n" : "")
            + "详情: " + item.getUrl();

        notificationService.notify(List.of(new Recommendation(
            "comp_" + item.getType(),
            item.getTitle(),
            typeDisplayName + "比赛通知",
            item.getClassifierReason(),
            message,
            item.getUrl(),
            1.0f,
            System.currentTimeMillis()
        )));
        log.info("比赛通知已推送 | title={}", item.getTitle());
    }

    private void askUser(NotificationItem item) {
        String userId = userManager.getOwnerUserId();
        String typeDisplayName = typeDisplayName(item.getType());

        String question = "检测到新的「" + typeDisplayName + "」比赛通知："
            + item.getTitle() + "，\n需要关注这类比赛吗？（回复 需要/不需要）";

        notificationService.notify(List.of(new Recommendation(
            "ask_comp_" + item.getType(),
            item.getTitle() + " - 是否需要关注",
            "需要用户确认",
            item.getClassifierReason(),
            question,
            null,
            0.8f,
            System.currentTimeMillis()
        )));

        pendingAskStore.save("COMPETITION", item.getType(), question, "PENDING");
        log.info("已询问用户是否关注比赛 | type={} | title={}", item.getType(), item.getTitle());
    }

    private String typeDisplayName(String type) {
        if (type == null) return "未知";
        return switch (type) {
            case "CHALLENGE_CUP" -> "挑战杯";
            case "INTERNET_PLUS" -> "互联网+";
            case "INNOVATION_EXPO" -> "大创";
            case "ACADEMIC_COMPETITION" -> "学科竞赛";
            case "SKILL_COMPETITION" -> "技能大赛";
            case "OTHER_COMPETITION" -> "其他比赛";
            default -> type;
        };
    }
}
