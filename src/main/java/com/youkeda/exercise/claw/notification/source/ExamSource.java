package com.youkeda.exercise.claw.notification.source;
import com.youkeda.exercise.claw.notification.NotificationSource;

import com.youkeda.exercise.claw.campus.classifier.ExamLLMClassifier;
import com.youkeda.exercise.claw.campus.classifier.ExamRuleClassifier;
import com.youkeda.exercise.claw.campus.collector.CampusNoticeCollector;
import com.youkeda.exercise.claw.campus.model.CampusConfig;
import com.youkeda.exercise.claw.campus.model.ExamClassification;
import com.youkeda.exercise.claw.campus.model.NoticeItem;
import com.youkeda.exercise.claw.campus.model.NoticeType;
import com.youkeda.exercise.claw.campus.model.NotificationItem;
import com.youkeda.exercise.claw.campus.policy.DefaultPolicy;
import com.youkeda.exercise.claw.campus.policy.NotificationPolicy;
import com.youkeda.exercise.claw.campus.policy.rule.ExamRules;
import com.youkeda.exercise.claw.campus.store.CampusConfigStore;
import com.youkeda.exercise.claw.campus.store.CampusNoticeStore;
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
public class ExamSource implements NotificationSource {

    private static final Logger log = LoggerFactory.getLogger(ExamSource.class);

    private final CampusNoticeCollector collector;
    private final CampusNoticeStore noticeStore;
    private final DefaultPolicy policy;
    private final CampusConfigStore configStore;
    private final PendingAskStore pendingAskStore;
    private final NotificationService notificationService;
    private final WechatUserManager userManager;
    private final ExamRuleClassifier ruleClassifier;
    private final ExamLLMClassifier llmClassifier;

    public ExamSource(CampusNoticeCollector collector,
                      CampusNoticeStore noticeStore,
                      DefaultPolicy policy,
                      CampusConfigStore configStore,
                      PendingAskStore pendingAskStore,
                      NotificationService notificationService,
                      WechatUserManager userManager,
                      ExamRuleClassifier ruleClassifier,
                      ExamLLMClassifier llmClassifier) {
        this.collector = collector;
        this.noticeStore = noticeStore;
        this.policy = policy;
        this.configStore = configStore;
        this.pendingAskStore = pendingAskStore;
        this.notificationService = notificationService;
        this.userManager = userManager;
        this.ruleClassifier = ruleClassifier;
        this.llmClassifier = llmClassifier;
    }

    @Override
    public String getName() { return "EXAM"; }

    @Override
    public void check() {
        try {
            CampusConfig config = configStore.get();
            if (config == null || config.getSchool() == null || config.getSchool().isBlank()) return;

            String schoolUrl = resolveSchoolUrl(config.getSchool());
            if (schoolUrl == null) {
                log.warn("未知学校，跳过考试通知检查 | school={}", config.getSchool());
                return;
            }

            log.info("===== ExamSource 检查 | school={} =====", config.getSchool());

            // 1. 采集
            List<NoticeItem> notices = collector.collect(schoolUrl);
            if (notices.isEmpty()) return;

            // 2. 去重
            List<NoticeItem> newNotices = noticeStore.deduplicate(notices);
            if (newNotices.isEmpty()) return;

            // 3. 逐条处理：分类 → 桥接 → 策略决策 → 推送
            for (NoticeItem notice : newNotices) {
                processNotice(notice, config);
            }

            log.info("===== ExamSource 完成 | school={} | new={} =====",
                    config.getSchool(), newNotices.size());
        } catch (Exception e) {
            log.error("ExamSource 检查异常", e);
        }
    }

    private void processNotice(NoticeItem notice, CampusConfig config) {
        // 1. 分类：规则优先，LLM 兜底
        ExamClassification result = ruleClassifier.classify(notice);
        if (result == null) {
            result = llmClassifier.classify(notice);
        }
        if (result == null || result.type() == NoticeType.UNKNOWN) {
            log.warn("通知分类失败，下次重试 | title={}", notice.getTitle());
            return;
        }

        // 更新通知的分类结果
        notice.setType(result.type());
        notice.setConfidence(result.confidence());
        notice.setScoreSource(result.scoreSource());
        notice.setClassifierReason(result.reason());
        notice.setProcessedAt(System.currentTimeMillis() / 1000);
        noticeStore.update(notice);

        // 2. 桥接为 NotificationItem，供 DefaultPolicy 决策
        NotificationItem notificationItem = bridgeToNotificationItem(notice);

        // 3. 策略决策
        NotificationPolicy.Decision decision = policy.decide(
            notificationItem, config, new ExamRules());

        // 4. 执行决策
        switch (decision) {
            case NOTIFY -> notifyUser(notice, notificationItem);
            case ASK -> askUser(notice, notificationItem);
            case SKIP -> log.debug("用户已忽略 {}，跳过", notice.getType());
            case IGNORE -> log.debug("非考试通知，忽略");
        }
    }

    private NotificationItem bridgeToNotificationItem(NoticeItem notice) {
        NotificationItem item = new NotificationItem("EXAM",
            notice.getTitle(), notice.getUrl(), notice.getPublishAt());
        if (notice.getType() != null) item.setType(notice.getType().name());
        item.setConfidence(notice.getConfidence());
        item.setScoreSource(notice.getScoreSource());
        item.setClassifierReason(notice.getClassifierReason());
        item.setStatus(notice.getStatus());
        item.setProcessedAt(notice.getProcessedAt());
        return item;
    }

    private void notifyUser(NoticeItem notice, NotificationItem notificationItem) {
        String userId = userManager.getOwnerUserId();
        String typeDisplayName = typeDisplayName(notice.getType());

        String message = "📌 考试提醒\n"
            + "「" + notice.getTitle() + "」\n"
            + "类型: " + typeDisplayName + "\n"
            + (notice.getPublishAt() != null && !notice.getPublishAt().isBlank()
                ? "发布日期: " + notice.getPublishAt() + "\n" : "")
            + "详情: " + notice.getUrl();

        notificationService.notify(List.of(new Recommendation(
            "exam_" + notificationItem.getType(),
            notice.getTitle(),
            typeDisplayName + "考试提醒",
            notificationItem.getClassifierReason(),
            message,
            notice.getUrl(),
            1.0f,
            System.currentTimeMillis()
        )));
        log.info("考试通知已推送 | title={}", notice.getTitle());
    }

    private void askUser(NoticeItem notice, NotificationItem notificationItem) {
        String userId = userManager.getOwnerUserId();
        String typeDisplayName = typeDisplayName(notice.getType());

        String question = "检测到新的「" + typeDisplayName + "」通知："
            + notice.getTitle() + "，\n需要提醒你吗？（回复 需要/不需要）";

        notificationService.notify(List.of(new Recommendation(
            "ask_" + notificationItem.getType(),
            notice.getTitle() + " - 是否需要提醒",
            "需要用户确认",
            notificationItem.getClassifierReason(),
            question,
            null,
            0.8f,
            System.currentTimeMillis()
        )));

        pendingAskStore.save("EXAM", notificationItem.getType(), question, "PENDING");
        log.info("已询问用户是否推送 | type={} | title={}",
                notificationItem.getType(), notice.getTitle());
    }

    private String typeDisplayName(NoticeType type) {
        if (type == null) return "未知";
        return switch (type) {
            case FINAL_EXAM -> "期末考试";
            case CET -> "大学英语四六级";
            case RETAKE -> "补考/重修";
            case MIDTERM -> "期中考试";
            case COMPUTER_LEVEL -> "计算机等级考试";
            case PUTONGHUA -> "普通话测试";
            case OTHER_EXAM -> "其他考试";
            default -> "未知类型";
        };
    }

    private String resolveSchoolUrl(String school) {
        if (school == null) return null;
        if (school.contains("南邮") || school.contains("南京邮电")) {
            return CampusNoticeCollector.NJUPT_NOTICE_URL;
        }
        log.warn("未配置的学校: {}", school);
        return null;
    }
}
