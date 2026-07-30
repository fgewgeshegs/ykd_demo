package com.youkeda.exercise.claw.notification.source;
import com.youkeda.exercise.claw.notification.NotificationSource;

import com.youkeda.exercise.claw.campus.classifier.JobClassifier;
import com.youkeda.exercise.claw.campus.collector.CompetitionCollector;
import com.youkeda.exercise.claw.campus.model.NotificationItem;
import com.youkeda.exercise.claw.campus.policy.DefaultPolicy;
import com.youkeda.exercise.claw.campus.policy.NotificationPolicy;
import com.youkeda.exercise.claw.campus.policy.rule.JobRules;
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

/**
 * 就业通知 Source。
 * 复用教务处通知列表，通过 JobClassifier 筛选就业相关通知。
 */
@Component
@ConditionalOnProperty(name = "campus.enabled", havingValue = "true")
public class JobInfoSource implements NotificationSource {

    private static final Logger log = LoggerFactory.getLogger(JobInfoSource.class);
    private static final JobRules RULES = new JobRules();

    private final CompetitionCollector collector;
    private final CampusNotificationStore store;
    private final JobClassifier classifier;
    private final DefaultPolicy policy;
    private final PendingAskStore pendingAskStore;
    private final NotificationService notificationService;
    private final WechatUserManager userManager;

    public JobInfoSource(CompetitionCollector collector,
                         CampusNotificationStore store,
                         JobClassifier classifier,
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
    public String getName() { return "JOB"; }

    @Override
    public void check() {
        log.info("===== JobInfoSource 检查 =====");

        // 1. 采集（复用教务处通知列表）
        List<NotificationItem> items = collector.collect();
        if (items.isEmpty()) return;

        // 2. 去重（按 url + source 联合键）
        List<NotificationItem> newItems = store.deduplicate(items);
        if (newItems.isEmpty()) return;

        // 3. 逐条分类→决策→推送
        for (NotificationItem item : newItems) {
            processItem(item);
        }

        log.info("===== JobInfoSource 完成 | new={} =====", newItems.size());
    }

    private void processItem(NotificationItem item) {
        // 分类
        String type = classifier.classify(item);
        if (type == null) {
            log.debug("非就业通知，跳过 | title={}", item.getTitle());
            return;
        }
        item.setType(type);

        // 更新存储
        store.update(item);

        // 决策
        NotificationPolicy.Decision decision = policy.decide(item, null, RULES);

        switch (decision) {
            case NOTIFY -> notifyUser(item);
            case ASK -> askUser(item);
            case SKIP -> log.debug("用户已忽略就业 {}，跳过", type);
            case IGNORE -> log.debug("忽略就业通知");
        }
    }

    private void notifyUser(NotificationItem item) {
        String userId = userManager.getOwnerUserId();
        String typeDisplayName = typeDisplayName(item.getType());

        String message = "💼 就业提醒\n"
            + "「" + item.getTitle() + "」\n"
            + "类型: " + typeDisplayName + "\n"
            + (item.getPublishAt() != null && !item.getPublishAt().isBlank()
                ? "发布日期: " + item.getPublishAt() + "\n" : "")
            + "详情: " + item.getUrl();

        notificationService.notify(List.of(new Recommendation(
            "job_" + item.getType(),
            item.getTitle(),
            typeDisplayName + "就业通知",
            item.getClassifierReason(),
            message,
            item.getUrl(),
            1.0f,
            System.currentTimeMillis()
        )));
        log.info("就业通知已推送 | title={}", item.getTitle());
    }

    private void askUser(NotificationItem item) {
        String userId = userManager.getOwnerUserId();
        String typeDisplayName = typeDisplayName(item.getType());

        String question = "检测到新的「" + typeDisplayName + "」信息："
            + item.getTitle() + "，\n需要关注这类就业信息吗？（回复 需要/不需要）";

        notificationService.notify(List.of(new Recommendation(
            "ask_job_" + item.getType(),
            item.getTitle() + " - 是否需要关注",
            "需要用户确认",
            item.getClassifierReason(),
            question,
            null,
            0.8f,
            System.currentTimeMillis()
        )));

        pendingAskStore.save("JOB", item.getType(), question, "PENDING");
        log.info("已询问用户是否关注就业信息 | type={} | title={}", item.getType(), item.getTitle());
    }

    private String typeDisplayName(String type) {
        if (type == null) return "未知";
        return switch (type) {
            case "CAREER_FAIR" -> "招聘会/双选会";
            case "ELITE_TALK" -> "名企宣讲";
            case "INTERN_RECRUIT" -> "实习/校招";
            case "JOB_GUIDANCE" -> "就业指导";
            case "OTHER_JOB" -> "其他就业";
            default -> type;
        };
    }
}
