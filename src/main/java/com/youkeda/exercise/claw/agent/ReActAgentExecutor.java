package com.youkeda.exercise.claw.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.youkeda.exercise.claw.agent.memory.ContextStore;
import com.youkeda.exercise.claw.agent.memory.Message;
import com.youkeda.exercise.claw.agent.memory.longterm.LongTermMemoryService;
import com.youkeda.exercise.claw.agent.memory.longterm.MemoryItem;
import com.youkeda.exercise.claw.agent.activity.AgentActivityRecorder;
import com.youkeda.exercise.claw.agent.model.*;
import com.youkeda.exercise.claw.agent.plan.PlanStore;
import com.youkeda.exercise.claw.agent.plan.PlanValidator;
import com.youkeda.exercise.claw.agent.plan.ValidationResult;
import com.youkeda.exercise.claw.agent.tool.LLMFunction;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import com.youkeda.exercise.claw.agent.tool.FunctionExecutionContext;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.youkeda.exercise.claw.agent.skill.*;
import com.youkeda.exercise.claw.wechat.user.WechatUserManager;

/**
 * ReAct 模式 Agent 执行器
 *
 * <p>核心调度器：接收用户消息，通过 LLM + Function Calling 的循环自主决定调用哪些工具，
 * 最终给出回复。支持三路 LLM 输出：文本回复、工具调用、结构化计划。
 *
 * <p>执行流程：
 * <ol>
 *   <li>取对话历史 + 当前用户消息</li>
 *   <li>加载当前会话的 PlanState（如果有）</li>
 *   <li>快速判断：明显不需要工具的闲聊直接 LLM 回复（不含工具定义），跳过后续循环</li>
 *   <li>调 LLM（带所有已注册的 {@link LLMFunction} 定义）</li>
 *   <li>LLM 返回
 *     <ul>
 *       <li>文本 → 结束，保存回复到上下文</li>
 *       <li>tool_calls → 逐个执行 → 更新 TaskResult → 结果追加到消息列表 → 回到步骤 3</li>
 *       <li>plan → PlanValidator 校验 → PlanStore.save → 回到步骤 3</li>
 *     </ul>
 *   </li>
 *   <li>达到最大轮次 → 返回当前可用结果</li>
 * </ol>
 */
@Component
public class ReActAgentExecutor implements AgentExecutor {

    private static final Logger log = LoggerFactory.getLogger(ReActAgentExecutor.class);

    /** 每次请求携带的最大历史消息条数 */
    private static final int MAX_HISTORY = 20;

    /** 工具调用循环最大轮次 */
    private static final int MAX_ROUNDS = 15;

    /** 单次请求允许执行的最大工具数 */
    private static final int MAX_TOOL_CALLS = 16;

    private static final String ERROR_REPLY = "抱歉，处理请求超时，请稍后再试。";

    public static final String SILENT_REPLY = "__HANDLED_WITHOUT_USER_REPLY__";

    private final LLMClient llmClient;
    private final LLMFunctionRegistry functionRegistry;
    private final ContextStore contextStore;
    private final ObjectMapper objectMapper;
    private final PlanStore planStore;
    private final PlanValidator planValidator;
    private final SafetyPolicy safetyPolicy;
    private final LongTermMemoryService longTermMemoryService;
    private final SkillRouter skillRouter;
    private final SkillSessionStore skillSessionStore;
    private final SkillRegistry skillRegistry;
    private final SkillsProperties skillsProperties;
    private final WechatUserManager wechatUserManager;
    private final SkillKnowledgeService skillKnowledgeService;
    private final AgentActivityRecorder activityRecorder;
    private final SkillPendingCoordinator skillPendingCoordinator;
    private final SkillToolFallbackPolicy skillToolFallbackPolicy;
    private final ToolResultStatusParser toolResultStatusParser;

    public ReActAgentExecutor(LLMClient llmClient,
                               LLMFunctionRegistry functionRegistry,
                               ContextStore contextStore,
                               ObjectMapper objectMapper,
                               PlanStore planStore,
                               PlanValidator planValidator,
                               SafetyPolicy safetyPolicy,
                               LongTermMemoryService longTermMemoryService,
                               SkillRouter skillRouter,
                               SkillSessionStore skillSessionStore,
                               SkillRegistry skillRegistry,
                               SkillsProperties skillsProperties,
                               WechatUserManager wechatUserManager,
                               SkillKnowledgeService skillKnowledgeService,
                               AgentActivityRecorder activityRecorder,
                               SkillPendingCoordinator skillPendingCoordinator,
                               SkillToolFallbackPolicy skillToolFallbackPolicy,
                               ToolResultStatusParser toolResultStatusParser) {
        this.llmClient = llmClient;
        this.functionRegistry = functionRegistry;
        this.contextStore = contextStore;
        this.objectMapper = objectMapper;
        this.planStore = planStore;
        this.planValidator = planValidator;
        this.safetyPolicy = safetyPolicy;
        this.longTermMemoryService = longTermMemoryService;
        this.skillRouter = skillRouter;
        this.skillSessionStore = skillSessionStore;
        this.skillRegistry = skillRegistry;
        this.skillsProperties = skillsProperties;
        this.wechatUserManager = wechatUserManager;
        this.skillKnowledgeService = skillKnowledgeService;
        this.activityRecorder = activityRecorder;
        this.skillPendingCoordinator = skillPendingCoordinator;
        this.skillToolFallbackPolicy = skillToolFallbackPolicy;
        this.toolResultStatusParser = toolResultStatusParser;
    }

    @Override
    public String execute(AgentContext context) {
        long requestStartedAt = System.currentTimeMillis();
        String activityRequestId = activityRecorder.beginRequest();
        String userMessage = context.getMessage();

        log.info("AgentExecutor 执行 | message={}", userMessage);

        // Resolve userId
        String userId = context.getUserId();
        if (userId == null || userId.isBlank()) {
            userId = wechatUserManager.getOwnerUserId();
            context.setUserId(userId);
        }

        // Route through SkillRouter
        SkillRoutingResult routingResult = skillRouter.route(userMessage, userId);
        SkillSession session = updateSession(userId, routingResult);
        context.setSkillSession(session);

        // Get active SkillDefinition
        String activeSkillName = session.activeSkill();
        SkillDefinition activeSkill = skillRegistry.find(activeSkillName).orElse(null);
        activityRecorder.skillSelected(activityRequestId, activeSkillName);

        // Build effective tool set
        Set<String> effectiveTools = new LinkedHashSet<>();
        if (skillsProperties.getGlobalTools() != null) {
            effectiveTools.addAll(skillsProperties.getGlobalTools());
        }
        if (activeSkill != null) {
            effectiveTools.addAll(activeSkill.allowedTools());
        }

        // Build dynamic system prompt
        String systemPrompt = buildSystemPrompt(context, activeSkill);

        // 1. 加载 PlanState
        PlanState planState = context.getPlanState() != null
                ? context.getPlanState()
                : planStore.get();
        context.setPlanState(planState);

        // 2. 历史 + 当前消息
        List<Message> history = contextStore.getHistory(MAX_HISTORY);
        boolean continuationRequest = isContinuationRequest(userMessage);
        List<Message> messages = new ArrayList<>();
        for (Message message : history) {
            if (continuationRequest && isLegacyLimitReply(message)) continue;
            messages.add(message);
        }
        if (!historyContainsCurrentMessage(history, userMessage)) {
            messages.add(new Message("user", userMessage));
        }

        // 2.5 长期记忆召回：根据当前消息语义检索相关记忆，注入消息列表
        List<MemoryItem> recalledMemories = longTermMemoryService.recall(userMessage);
        if (!recalledMemories.isEmpty()) {
            String memoryPrompt = longTermMemoryService.buildMemoryPrompt(recalledMemories);
            messages.add(0, new Message("system", memoryPrompt));
            log.debug("长期记忆已注入 | count={}", recalledMemories.size());
        }

        // 3. 快速路径：明显不需要工具的闲聊跳过 tool-calling 循环
        // 当 activeSkill 为 common（或 null）时才走快速路径；有专属 Skill 时跳过
        if (!continuationRequest && (activeSkill == null || "common".equals(activeSkill.name()))) {
            if (isSimpleChat(userMessage)) {
                log.debug("快速通道：用户消息不需工具，走纯对话");
                LLMResponse quickResponse = llmClient.chatWithTools(systemPrompt, messages, List.of());
                if (quickResponse != null && !quickResponse.isToolCall()
                        && quickResponse.getContent() != null
                        && !quickResponse.getContent().isBlank()) {
                    String reply = quickResponse.getContent();
                    log.info("快速对话回复 | reply={}", reply);
                    contextStore.append("assistant", reply);
                    // 异步提取长期记忆
                    final String fastUserMsg = userMessage;
                    final String fastReply = reply;
                    longTermMemoryService.processAndStoreAsync(fastUserMsg, fastReply);
                    // Update session after fast path
                    skillSessionStore.save(userId, session);
                    activityRecorder.requestCompleted(
                            activityRequestId, System.currentTimeMillis() - requestStartedAt);
                    return reply;
                }
                log.warn("快速对话路径异常，回退到工具循环");
            }
        }

        // 4. 按 Skill 过滤可用工具
        FunctionExecutionContext execContext = new FunctionExecutionContext(userMessage, session, userId);
        List<ToolDefinition> tools = functionRegistry.getAvailableDefinitions(effectiveTools, execContext);
        log.info("[Agent Available Tools] skill={} | count={} | tools={}",
                activeSkillName,
                tools.size(),
                tools.stream().map(ToolDefinition::name).toList());

        // 5. tool-calling 循环
        Set<String> executedCalls = new HashSet<>();
        int toolCallCount = 0;
        boolean forceTextResponse = false;
        for (int round = 0; round < MAX_ROUNDS; round++) {
            log.info("工具调用循环第 {} 轮 | messages={}", round + 1, messages.size());

            List<ToolDefinition> roundTools = forceTextResponse ? List.of() : tools;
            LLMResponse response = llmClient.chatWithTools(systemPrompt, messages, roundTools);
            forceTextResponse = false;

            // 工具调用返回 null 时的单轮降级：
            // 本轮先去掉工具重试一次（兼容不支持 tool_calls 的模型），
            // 下一轮仍会带上工具（不同上下文模型可能选择不同行为）
            if (response == null) {
                if (!roundTools.isEmpty()) {
                    log.warn("本轮带工具的 LLM 调用失败，降级不带工具重试（下一轮恢复工具）");
                    response = llmClient.chatWithTools(messages, List.of());
                }
                if (response == null) {
                    log.warn("LLM 调用失败，结束循环");
                    activityRecorder.requestFailed(
                            activityRequestId, "LLM 返回空", System.currentTimeMillis() - requestStartedAt);
                    return handleError();
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
                context.setPlanState(newPlan);
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
                var fallbackCall = skillToolFallbackPolicy.createFallback(
                        routingResult, userMessage, execContext, toolCallCount);
                if (fallbackCall.isPresent()) {
                    log.info("LLM 未调用已选中的信息猎手，Runtime 执行兜底调用");
                    response = new LLMResponse(
                            null, List.of(fallbackCall.get()), "tool_calls");
                } else {
                    // 防幻觉检测：用户要求创建定时提醒，但 create_schedule_task 未被调用
                    // LLM 经常在 reasoning 中"想"了要调用但实际上输出跳过，导致提醒未真正保存
                    if (!wasScheduleTaskCalled(executedCalls) && isScheduleTaskRequest(userMessage)) {
                        log.warn("LLM 幻觉检测：用户要求创建提醒但 create_schedule_task 未被调用，注入提示重试");
                        messages.add(new Message("system",
                                "注意：你刚才未调用 create_schedule_task 工具。"
                                        + "用户明确要求创建定时提醒，请先调用 create_schedule_task 完成创建，"
                                        + "创建成功后再回复用户。不要重复调用已经执行过的 time_query。"));
                        continue;
                    }

                    String reply = response.getContent();
                    log.info("LLM 直接回复 | reply={}", reply);
                    contextStore.append("assistant", reply);
                    if (toolCallCount == 0) {
                        session = skillPendingCoordinator.afterDirectReply(session, routingResult);
                    }
                    // 异步提取长期记忆
                    final String directUserMsg = userMessage;
                    final String directReply = reply;
                    longTermMemoryService.processAndStoreAsync(directUserMsg, directReply);
                    skillSessionStore.save(userId, session);
                    activityRecorder.requestCompleted(
                            activityRequestId, System.currentTimeMillis() - requestStartedAt);
                    return reply;
                }
            }

            // === 分支 3：工具调用 ===
            List<LLMResponse.ToolCall> toolCalls = response.getToolCalls();
            List<String> batchToolNames = toolCalls.stream().map(LLMResponse.ToolCall::name).toList();
            List<String> toolResults = new ArrayList<>();
            boolean executedInBatch = false;

            for (LLMResponse.ToolCall tc : toolCalls) {
                String toolName = tc.name();
                log.info("工具调用 | name={} | args={} | id={}", toolName, tc.arguments(), tc.id());

                LLMFunction fn = functionRegistry.find(toolName);
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
                toolResults.add(result);
            }

            if (isStartedInformationScout(toolCalls, toolResults)) {
                skillSessionStore.save(userId, session);
                activityRecorder.requestCompleted(
                        activityRequestId, System.currentTimeMillis() - requestStartedAt);
                log.info("信息猎手后台任务已受理，本轮保持静默");
                return SILENT_REPLY;
            }

            // 添加 assistant 消息（合并本轮所有 tool_calls）
            if (toolCalls.size() == 1) {
                LLMResponse.ToolCall tc = toolCalls.get(0);
                messages.add(new Message("assistant", tc.arguments(),
                        null, null, null, tc.id(), tc.name(), response.getReasoningContent()));
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
                        null, null, null, ids.toString(), names.toString(),
                        response.getReasoningContent()));
                log.info("合并 {} 个并行工具调用 | ids={} | names={}",
                        toolCalls.size(), ids, names);
            }

            // 添加 tool 结果消息
            for (int i = 0; i < toolCalls.size(); i++) {
                messages.add(new Message("tool", toolResults.get(i),
                        null, null, null, toolCalls.get(i).id(), null));
            }

            // 本轮没有任何工具实际执行 → 下一轮强制文本回复
            if (!executedInBatch) {
                forceTextResponse = true;
            }
        }

        // 6. 达到局部上限，兜底回复
        log.warn("工具调用循环达到上限 {} 轮", MAX_ROUNDS);
        String synthesizedReply = synthesizeWithExistingResults(messages, systemPrompt);
        // 异步提取长期记忆
        final String synthUserMsg = userMessage;
        final String synthReply = synthesizedReply;
        longTermMemoryService.processAndStoreAsync(synthUserMsg, synthReply);
        skillSessionStore.save(userId, session);
        activityRecorder.requestCompleted(
                activityRequestId, System.currentTimeMillis() - requestStartedAt);
        return synthesizedReply;
    }

    // ==================== 错误与兜底 ====================

    private String handleError() {
        contextStore.append("assistant", ERROR_REPLY);
        return ERROR_REPLY;
    }

    /**
     * 达到局部上限时让 LLM 基于已有结果生成最终回复。
     */
    private String synthesizeWithExistingResults(List<Message> messages, String systemPrompt) {
        List<Message> finalMessages = new ArrayList<>(messages);
        finalMessages.add(new Message("system",
                "工具调用轮次已结束。请仅根据已有结果回复："
                        + "若信息不足，提出一个明确问题让用户补充；"
                        + "若信息已齐全，给出当前结果；缺失信息标记待确认，不得编造。"));
        LLMResponse response = llmClient.chatWithTools(systemPrompt, finalMessages, List.of());
        if (response != null && response.getContent() != null
                && !response.getContent().isBlank()) {
            String reply = response.getContent();
            contextStore.append("assistant", reply);
            return reply;
        }
        log.warn("最终汇总仍返回工具调用，使用兜底消息");
        String fallback = "已根据当前可用信息整理方案。尚未核实的信息标记为待确认。";
        contextStore.append("assistant", fallback);
        return fallback;
    }

    // ==================== Plan 辅助方法 ====================

    /**
     * 将 LLM 产出的 {@link PlanDecision} 转为 {@link PlanState}。
     */
    private PlanState planDecisionToState(PlanDecision decision) {
        List<PlanTask> tasks = new ArrayList<>();
        if (decision.getTasks() != null) {
            for (TaskDefinition td : decision.getTasks()) {
                tasks.add(new PlanTask(td.getId(), td.getDescription(), td.getDependencies()));
            }
        }
        return new PlanState(decision.getGoal(), tasks);
    }

    /**
     * 将 PlanState 摘要注入 messages，代替 TeamTrip 阶段的上下文注入。
     */
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

    /**
     * 从工具返回的 JSON 字符串中解析 ResultStatus。
     */
    private ResultStatus parseResultStatus(String resultJson) {
        return toolResultStatusParser.parse(resultJson);
    }

    private boolean isStartedInformationScout(
            List<LLMResponse.ToolCall> toolCalls, List<String> toolResults) {
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

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    /**
     * 检查 executedCalls 中是否已包含 create_schedule_task 的调用记录。
     */
    private boolean wasScheduleTaskCalled(Set<String> executedCalls) {
        for (String sig : executedCalls) {
            if (sig.startsWith("create_schedule_task|")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断用户消息是否要求创建定时提醒/任务。
     * 使用精确匹配，避免"谢谢提醒"等非创建类消息误触发防幻觉逻辑。
     */
    private boolean isScheduleTaskRequest(String userMessage) {
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

    private boolean historyContainsCurrentMessage(List<Message> history, String userMessage) {
        if (history.isEmpty() || userMessage == null) return false;
        Message last = history.get(history.size() - 1);
        if (!"user".equals(last.role()) || last.content() == null) return false;
        return last.content().equals(userMessage) || last.content().equals("[语音]" + userMessage);
    }

    private boolean isContinuationRequest(String userMessage) {
        if (userMessage == null) return false;
        String normalized = userMessage.replaceAll("[\\s，。！!？?]", "");
        return Set.of("继续生成", "继续", "接着生成", "继续完成方案").contains(normalized);
    }

    private boolean isLegacyLimitReply(Message message) {
        if (message == null || !"assistant".equals(message.role()) || message.content() == null) {
            return false;
        }
        return message.content().contains("本轮处理步骤已达到上限")
                || message.content().contains("请回复\"继续生成\"")
                || message.content().contains("请回复“继续生成”");
    }

    /**
     * 快速判断用户消息是否需要调用工具。
     */
    private boolean isSimpleChat(String userMessage) {
        if (userMessage == null || userMessage.trim().length() <= 3) return false;

        String prompt = "你是一个分类器。判断用户消息是否需要调用工具才能完整回答。\n"
                + "需要工具：查天气、查地图/地点/路线、查时间/日期/节假日、搜索网页、"
                + "生成图片、生成文件/文档、语音合成、交通推荐、预算计算、"
                + "查课表/今天课表/导入课表/课程信息/考试安排、"
                + "设置提醒/定时提醒/自定义提醒/创建提醒。\n"
                + "不需要工具：纯粹的聊天、问答、解释、翻译、写作、闲聊、感谢。\n"
                + "如果用户消息很短（如\"好\"\"可以\"\"继续\"），可能是在回应之前提出的方案，"
                + "需要让工具系统处理，返回 NEED_TOOLS。\n"
                + "不确定时返回 NEED_TOOLS。\n"
                + "只返回一个词：NEED_TOOLS 或 CHAT_ONLY。";

        String result = llmClient.chatWithSystemPrompt(prompt, userMessage);
        return "CHAT_ONLY".equals(result != null ? result.trim() : "");
    }

    // ==================== Skill 支持方法 ====================

    /**
     * 根据 SkillRouter 的 routing 结果更新 SkillSession。
     * <ul>
     *   <li>ACTIVATE/SWITCH → 切换 activeSkill</li>
     *   <li>CONTINUE → 高置信度重置不活跃计数，低置信度递增</li>
     *   <li>DEACTIVATE → 创建新 session（回退 common）</li>
     *   <li>NONE → 非 common 时递增不活跃计数</li>
     * </ul>
     */
    private SkillSession updateSession(String userId, SkillRoutingResult routing) {
        java.util.Optional<SkillSession> existing = skillSessionStore.find(userId);
        SkillSession session = existing.orElseGet(() -> SkillSession.create(userId));

        switch (routing.action()) {
            case ACTIVATE, SWITCH -> session = session.withActiveSkill(routing.primarySkill());
            case CONTINUE -> {
                if (routing.confidence() >= 0.3) {
                    session = session.withResetInactivity();
                } else {
                    session = session.withIncrementInactivity();
                }
            }
            case DEACTIVATE -> { session = SkillSession.create(userId); }
            case NONE -> {
                if (!"common".equals(session.activeSkill())) {
                    session = session.withIncrementInactivity();
                }
            }
        }
        skillSessionStore.save(userId, session);
        return session;
    }

    /**
     * 构建动态 system prompt：基础 prompt + Active Skill 上下文。
     * <p>Release 2 还会在此追加知识库上下文。
     */
    private String buildSystemPrompt(AgentContext context, SkillDefinition activeSkill) {
        StringBuilder sb = new StringBuilder();
        sb.append(llmClient.getSystemPrompt()).append("\n\n");

        if (activeSkill != null && activeSkill.systemPromptResource() != null) {
            String skillPrompt = loadSkillPrompt(activeSkill.systemPromptResource());
            if (skillPrompt != null) {
                sb.append("--- 当前上下文 ---\n\n");
                sb.append("[SKILL_CONTEXT]\n");
                sb.append(skillPrompt).append("\n");
                sb.append("[/SKILL_CONTEXT]\n\n");
            }
        }
        if (context.getSkillSession() != null
                && context.getSkillSession().hasPendingAction(
                        SkillPendingCoordinator.START_INFORMATION_SCOUT)) {
            sb.append("[RUNTIME_STATE]\n");
            sb.append("用户当前消息是对信息猎手搜索方向追问的补充回答。\n");
            sb.append("请将当前消息作为 query 调用 information_scout，不要再次追问方向。\n");
            sb.append("[/RUNTIME_STATE]\n\n");
        }
        // RAG knowledge context
        if (skillKnowledgeService != null && activeSkill != null
                && activeSkill.knowledge() != null && activeSkill.knowledge().enabled()) {
            try {
                String knowledge = skillKnowledgeService.recall(context.getMessage(), activeSkill.name());
                if (knowledge != null && !knowledge.isEmpty()) {
                    sb.append(knowledge).append("\n\n");
                }
            } catch (Exception e) {
                log.warn("Failed to recall skill knowledge for: {}", activeSkill.name(), e);
            }
        }

        return sb.toString();
    }

    /**
     * 从 classpath 加载 Skill 的 system prompt 资源文件。
     */
    private String loadSkillPrompt(String resourcePath) {
        try {
            return new String(
                new org.springframework.core.io.ClassPathResource(resourcePath)
                    .getInputStream().readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8
            );
        } catch (Exception e) {
            log.warn("Failed to load skill prompt: {}", resourcePath, e);
            return null;
        }
    }
}
