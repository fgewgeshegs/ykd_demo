package com.youkeda.exercise.claw.scout;

import com.youkeda.exercise.claw.scout.collector.CollectorRegistry;
import com.youkeda.exercise.claw.scout.context.UserBehaviorAnalyzer;
import com.youkeda.exercise.claw.scout.context.UserContextService;
import com.youkeda.exercise.claw.scout.context.UserProfile;
import com.youkeda.exercise.claw.scout.judge.DecisionMaker;
import com.youkeda.exercise.claw.scout.judge.Recommendation;
import com.youkeda.exercise.claw.scout.matcher.CandidateMatcher;
import com.youkeda.exercise.claw.scout.matcher.MatchedCandidate;
import com.youkeda.exercise.claw.scout.notifier.NotificationService;
import com.youkeda.exercise.claw.scout.planner.SearchPlanner;
import com.youkeda.exercise.claw.scout.planner.SearchTask;
import com.youkeda.exercise.claw.scout.processor.InformationItem;
import com.youkeda.exercise.claw.scout.processor.InformationProcessor;
import com.youkeda.exercise.claw.scout.store.InformationStore;
import com.youkeda.exercise.claw.wechat.user.WechatUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 信息猎手总调度器
 *
 * 拆分为四个独立定时任务：
 * 1. 信息采集（每 4 小时）— 搜索 → 处理 → 存入信息库
 * 2. 推荐决策（每天早 8 点）— 匹配 → 判断 → 推送
 * 3. 画像更新（每天晚 10 点）— 从对话更新用户画像
 * 4. 过期清理（每天凌晨 3 点）— 清理过期信息
 */
@Component
@ConditionalOnProperty(name = "scout.enabled", havingValue = "true")
public class ScoutOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(ScoutOrchestrator.class);

    private final UserContextService contextService;
    private final UserBehaviorAnalyzer behaviorAnalyzer;
    private final SearchPlanner planner;
    private final CollectorRegistry collectorRegistry;
    private final InformationProcessor processor;
    private final InformationStore store;
    private final CandidateMatcher matcher;
    private final DecisionMaker decisionMaker;
    private final NotificationService notifier;
    private final ScoutProperties props;
    private final WechatUserManager userManager;

    public ScoutOrchestrator(UserContextService contextService,
                              UserBehaviorAnalyzer behaviorAnalyzer,
                              SearchPlanner planner,
                              CollectorRegistry collectorRegistry,
                              InformationProcessor processor,
                              InformationStore store,
                              CandidateMatcher matcher,
                              DecisionMaker decisionMaker,
                              NotificationService notifier,
                              ScoutProperties props,
                              WechatUserManager userManager) {
        this.contextService = contextService;
        this.behaviorAnalyzer = behaviorAnalyzer;
        this.planner = planner;
        this.collectorRegistry = collectorRegistry;
        this.processor = processor;
        this.store = store;
        this.matcher = matcher;
        this.decisionMaker = decisionMaker;
        this.notifier = notifier;
        this.props = props;
        this.userManager = userManager;
    }

    // ==================== 手动触发：完整流程 ====================

    /**
     * 为单个用户执行信息猎手完整流程（手动触发时使用）
     */
    public ScoutReport runForUser(String userId) {
        log.info("========== 信息猎手启动（完整流程）| userId={} ==========", userId);

        try {
            // 1. 构建用户画像
            log.info("[1/7] 构建用户画像...");
            UserProfile profile = contextService.buildProfile(userId);

            // 2. 生成搜索任务
            log.info("[2/7] 生成搜索任务...");
            List<SearchTask> tasks = planner.plan(profile);
            log.info("搜索任务 | count={}", tasks.size());

            // 3. 多源采集
            log.info("[3/7] 多源信息采集...");
            List<InformationItem> rawItems = collectorRegistry.collectAll(tasks, userId);
            log.info("采集结果 | count={}", rawItems.size());

            if (rawItems.isEmpty()) {
                log.info("无采集结果，流程结束 | userId={}", userId);
                return new ScoutReport(tasks.size(), 0, 0);
            }

            // 4. 信息处理
            log.info("[4/7] 信息处理...");
            List<InformationItem> processed = processor.process(rawItems);
            log.info("处理结果 | count={}", processed.size());

            // 5. 存入信息库
            log.info("[5/7] 存入信息库...");
            store.batchSave(processed);

            // 6. 语义匹配
            log.info("[6/7] 语义匹配...");
            List<MatchedCandidate> candidates = matcher.match(userId, profile, processed);
            log.info("匹配结果 | count={}", candidates.size());

            // 7. LLM 价值判断 + 推送
            log.info("[7/7] LLM 价值判断...");
            List<Recommendation> recommendations = decisionMaker.judge(profile, candidates);
            log.info("推荐结果 | count={}", recommendations.size());

            // 推送
            notifier.notify(userId, recommendations);

            ScoutReport report = new ScoutReport(tasks.size(), processed.size(), recommendations.size());
            log.info("========== 信息猎手完成 | userId={} | report={} ==========", userId, report);
            return report;

        } catch (Exception e) {
            log.error("信息猎手执行异常 | userId={}", userId, e);
            return new ScoutReport(0, 0, 0);
        }
    }

    // ==================== 定时任务 1：信息采集（每 4 小时） ====================

    /**
     * 定时采集：只做 搜索 → 处理 → 存库，不推送
     */
    @Scheduled(cron = "0 0 */4 * * *")
    public void scheduledCollect() {
        List<String> users = getTargetUsers();
        if (users.isEmpty()) {
            log.debug("尚未识别个人助手主人，跳过采集");
            return;
        }

        log.info("========== 定时采集启动 | users={} ==========", users.size());
        for (String userId : users) {
            try {
                collectForUser(userId);
            } catch (Exception e) {
                log.error("定时采集失败 | userId={}", userId, e);
            }
        }
        log.info("========== 定时采集完成 ==========");
    }

    /**
     * 为单个用户执行采集（不含推荐推送）
     */
    private void collectForUser(String userId) {
        UserProfile profile = contextService.buildProfile(userId);
        List<SearchTask> tasks = planner.plan(profile);
        List<InformationItem> rawItems = collectorRegistry.collectAll(tasks, userId);

        if (rawItems.isEmpty()) {
            log.info("采集无结果 | userId={}", userId);
            return;
        }

        List<InformationItem> processed = processor.process(rawItems);
        store.batchSave(processed);
        log.info("采集完成 | userId={} | tasks={} | stored={}", userId, tasks.size(), processed.size());
    }

    // ==================== 定时任务 2：推荐决策（每天早 8 点） ====================

    /**
     * 定时推荐：每天固定时间，从信息库中匹配 → 判断 → 推送
     */
    @Scheduled(cron = "${scout.cron:0 0 8 * * *}")
    public void scheduledRecommend() {
        List<String> users = getTargetUsers();
        if (users.isEmpty()) {
            log.debug("尚未识别个人助手主人，跳过推荐");
            return;
        }

        log.info("========== 定时推荐启动 | users={} ==========", users.size());
        for (String userId : users) {
            try {
                recommendForUser(userId);
            } catch (Exception e) {
                log.error("定时推荐失败 | userId={}", userId, e);
            }
        }
        log.info("========== 定时推荐完成 ==========");
    }

    /**
     * 为单个用户执行推荐（从已有信息库中匹配）
     */
    private void recommendForUser(String userId) {
        UserProfile profile = contextService.buildProfile(userId);

        // 从信息库中取最近的信息
        List<InformationItem> recentItems = store.getRecent(userId, props.getMaxCandidates() * 3);
        if (recentItems.isEmpty()) {
            log.info("信息库为空，跳过推荐 | userId={}", userId);
            return;
        }

        // 语义匹配
        List<MatchedCandidate> candidates = matcher.match(userId, profile, recentItems);
        if (candidates.isEmpty()) {
            log.info("无匹配候选，跳过推荐 | userId={}", userId);
            return;
        }

        // LLM 判断
        List<Recommendation> recommendations = decisionMaker.judge(profile, candidates);

        // 推送
        notifier.notify(userId, recommendations);
        log.info("推荐完成 | userId={} | candidates={} | recommended={}",
                userId, candidates.size(), recommendations.size());
    }

    // ==================== 定时任务 3：画像更新（每天晚 10 点） ====================

    /**
     * 定时画像更新：分析用户行为 + 重建画像
     */
    @Scheduled(cron = "0 0 22 * * *")
    public void scheduledProfileUpdate() {
        List<String> users = getTargetUsers();
        if (users.isEmpty()) return;

        log.info("========== 定时画像更新 | users={} ==========", users.size());
        for (String userId : users) {
            try {
                // 1. 从对话历史提取新兴趣，存入长期记忆
                int newInterests = behaviorAnalyzer.analyzeAndStore(userId);
                if (newInterests > 0) {
                    log.info("发现新兴趣 | userId={} | count={}", userId, newInterests);
                }

                // 2. 重建用户画像（基于最新的长期记忆）
                contextService.buildProfile(userId);
                log.debug("画像更新完成 | userId={}", userId);
            } catch (Exception e) {
                log.error("画像更新失败 | userId={}", userId, e);
            }
        }
    }

    // ==================== 定时任务 4：过期清理（每天凌晨 3 点） ====================

    /**
     * 定时清理：删除过期信息
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void scheduledCleanup() {
        List<String> users = getTargetUsers();
        if (users.isEmpty()) return;

        long expireBefore = Instant.now()
                .minus(props.getTtlDays(), ChronoUnit.DAYS)
                .toEpochMilli();

        log.info("========== 定时清理启动 | ttlDays={} ==========", props.getTtlDays());
        for (String userId : users) {
            try {
                store.deleteExpired(userId, expireBefore);
            } catch (Exception e) {
                log.error("过期清理失败 | userId={}", userId, e);
            }
        }
    }

    // ==================== 个人助手主人识别 ====================

    /**
     * 记录用户活跃（由外部调用，如消息收发层）
     */
    public void markUserActive(String userId) {
        userManager.recordInteraction(userId);
    }

    /**
     * 获取本地安装对应的唯一主人。
     */
    private List<String> getTargetUsers() {
        String ownerUserId = userManager.getOwnerUserId();
        return ownerUserId == null || ownerUserId.isBlank()
                ? List.of() : List.of(ownerUserId);
    }
}
