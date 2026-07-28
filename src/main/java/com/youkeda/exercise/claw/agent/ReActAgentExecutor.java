package com.youkeda.exercise.claw.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.youkeda.exercise.claw.agent.memory.ContextStore;
import com.youkeda.exercise.claw.agent.memory.Message;
import com.youkeda.exercise.claw.agent.memory.longterm.LongTermMemoryService;
import com.youkeda.exercise.claw.agent.memory.longterm.MemoryItem;
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
import java.util.List;
import java.util.Set;

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

    private final LLMClient llmClient;
    private final LLMFunctionRegistry functionRegistry;
    private final ContextStore contextStore;
    private final ObjectMapper objectMapper;
    private final PlanStore planStore;
    private final PlanValidator planValidator;
    private final SafetyPolicy safetyPolicy;
    private final LongTermMemoryService longTermMemoryService;

    public ReActAgentExecutor(LLMClient llmClient,
                               LLMFunctionRegistry functionRegistry,
                               ContextStore contextStore,
                               ObjectMapper objectMapper,
                               PlanStore planStore,
                               PlanValidator planValidator,
                               SafetyPolicy safetyPolicy,
                               LongTermMemoryService longTermMemoryService) {
        this.llmClient = llmClient;
        this.functionRegistry = functionRegistry;
        this.contextStore = contextStore;
        this.objectMapper = objectMapper;
        this.planStore = planStore;
        this.planValidator = planValidator;
        this.safetyPolicy = safetyPolicy;
        this.longTermMemoryService = longTermMemoryService;
    }

    @Override
    public String execute(AgentContext context) {
        String userMessage = context.getMessage();

        log.info("AgentExecutor 执行 | message={}", userMessage);

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
        if (!continuationRequest && isSimpleChat(userMessage)) {
            log.debug("快速通道：用户消息不需工具，走纯对话");
            LLMResponse quickResponse = llmClient.chatWithTools(messages, List.of());
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
                return reply;
            }
            log.warn("快速对话路径异常，回退到工具循环");
        }

        // 4. 所有可用工具定义（不再按 stage 筛选）
        List<ToolDefinition> tools = functionRegistry.getAllDefinitions();
        log.debug("可用工具: {}", tools.stream().map(ToolDefinition::name).toList());

        // 5. tool-calling 循环
        Set<String> executedCalls = new HashSet<>();
        int toolCallCount = 0;
        boolean forceTextResponse = false;
        for (int round = 0; round < MAX_ROUNDS; round++) {
            log.info("工具调用循环第 {} 轮 | messages={}", round + 1, messages.size());

            List<ToolDefinition> roundTools = forceTextResponse ? List.of() : tools;
            LLMResponse response = llmClient.chatWithTools(messages, roundTools);
            forceTextResponse = false;
            if (response == null) {
                log.warn("LLM 返回空，结束循环");
                return handleError();
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
                String reply = response.getContent();
                log.info("LLM 直接回复 | reply={}", reply);
                contextStore.append("assistant", reply);
                // 异步提取长期记忆
                final String directUserMsg = userMessage;
                final String directReply = reply;
                longTermMemoryService.processAndStoreAsync(directUserMsg, directReply);
                return reply;
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
                }
                // 安全检查阻止
                else if (blockedReason != null) {
                    result = policyBlocked(blockedReason);
                }
                // 工具调用数量上限
                else if (toolCallCount >= MAX_TOOL_CALLS) {
                    result = policyBlocked("本次请求工具调用数量已达上限，请使用已有结果生成答复。");
                }
                // 去重（相同工具 + 相同参数）
                else if (!executedCalls.add(callSignature)) {
                    result = policyBlocked("相同工具和参数已经执行过，请使用已有结果，不要重复调用。");
                }
                // 执行
                else {
                    toolCallCount++;
                    executedInBatch = true;
                    result = fn.execute(tc.arguments(), new FunctionExecutionContext(userMessage));
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
        String synthesizedReply = synthesizeWithExistingResults(messages);
        // 异步提取长期记忆
        final String synthUserMsg = userMessage;
        final String synthReply = synthesizedReply;
        longTermMemoryService.processAndStoreAsync(synthUserMsg, synthReply);
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
    private String synthesizeWithExistingResults(List<Message> messages) {
        List<Message> finalMessages = new ArrayList<>(messages);
        finalMessages.add(new Message("system",
                "工具调用轮次已结束。请仅根据已有结果回复："
                        + "若信息不足，提出一个明确问题让用户补充；"
                        + "若信息已齐全，给出当前结果；缺失信息标记待确认，不得编造。"));
        LLMResponse response = llmClient.chatWithTools(finalMessages, List.of());
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
        if (resultJson == null || resultJson.isBlank()) return ResultStatus.FAILED;
        try {
            JsonNode node = objectMapper.readTree(resultJson);
            if (node.has("error")) return ResultStatus.FAILED;
            String status = node.path("status").asText("SUCCESS").toUpperCase();
            return switch (status) {
                case "SUCCESS" -> ResultStatus.SUCCESS;
                case "PARTIAL" -> ResultStatus.PARTIAL;
                case "BLOCKED" -> ResultStatus.BLOCKED;
                default -> ResultStatus.FAILED;
            };
        } catch (Exception e) {
            return ResultStatus.SUCCESS; // 非 JSON 工具输出视为成功
        }
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
                + "生成图片、生成文件/文档、语音合成、交通推荐、预算计算。\n"
                + "不需要工具：纯粹的聊天、问答、解释、翻译、写作、闲聊、感谢。\n"
                + "如果用户消息很短（如\"好\"\"可以\"\"继续\"），可能是在回应之前提出的方案，"
                + "需要让工具系统处理，返回 NEED_TOOLS。\n"
                + "不确定时返回 NEED_TOOLS。\n"
                + "只返回一个词：NEED_TOOLS 或 CHAT_ONLY。";

        String result = llmClient.chatWithSystemPrompt(prompt, userMessage);
        return "CHAT_ONLY".equals(result != null ? result.trim() : "");
    }
}
