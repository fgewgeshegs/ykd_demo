package com.youkeda.exercise.claw.agent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.youkeda.exercise.claw.agent.SafetyPolicy;
import com.youkeda.exercise.claw.agent.ToolResultStatusParser;
import com.youkeda.exercise.claw.agent.activity.AgentActivityRecorder;
import com.youkeda.exercise.claw.agent.model.ExecutionStatus;
import com.youkeda.exercise.claw.agent.model.PlanState;
import com.youkeda.exercise.claw.agent.model.PlanTask;
import com.youkeda.exercise.claw.agent.model.ResultStatus;
import com.youkeda.exercise.claw.agent.model.TaskResult;
import com.youkeda.exercise.claw.agent.plan.PlanStore;
import com.youkeda.exercise.claw.agent.skill.SkillPendingCoordinator;
import com.youkeda.exercise.claw.agent.skill.SkillSession;
import com.youkeda.exercise.claw.ai.llm.LLMResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 工具调用执行器。
 *
 * <p>负责完整生命周期：查找工具 → 安全/可用性/去重校验 → 执行 → 记录活动 → 更新计划状态。
 * 不涉及 LLM 通信，纯工具执行层。
 */
@Component
public class ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);

    /** 单次请求允许执行的最大工具数 */
    static final int MAX_TOOL_CALLS = 16;

    private final ToolRegistry toolRegistry;
    private final SafetyPolicy safetyPolicy;
    private final SkillPendingCoordinator skillPendingCoordinator;
    private final AgentActivityRecorder activityRecorder;
    private final ToolResultStatusParser toolResultStatusParser;
    private final PlanStore planStore;
    private final ObjectMapper objectMapper;

    public ToolExecutor(ToolRegistry toolRegistry,
                        SafetyPolicy safetyPolicy,
                        SkillPendingCoordinator skillPendingCoordinator,
                        AgentActivityRecorder activityRecorder,
                        ToolResultStatusParser toolResultStatusParser,
                        PlanStore planStore,
                        ObjectMapper objectMapper) {
        this.toolRegistry = toolRegistry;
        this.safetyPolicy = safetyPolicy;
        this.skillPendingCoordinator = skillPendingCoordinator;
        this.activityRecorder = activityRecorder;
        this.toolResultStatusParser = toolResultStatusParser;
        this.planStore = planStore;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行一批工具调用。
     *
     * @param toolCalls         LLM 返回的工具调用
     * @param execContext       执行上下文
     * @param session           当前 Skill 会话（可能被更新）
     * @param planState         当前计划状态（可能被更新）
     * @param activityRequestId 活动记录请求 ID
     * @param activeSkillName   当前技能名
     * @param userMessage       用户原始消息
     * @param executedCalls     已执行调用的签名集合（可变，会新增）
     * @return 执行结果
     */
    public ToolExecutionBatch executeToolCalls(
            List<LLMResponse.ToolCall> toolCalls,
            ToolExecutionContext execContext,
            SkillSession session,
            PlanState planState,
            String activityRequestId,
            String activeSkillName,
            String userMessage,
            Set<String> executedCalls) {

        List<String> results = new ArrayList<>();
        boolean executedInBatch = false;
        int toolCallCount = 0;

        for (LLMResponse.ToolCall tc : toolCalls) {
            String toolName = tc.name();
            log.info("工具调用 | name={} | args={} | id={}", toolName, tc.arguments(), tc.id());

            Tool fn = toolRegistry.find(toolName);
            String result;
            String callSignature = toolName + "|" + tc.arguments();

            // Phase 1: 安全检查（CanExecute）
            String blockedReason = safetyPolicy.canExecute(toolName, tc.arguments());

            // 工具不存在
            if (fn == null) {
                log.warn("未找到工具: {}", toolName);
                result = "{\"error\":\"未知工具: " + toolName + "\"}";
                activityRecorder.toolBlocked(
                        activityRequestId, activeSkillName, toolName, "未知工具");
            }
            // 当前消息不满足工具的严格触发条件
            else if (!fn.isAvailable(execContext)) {
                log.warn("工具调用被可用性策略阻止 | name={} | message={}", toolName, userMessage);
                String reason = fn.getUnavailableReason(execContext);
                result = policyBlocked(reason);
                activityRecorder.toolBlocked(
                        activityRequestId, activeSkillName, toolName, reason);
            }
            // 安全检查阻止
            else if (blockedReason != null) {
                result = policyBlocked(blockedReason);
                activityRecorder.toolBlocked(
                        activityRequestId, activeSkillName, toolName, blockedReason);
            }
            // 工具调用数量上限
            else if (toolCallCount >= MAX_TOOL_CALLS) {
                result = policyBlocked("本次请求工具调用数量已达上限，请使用已有结果生成答复。");
                activityRecorder.toolBlocked(
                        activityRequestId, activeSkillName, toolName, "工具调用数量已达上限");
            }
            // 去重（相同工具 + 相同参数）
            else if (!executedCalls.add(callSignature)) {
                result = policyBlocked("相同工具和参数已经执行过，请使用已有结果，不要重复调用。");
                activityRecorder.toolBlocked(
                        activityRequestId, activeSkillName, toolName, "重复工具调用");
            }
            // 执行
            else {
                toolCallCount++;
                executedInBatch = true;
                long toolStartedAt = System.currentTimeMillis();
                activityRecorder.toolStarted(
                        activityRequestId, activeSkillName, toolName);
                try {
                    result = fn.execute(tc.arguments(), execContext);
                    session = skillPendingCoordinator.afterToolExecution(session, toolName);
                    ResultStatus resultStatus = parseResultStatus(result);
                    boolean succeeded = resultStatus == ResultStatus.SUCCESS
                            || resultStatus == ResultStatus.PARTIAL;
                    activityRecorder.toolFinished(
                            activityRequestId, activeSkillName, toolName, succeeded,
                            System.currentTimeMillis() - toolStartedAt);
                } catch (RuntimeException e) {
                    activityRecorder.toolFinished(
                            activityRequestId, activeSkillName, toolName, false,
                            System.currentTimeMillis() - toolStartedAt);
                    throw e;
                }
                log.info("工具执行完成 | name={} | result={}", toolName, truncate(result, 200));

                // 更新 PlanState（如果有）
                if (planState != null) {
                    PlanTask matchingTask = findTaskByToolName(planState, toolName);
                    if (matchingTask != null) {
                        matchingTask.setExecutionStatus(ExecutionStatus.DONE);
                        TaskResult taskResult = new TaskResult(
                                matchingTask.getId(), toolName,
                                parseResultStatus(result), result, System.currentTimeMillis());
                        matchingTask.setResult(taskResult);
                        planStore.save(planState);
                    }
                }
            }
            results.add(result);
        }

        return new ToolExecutionBatch(results, session, planState, executedInBatch, toolCallCount);
    }

    /**
     * 检查某批工具调用是否启动了信息猎手。
     */
    boolean isStartedInformationScout(List<LLMResponse.ToolCall> toolCalls, List<String> toolResults) {
        for (int i = 0; i < toolCalls.size(); i++) {
            if (!"information_scout".equals(toolCalls.get(i).name())) continue;
            try {
                JsonNode result = objectMapper.readTree(toolResults.get(i));
                if ("started".equalsIgnoreCase(result.path("status").asText())) {
                    return true;
                }
            } catch (Exception ignored) {
                // 非 JSON 结果不能视为已受理后台任务。
            }
        }
        return false;
    }

    // ==================== 工具方法 ====================

    public record ToolExecutionBatch(
            List<String> results,
            SkillSession session,
            PlanState planState,
            boolean executedInBatch,
            int toolCallCount
    ) {}

    private String policyBlocked(String reason) {
        try {
            var node = objectMapper.createObjectNode();
            node.put("status", "BLOCKED");
            node.put("reason", reason);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return "{\"status\":\"BLOCKED\"}";
        }
    }

    /**
     * 根据工具名模糊匹配 PlanState 中的 PENDING 任务。
     * 优先匹配 description 包含工具名的任务；无匹配时返回第一个 PENDING 任务。
     */
    private PlanTask findTaskByToolName(PlanState planState, String toolName) {
        if (planState == null || planState.getTasks() == null) return null;
        // 优先匹配 description 包含工具名的 PENDING 任务
        for (PlanTask task : planState.getTasks()) {
            if (task.getExecutionStatus() == ExecutionStatus.PENDING
                    && task.getDescription() != null
                    && task.getDescription().toLowerCase().contains(toolName.toLowerCase())) {
                return task;
            }
        }
        // 回退：任意 PENDING 任务
        for (PlanTask task : planState.getTasks()) {
            if (task.getExecutionStatus() == ExecutionStatus.PENDING) {
                return task;
            }
        }
        return null;
    }

    private ResultStatus parseResultStatus(String resultJson) {
        return toolResultStatusParser.parse(resultJson);
    }

    static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
