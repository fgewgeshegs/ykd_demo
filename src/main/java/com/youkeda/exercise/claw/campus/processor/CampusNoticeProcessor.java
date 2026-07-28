package com.youkeda.exercise.claw.campus.processor;

import com.youkeda.exercise.claw.campus.classifier.ExamLLMClassifier;
import com.youkeda.exercise.claw.campus.classifier.ExamRuleClassifier;
import com.youkeda.exercise.claw.campus.collector.CampusNoticeCollector;
import com.youkeda.exercise.claw.campus.model.CampusConfig;
import com.youkeda.exercise.claw.campus.model.ExamClassification;
import com.youkeda.exercise.claw.campus.model.NoticeItem;
import com.youkeda.exercise.claw.campus.model.NoticeType;
import com.youkeda.exercise.claw.campus.policy.CampusNotificationPolicy;
import com.youkeda.exercise.claw.campus.store.CampusNoticeStore;
import com.youkeda.exercise.claw.campus.store.PendingAskStore;
import com.youkeda.exercise.claw.scout.notifier.NotificationService;
import com.youkeda.exercise.claw.scout.judge.Recommendation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CampusNoticeProcessor {

    private static final Logger log = LoggerFactory.getLogger(CampusNoticeProcessor.class);

    private final CampusNoticeCollector collector;
    private final CampusNoticeStore noticeStore;
    private final ExamRuleClassifier ruleClassifier;
    private final ExamLLMClassifier llmClassifier;
    private final CampusNotificationPolicy policy;
    private final PendingAskStore pendingAskStore;
    private final NotificationService notificationService;

    public CampusNoticeProcessor(CampusNoticeCollector collector,
                                  CampusNoticeStore noticeStore,
                                  ExamRuleClassifier ruleClassifier,
                                  ExamLLMClassifier llmClassifier,
                                  CampusNotificationPolicy policy,
                                  PendingAskStore pendingAskStore,
                                  NotificationService notificationService) {
        this.collector = collector;
        this.noticeStore = noticeStore;
        this.ruleClassifier = ruleClassifier;
        this.llmClassifier = llmClassifier;
        this.policy = policy;
        this.pendingAskStore = pendingAskStore;
        this.notificationService = notificationService;
    }

    public void process(CampusConfig config) {
        String schoolUrl = resolveSchoolUrl(config.getSchool());
        if (schoolUrl == null) {
            log.warn("未知学校，跳过检查 | school={}", config.getSchool());
            return;
        }

        log.info("===== 开始检查考试通知 | school={} =====", config.getSchool());

        // 1. 采集
        List<NoticeItem> notices = collector.collect(schoolUrl);
        if (notices.isEmpty()) {
            log.info("未采集到通知 | school={}", config.getSchool());
            return;
        }

        // 2. 去重
        List<NoticeItem> newNotices = noticeStore.deduplicate(notices);
        if (newNotices.isEmpty()) {
            log.info("无新通知 | school={}", config.getSchool());
            return;
        }

        // 3. 逐条处理
        for (NoticeItem notice : newNotices) {
            processNotice(notice, config);
        }

        log.info("===== 考试通知检查完成 | school={} | new={} =====",
                config.getSchool(), newNotices.size());
    }

    private void processNotice(NoticeItem notice, CampusConfig config) {
        // 规则分类
        ExamClassification result = ruleClassifier.classify(notice);
        if (result == null) {
            // 规则未命中 → LLM 分类
            result = llmClassifier.classify(notice);
        }

        if (result == null || result.type() == NoticeType.UNKNOWN) {
            log.warn("通知分类失败，下次重试 | title={}", notice.getTitle());
            return;
        }

        // 更新数据库
        notice.setType(result.type());
        notice.setConfidence(result.confidence());
        notice.setScoreSource(result.scoreSource());
        notice.setClassifierReason(result.reason());
        notice.setProcessedAt(System.currentTimeMillis() / 1000);
        noticeStore.update(notice);

        // 策略决策
        CampusNotificationPolicy.Decision decision = policy.decide(result, config);
        log.info("通知处理结果 | title={} | type={} | confidence={} | decision={}",
                notice.getTitle(), result.type(), result.confidence(), decision);

        switch (decision) {
            case NOTIFY -> notifyUser(notice, result);
            case ASK -> askUser(notice, result);
            case SKIP -> log.debug("用户已忽略 {}, 跳过", result.type());
            case IGNORE -> log.debug("非考试通知, 忽略");
        }
    }

    private void notifyUser(NoticeItem notice, ExamClassification classification) {
        String message = formatNotification(notice, classification);
        notificationService.notify(null, List.of(new Recommendation(
            "exam_" + classification.type().name(),
            null,
            notice.getTitle(),
            typeDisplayName(classification.type()) + "考试提醒",
            classification.reason(),
            "请查看通知详情: " + notice.getUrl(),
            notice.getUrl(),
            1.0f,
            System.currentTimeMillis()
        )));
        log.info("考试通知已推送 | title={}", notice.getTitle());
    }

    private void askUser(NoticeItem notice, ExamClassification classification) {
        String question = "检测到新的「" + typeDisplayName(classification.type()) + "」通知："
            + notice.getTitle() + "，\n需要提醒你吗？（回复 需要/不需要）";

        notificationService.notify(null, List.of(new Recommendation(
            "ask_" + classification.type().name(),
            null,
            notice.getTitle() + " - 是否需要提醒",
            "需要用户确认",
            classification.reason(),
            question,
            null,
            0.8f,
            System.currentTimeMillis()
        )));

        pendingAskStore.save(classification.type().name(), question, "PENDING");
        log.info("已询问用户是否推送 | type={} | title={}",
                classification.type(), notice.getTitle());
    }

    private String formatNotification(NoticeItem notice, ExamClassification classification) {
        return "📌 考试提醒\n"
            + "「" + notice.getTitle() + "」\n"
            + "类型: " + typeDisplayName(classification.type()) + "\n"
            + (notice.getPublishAt() != null && !notice.getPublishAt().isBlank()
                ? "发布日期: " + notice.getPublishAt() + "\n" : "")
            + "详情: " + notice.getUrl();
    }

    private String typeDisplayName(NoticeType type) {
        return switch (type) {
            case FINAL_EXAM -> "期末考试";
            case CET -> "大学英语四六级";
            case RETAKE -> "补考/重修";
            case MIDTERM -> "期中考试";
            case COMPUTER_LEVEL -> "计算机等级考试";
            case PUTONGHUA -> "普通话测试";
            case OTHER_EXAM -> "其他考试";
            default -> type.name();
        };
    }

    /**
     * 根据学校名称解析教务处通知列表 URL
     */
    private String resolveSchoolUrl(String school) {
        if (school == null) return null;
        if (school.contains("南邮") || school.contains("南京邮电")) {
            return CampusNoticeCollector.NJUPT_NOTICE_URL;
        }
        // 后续扩展其他学校
        log.warn("未配置的学校: {}", school);
        return null;
    }
}
