package com.youkeda.exercise.claw.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import com.youkeda.exercise.claw.agent.memory.Message;
import com.youkeda.exercise.claw.agent.model.EvaluationState;
import com.youkeda.exercise.claw.agent.model.ExecutionStatus;
import com.youkeda.exercise.claw.agent.model.PlanState;
import com.youkeda.exercise.claw.agent.model.PlanTask;
import com.youkeda.exercise.claw.agent.plan.PlanStore;
import com.youkeda.exercise.claw.agent.plan.PlanValidator;
import com.youkeda.exercise.claw.agent.plan.ValidationResult;
import com.youkeda.exercise.claw.agent.skill.SkillSession;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.ai.llm.LLMResponse;
import com.youkeda.exercise.claw.ai.llm.PlanDecision;
import com.youkeda.exercise.claw.ai.llm.TaskDefinition;
import com.youkeda.exercise.claw.ai.llm.ToolDefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * LLM ↔ Tool 执行循环。
 *
 * <p>职责：接收 system prompt、消息列表、工具定义，驱动 LLM 自主决策调用工具或直接回复。
 * 内部管理：
 * <ul>
 *   <li>LLM 调用（含空响应降级重试）</li>
 *   <li>结构化计划处理（校验、持久化、上下文注入）</li>
 *   <li>文本回复 → 即时返回</li>
 *   <li>工具调用 → 委托 {@link ToolExecutor} → 结果追加到消息列表 → 继续循环</li>
 * </ul>
 *
 * <p>核心原则：不感知 Skill、Memory、Plan 的上层语义——这些由 {@code AgentRuntime} 编排。
 */
@Component
public class ExecutionLoop {

    private static final Logger log = LoggerFactory.getLogger(ExecutionLoop.class);

    /** 工具调用循环最大轮次 */
    private static final int MAX_ROUNDS = 15;

    /**
     * 取消/删除定时任务的用户意图。
     * 动词与任务名词成对出现，避免误伤「删除文件」「删除聊天记录」等普通删除请求。
     */
    private static final Pattern TASK_CANCEL_REQUEST = Pattern.compile(
            "(?:删除|取消|移除|去掉|停掉|关掉|暂停).{0,8}(?:提醒|任务|定时任务|闹钟|推送)"
                    + "|(?:提醒|任务|定时任务|闹钟|推送).{0,10}(?:删除|取消|移除|去掉)");

    /**
     * 修改/调整定时任务的用户意图。
     * 「改」字较宽泛，必须与任务名词成对出现，避免误伤「改一下报告」等普通请求。
     */
    private static final Pattern TASK_UPDATE_REQUEST = Pattern.compile(
            "(?:修改|调整|提前|延后|往后|往前|挪).{0,6}(?:提醒|任务|时间|定时|闹钟|推送|计划)"
                    + "|(?:提醒|任务|定时任务).{0,6}(?:改成|改为|调整|修改|提前|延后)");

    private final LLMClient llmClient;
    private final ToolExecutor toolExecutor;
    private final PlanStore planStore;
    private final PlanValidator planValidator;
    private final ObjectMapper objectMapper;

    public ExecutionLoop(LLMClient llmClient,
                         ToolExecutor toolExecutor,
                         PlanStore planStore,
                         PlanValidator planValidator,
                         ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.toolExecutor = toolExecutor;
        this.planStore = planStore;
        this.planValidator = planValidator;
        this.objectMapper = objectMapper;
    }

    /**
     * 运行 LLM ↔ Tool 执行循环。
     *
     * @param systemPrompt     系统提示
     * @param messages         消息列表（可变，调用方传入后会被追加 assistant/tool 消息）
     * @param tools            可用工具定义
     * @param initialPlanState 初始计划状态
     * @param execContext      工具执行上下文
     * @param session          当前 Skill 会话
     * @param activityRequestId 活动记录请求 ID
     * @param activeSkillName  当前技能名
     * @param userMessage      用户原始消息
     * @return 执行结果
     */
    public Result run(
            String systemPrompt,
            List<Message> messages,
            List<ToolDefinition> tools,
            PlanState initialPlanState,
            ToolExecutionContext execContext,
            SkillSession session,
            String activityRequestId,
            String activeSkillName,
            String userMessage) {

        Set<String> executedCalls = new HashSet<>();
        boolean forceTextResponse = false;
        PlanState planState = initialPlanState;

        for (int round = 0; round < MAX_ROUNDS; round++) {
            log.info("工具调用循环第 {} 轮 | messages={}", round + 1, messages.size());

            List<ToolDefinition> roundTools = forceTextResponse ? List.of() : tools;
            LLMResponse response = llmClient.chatWithTools(systemPrompt, messages, roundTools);
            forceTextResponse = false;

            // 工具调用返回 null 时的单轮降级
            if (response == null) {
                if (!roundTools.isEmpty()) {
                    log.warn("本轮带工具的 LLM 调用失败，降级不带工具重试（下一轮恢复工具）");
                    response = llmClient.chatWithTools(messages, List.of());
                }
                if (response == null) {
                    log.warn("LLM 调用失败，结束循环");
                    return Result.llmFailed(messages, planState, session);
                }
            }

            // === 分支 1：结构化计划 ===
            if (response.isPlan()) {
                PlanDecision plan = response.getPlan();
                log.info("LLM 返回计划 | goal={} | tasks={}",
                        plan.getGoal(),
                        plan.getTasks().stream().map(TaskDefinition::getId).toList());

                PlanState newPlan = planDecisionToState(plan);
                ValidationResult vr = planValidator.validate(newPlan);
                if (!vr.valid()) {
                    log.warn("计划校验失败 | errors={}", vr.errors());
                    String errorMsg = "你生成的计划存在结构问题："
                            + String.join("；", vr.errors())
                            + "。请修正后重新生成。";
                    messages.add(new Message("system", errorMsg));
                    continue;
                }
                if (!vr.warnings().isEmpty()) {
                    log.info("计划警告 | warnings={}", vr.warnings());
                }

                newPlan.setVersion(planState != null ? planState.getVersion() + 1 : 1);
                planStore.save(newPlan);
                planState = newPlan;

                // 所有任务都已完成 → 结束循环
                if (newPlan.getTasks().stream()
                        .allMatch(t -> t.getExecutionStatus() == ExecutionStatus.DONE
                                || t.getEvaluationState() == EvaluationState.SUPERSEDED)) {
                    log.info("所有计划任务已完成，进入最终回复");
                    forceTextResponse = true;
                    continue;
                }
                // 有任务未完成，继续让 LLM 执行
                injectPlanContext(messages, newPlan);
                continue;
            }

            // === 分支 2：直接回复文本 ===
            if (!response.isToolCall()) {
                // 防幻觉检测：用户要求创建定时提醒但 create_schedule_task 未被调用
                if (!wasScheduleTaskCalled(executedCalls)
                        && isScheduleTaskRequest(userMessage)) {
                    log.warn("LLM 幻觉检测：用户要求创建提醒但 create_schedule_task 未被调用，注入提示重试");
                    messages.add(new Message("system",
                            "注意：你刚才未调用 create_schedule_task 工具。"
                                    + "用户明确要求创建定时提醒，请先调用 create_schedule_task 完成创建，"
                                    + "创建成功后再回复用户。不要重复调用已经执行过的工具。"));
                    continue;
                }

                // 防幻觉检测：用户要求取消/删除任务但 cancel_schedule_task 未被调用
                if (!wasScheduleTaskCancelCalled(executedCalls)
                        && isScheduleTaskCancelRequest(userMessage)) {
                    log.warn("LLM 幻觉检测：用户要求取消任务但 cancel_schedule_task 未被调用，注入提示重试");
                    messages.add(new Message("system",
                            "注意：你刚才未调用 cancel_schedule_task 工具。"
                                    + "用户明确要求取消/删除定时任务，请先调用 list_schedule_tasks 确认任务 ID，"
                                    + "再调用 cancel_schedule_task 完成取消，取消成功后再回复用户。"));
                    continue;
                }

                // 防幻觉检测：用户要求修改任务但 update_schedule_task 未被调用
                if (!wasScheduleTaskUpdateCalled(executedCalls)
                        && isScheduleTaskUpdateRequest(userMessage)) {
                    log.warn("LLM 幻觉检测：用户要求修改任务但 update_schedule_task 未被调用，注入提示重试");
                    messages.add(new Message("system",
                            "注意：你刚才未调用 update_schedule_task 工具。"
                                    + "用户明确要求修改定时任务，请先调用 list_schedule_tasks 确认任务 ID，"
                                    + "再调用 update_schedule_task 完成修改，修改成功后再回复用户。"));
                    continue;
                }

                String reply = response.getContent();
                log.info("LLM 直接回复 | reply={}", reply);
                return Result.textReply(reply, messages, planState, session);
            }

            // === 分支 3：工具调用 ===
            List<LLMResponse.ToolCall> toolCalls = response.getToolCalls();
            ToolExecutor.ToolExecutionBatch batch = toolExecutor.executeToolCalls(
                    toolCalls, execContext, session, planState,
                    activityRequestId, activeSkillName, userMessage, executedCalls);
            session = batch.session();
            planState = batch.planState();

            if (toolExecutor.isStartedInformationScout(toolCalls, batch.results())) {
                log.info("信息猎手后台任务已受理，本轮保持静默");
                return Result.silent(messages, planState, session);
            }

            // 添加 assistant 消息（合并本轮所有 tool_calls）
            addAssistantMessage(messages, toolCalls, response.getReasoningContent());

            // 添加 tool 结果消息
            for (int i = 0; i < toolCalls.size(); i++) {
                messages.add(new Message("tool", batch.results().get(i),
                        null, null, null, toolCalls.get(i).id(), null));
            }

            // 本轮没有任何工具实际执行 → 下一轮强制文本回复
            if (!batch.executedInBatch()) {
                forceTextResponse = true;
            }
        }

        // 达到局部上限
        log.warn("工具调用循环达到上限 {} 轮", MAX_ROUNDS);
        return Result.maxRounds(messages, planState, session);
    }

    /**
     * 达到循环上限后，让 LLM 基于已有结果生成最终回复。
     *
     * @param systemPrompt 系统提示
     * @param messages     当前消息列表（含工具执行结果）
     * @return LLM 合成的回复文本
     */
    public String synthesize(String systemPrompt, List<Message> messages) {
        List<Message> finalMessages = new ArrayList<>(messages);
        finalMessages.add(new Message("system",
                "工具调用轮次已结束。请仅根据已有结果回复："
                        + "若信息不足，提出一个明确问题让用户补充；"
                        + "若信息已齐全，给出当前结果；缺失信息标记待确认，不得编造。"));
        LLMResponse response = llmClient.chatWithTools(systemPrompt, finalMessages, List.of());
        if (response != null && response.getContent() != null
                && !response.getContent().isBlank()) {
            return response.getContent();
        }
        log.warn("最终汇总仍返回工具调用，使用兜底消息");
        return "已根据当前可用信息整理方案。尚未核实的信息标记为待确认。";
    }

    // ==================== 循环结果 ====================

    public enum LoopStatus {
        /** LLM 直接返回了文本回复 */
        TEXT_REPLY,
        /** LLM 返回 null，已降级重试仍失败 */
        LLM_FAILED,
        /** 信息猎手后台任务已受理，需静默返回 */
        SILENT,
        /** 达到循环上限，需要使用 synthesize 汇总 */
        MAX_ROUNDS
    }

    public record Result(
            LoopStatus status,
            String reply,
            List<Message> messages,
            PlanState planState,
            SkillSession session
    ) {
        static Result textReply(String reply, List<Message> messages,
                                PlanState planState, SkillSession session) {
            return new Result(LoopStatus.TEXT_REPLY, reply, messages, planState, session);
        }

        static Result llmFailed(List<Message> messages,
                                PlanState planState, SkillSession session) {
            return new Result(LoopStatus.LLM_FAILED, null, messages, planState, session);
        }

        static Result silent(List<Message> messages,
                             PlanState planState, SkillSession session) {
            return new Result(LoopStatus.SILENT, null, messages, planState, session);
        }

        static Result maxRounds(List<Message> messages,
                                PlanState planState, SkillSession session) {
            return new Result(LoopStatus.MAX_ROUNDS, null, messages, planState, session);
        }
    }

    // ==================== Plan 辅助方法 ====================

    private PlanState planDecisionToState(PlanDecision decision) {
        List<PlanTask> tasks = new ArrayList<>();
        if (decision.getTasks() != null) {
            for (TaskDefinition td : decision.getTasks()) {
                tasks.add(new PlanTask(td.getId(), td.getDescription(), td.getDependencies()));
            }
        }
        return new PlanState(decision.getGoal(), tasks);
    }

    private void injectPlanContext(List<Message> messages, PlanState plan) {
        StringBuilder planSummary = new StringBuilder();
        planSummary.append("【当前计划】\n目标：").append(plan.getGoal()).append("\n\n任务进度：\n");
        for (PlanTask task : plan.getTasks()) {
            planSummary.append("  - ").append(task.getId()).append(": ").append(task.getDescription());
            planSummary.append(" [").append(task.getExecutionStatus()).append("]");
            if (task.getResult() != null && task.getResult().getSummary() != null) {
                planSummary.append(" → ").append(task.getResult().getSummary());
            }
            planSummary.append("\n");
        }
        messages.add(new Message("system", planSummary.toString().strip()));
    }

    // ==================== 防幻觉：定时任务创建检测 ====================

    /**
     * 检查 executedCalls 中是否已包含 create_schedule_task 的调用记录。
     */
    private static boolean wasScheduleTaskCalled(Set<String> executedCalls) {
        for (String sig : executedCalls) {
            if (sig.startsWith("create_schedule_task|")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断用户消息是否要求创建定时提醒/任务。
     */
    private static boolean isScheduleTaskRequest(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return false;
        String msg = userMessage.replaceAll("\\s+", "");
        // 精确匹配：提醒我、帮我提醒、设置提醒
        if (msg.contains("提醒我") || msg.contains("帮我提醒")
                || msg.contains("设置提醒") || msg.contains("定提醒")
                || msg.contains("创建提醒") || msg.contains("添加提醒")) {
            return true;
        }
        // 周期模式：每天/每周/每月/每隔 + 时间
        if ((msg.contains("每天") || msg.contains("每周")
                || msg.contains("每月") || msg.contains("每隔"))
                && msg.matches(".*[0-9时点分秒早中晚上午下午].*")) {
            return true;
        }
        // 显式关键词
        return msg.contains("定时") || msg.contains("闹钟")
                || msg.contains("备忘") || msg.contains("分钟后")
                || msg.contains("小时提醒");
    }

    /**
     * 检查 executedCalls 中是否已包含 cancel_schedule_task 的调用记录。
     */
    static boolean wasScheduleTaskCancelCalled(Set<String> executedCalls) {
        for (String sig : executedCalls) {
            if (sig.startsWith("cancel_schedule_task|")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查 executedCalls 中是否已包含 update_schedule_task 的调用记录。
     */
    static boolean wasScheduleTaskUpdateCalled(Set<String> executedCalls) {
        for (String sig : executedCalls) {
            if (sig.startsWith("update_schedule_task|")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断用户消息是否要求取消/删除定时任务。
     *
     * <p>要求「取消/删除类动词」与「任务/提醒类名词」成对出现，
     * 避免误伤「删除文件」「删除聊天记录」等普通删除请求。
     */
    static boolean isScheduleTaskCancelRequest(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return false;
        String normalized = userMessage.replaceAll("\\s+", "");
        return TASK_CANCEL_REQUEST.matcher(normalized).find();
    }

    /**
     * 判断用户消息是否要求修改/调整定时任务。
     *
     * <p>「改」字较宽泛，必须与任务/时间类名词成对出现，
     * 避免误伤「改一下报告」「帮我改文案」等普通修改请求。
     */
    static boolean isScheduleTaskUpdateRequest(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) return false;
        String normalized = userMessage.replaceAll("\\s+", "");
        return TASK_UPDATE_REQUEST.matcher(normalized).find();
    }

    // ==================== 消息辅助方法 ====================

    private void addAssistantMessage(
            List<Message> messages,
            List<LLMResponse.ToolCall> toolCalls,
            String reasoningContent) {
        if (toolCalls.size() == 1) {
            LLMResponse.ToolCall tc = toolCalls.get(0);
            messages.add(new Message("assistant", tc.arguments(),
                    null, null, null, tc.id(), tc.name(), reasoningContent));
        } else {
            StringBuilder ids = new StringBuilder();
            StringBuilder names = new StringBuilder();
            ArrayNode argsArray = objectMapper.createArrayNode();
            for (LLMResponse.ToolCall tc : toolCalls) {
                if (ids.length() > 0) ids.append(",");
                ids.append(tc.id());
                if (names.length() > 0) names.append(",");
                names.append(tc.name());
                try {
                    argsArray.add(objectMapper.readTree(tc.arguments()));
                } catch (Exception e) {
                    argsArray.add(tc.arguments());
                }
            }
            String combinedArgs;
            try {
                combinedArgs = objectMapper.writeValueAsString(argsArray);
            } catch (Exception e) {
                combinedArgs = "[]";
                log.warn("多 tool_call 参数序列化失败", e);
            }
            messages.add(new Message("assistant", combinedArgs,
                    null, null, null, ids.toString(), names.toString(), reasoningContent));
            log.info("合并 {} 个并行工具调用 | ids={} | names={}",
                    toolCalls.size(), ids, names);
        }
    }
}
