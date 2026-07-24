package com.youkeda.exercise.claw.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.youkeda.exercise.claw.agent.memory.ContextStore;
import com.youkeda.exercise.claw.agent.memory.Message;
import com.youkeda.exercise.claw.agent.tool.LLMFunction;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import com.youkeda.exercise.claw.agent.tool.FunctionExecutionContext;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.ai.llm.LLMResponse;
import com.youkeda.exercise.claw.ai.llm.ToolDefinition;
import com.youkeda.exercise.claw.teamtrip.TeamTripPlanDraft;
import com.youkeda.exercise.claw.teamtrip.TeamTripPlanService;
import com.youkeda.exercise.claw.teamtrip.TeamTripToolCallPolicy;
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
 * 最终给出回复。替代了旧的 {@code LLMIntentClassifier} + 硬编码路由。
 *
 * <p>执行流程：
 * <ol>
 *   <li>取对话历史 + 当前用户消息</li>
 *   <li>快速判断：明显不需要工具的闲聊直接 LLM 回复（不含工具定义），跳过后续循环</li>
 *   <li>调 LLM（带已注册的 {@link LLMFunction} 定义）</li>
 *   <li>LLM 返回文本 → 结束，保存回复到上下文</li>
 *   <li>LLM 返回 tool_calls → 逐个执行 → 结果追加到消息列表 → 回到步骤 3</li>
 *   <li>达到最大轮次 → 返回超时提示</li>
 * </ol>
 */
@Component
public class ReActAgentExecutor implements AgentExecutor {

    private static final Logger log = LoggerFactory.getLogger(ReActAgentExecutor.class);

    /** 每次请求携带的最大历史消息条数 */
    private static final int MAX_HISTORY = 20;

    /**
     * 工具调用循环最大轮次。
     * 搜索轮次和工具总数另有独立硬限制，这里给保存方案与预算核算预留自动收尾空间。
     */
    private static final int MAX_ROUNDS = 12;

    /** 单次请求允许执行的最大工具数，防止模型陷入循环 */
    private static final int MAX_TOOL_CALLS = 16;

    /** 完整方案最多进行两轮网页研究，且总搜索调用不超过 5 次。 */
    private static final int MAX_WEB_SEARCH_ROUNDS = 2;
    private static final int MAX_WEB_SEARCH_CALLS = 5;

    /**
     * 不受团建阶段限制的通用工具名称。
     * 即使用户处于等待选择/确认等阶段，这些工具也始终对 LLM 可见，
     * 保证用户可以随时要求生成文件、图片或语音。
     */
    private static final Set<String> ALWAYS_AVAILABLE_TOOLS =
            Set.of("file_generate", "image_generate", "text_to_speech", "plan_proposal");

    private static final String ERROR_REPLY = "抱歉，处理请求超时，请稍后再试。";
    private static final String LIMIT_REPLY =
            "已根据当前可用信息整理方案；尚未完成核实的费用会标记为“待确认”。";

    private final LLMClient llmClient;
    private final LLMFunctionRegistry functionRegistry;
    private final ContextStore contextStore;
    private final ObjectMapper objectMapper;
    private final TeamTripToolCallPolicy toolCallPolicy;
    private final TeamTripPlanService teamTripPlanService;

    public ReActAgentExecutor(LLMClient llmClient,
                               LLMFunctionRegistry functionRegistry,
                               ContextStore contextStore,
                               ObjectMapper objectMapper,
                               TeamTripToolCallPolicy toolCallPolicy,
                               TeamTripPlanService teamTripPlanService) {
        this.llmClient = llmClient;
        this.functionRegistry = functionRegistry;
        this.contextStore = contextStore;
        this.objectMapper = objectMapper;
        this.toolCallPolicy = toolCallPolicy;
        this.teamTripPlanService = teamTripPlanService;
    }

    @Override
    public String execute(AgentContext context) {
        String userId = context.getUserId();
        String userMessage = context.getMessage();

        log.info("ReActAgentExecutor 执行 | user={} | message={}", userId, userMessage);

        // 1. 历史 + 当前消息
        List<Message> history = contextStore.getHistory(userId, MAX_HISTORY);
        boolean continuationRequest = isContinuationRequest(userMessage);
        List<Message> messages = new ArrayList<>();
        for (Message message : history) {
            if (continuationRequest && isLegacyLimitReply(message)) continue;
            messages.add(message);
        }
        if (!historyContainsCurrentMessage(history, userMessage)) {
            messages.add(new Message("user", userMessage));
        }
        if (continuationRequest) {
            messages.add(new Message("system",
                    "用户是在要求系统继续完成尚未结束的方案流程。"
                            + "必须从已保存的团建草稿阶段继续，只执行后续必要步骤；"
                            + "不得复述“处理步骤达到上限”或再次要求用户回复“继续生成”；"
                            + "不得重新收集或覆盖用户已经确认的出发地、人数、日期、天数、目的地和预算。"));
        }

        WaitingInputResult waitingInput = handleWaitingInput(userId, userMessage);
        if (waitingInput.handled()) {
            if (waitingInput.reply() != null && !waitingInput.reply().isBlank()) {
                contextStore.append(userId, "assistant", waitingInput.reply());
                return waitingInput.reply();
            }
            messages.add(new Message("system", waitingInput.transitionSummary()));
        }

        // 快速路径：明显不需要工具的闲聊跳过 tool-calling 循环
        // 团建流程正在等待选择、确认或补充时，用户的回复必须先经过状态工具，
        // 不能被“闲聊”分类提前截断，否则只会口头确认而不会更新持久状态。
        boolean waitingForTeamTripInput = toolCallPolicy.shouldReplyWithoutTools(userId);
        if (!continuationRequest && !waitingInput.handled()
                && !waitingForTeamTripInput && isSimpleChat(userMessage)) {
            log.debug("快速通道：用户消息不需工具，走纯对话 | user={}", userId);
            LLMResponse quickResponse = llmClient.chatWithTools(messages, List.of());
            if (quickResponse != null && !quickResponse.isToolCall()
                    && quickResponse.getContent() != null
                    && !quickResponse.getContent().isBlank()) {
                String reply = quickResponse.getContent();
                log.info("快速对话回复 | user={} | reply={}", userId, reply);
                contextStore.append(userId, "assistant", reply);
                return reply;
            }
            // 快速路径异常（LLM 无返回或仍调工具），回退到完整工具循环
            log.warn("快速对话路径异常，回退到工具循环 | user={}", userId);
        }

        // 2. 所有可用工具定义
        List<ToolDefinition> tools = functionRegistry.getAllDefinitions();
        log.debug("可用工具: {}", tools.stream().map(ToolDefinition::name).toList());

        // 3. tool-calling 循环
        Set<String> executedCalls = new HashSet<>();
        int toolCallCount = 0;
        int webSearchRounds = 0;
        int webSearchCallCount = 0;
        boolean forceTextResponse = false;
        for (int round = 0; round < MAX_ROUNDS; round++) {
            log.info("工具调用循环第 {} 轮 | user={} | messages={}", round + 1, userId, messages.size());

            List<ToolDefinition> roundTools = forceTextResponse
                    ? List.of()
                    : selectToolsForStage(userId, tools, webSearchRounds);
            LLMResponse response = llmClient.chatWithTools(messages, roundTools);
            forceTextResponse = false;
            if (response == null) {
                log.warn("LLM 返回空，结束循环 | user={}", userId);
                return handleError(userId);
            }

            if (!response.isToolCall()) {
                // LLM 直接回复文本 → 结束
                String reply = response.getContent();
                log.info("LLM 直接回复 | user={} | reply={}", userId, reply);
                contextStore.append(userId, "assistant", reply);
                return reply;
            }

            // Step 1: 先执行本轮所有工具，收集结果
            List<LLMResponse.ToolCall> toolCalls = response.getToolCalls();
            List<String> batchToolNames = toolCalls.stream().map(LLMResponse.ToolCall::name).toList();
            Set<String> allowedToolNames = roundTools.stream()
                    .map(ToolDefinition::name)
                    .collect(java.util.stream.Collectors.toSet());
            List<String> toolResults = new ArrayList<>();
            boolean executedInBatch = false;
            boolean executedWebSearchInBatch = false;
            boolean requestedUnavailableTool = false;
            for (LLMResponse.ToolCall tc : toolCalls) {
                String toolName = resolveToolName(tc, allowedToolNames);
                log.info("工具调用 | name={} | resolvedName={} | args={} | id={}",
                        tc.name(), toolName, tc.arguments(), tc.id());

                LLMFunction fn = functionRegistry.find(toolName);
                String result;
                String blockedReason = toolCallPolicy.validate(userId, toolName, batchToolNames);
                String callSignature = toolName + "|" + tc.arguments();
                if (!allowedToolNames.contains(toolName)) {
                    requestedUnavailableTool = teamTripPlanService.getDraft(userId) != null;
                    result = policyBlocked("当前阶段不允许调用 " + toolName
                            + "，请只使用本轮提供的工具：" + allowedToolNames);
                } else if (blockedReason != null) {
                    result = policyBlocked(blockedReason);
                } else if ("web_search".equals(toolName)
                        && webSearchCallCount >= MAX_WEB_SEARCH_CALLS) {
                    result = policyBlocked("网页搜索已达到本次方案研究上限，请使用已有结果保存候选方案并核算费用。");
                } else if (toolCallCount >= MAX_TOOL_CALLS) {
                    result = policyBlocked("本次请求工具调用数量已达上限，请使用已有结果生成答复。");
                } else if (!executedCalls.add(callSignature)) {
                    result = policyBlocked("相同工具和参数已经执行过，请使用已有结果，不要重复调用。");
                } else if (fn == null) {
                    log.warn("未找到函数: {}", tc.name());
                    result = "{\"error\":\"未知工具: " + tc.name() + "\"}";
                } else {
                    toolCallCount++;
                    executedInBatch = true;
                    if ("web_search".equals(toolName)) {
                        webSearchCallCount++;
                        executedWebSearchInBatch = true;
                    }
                    result = fn.execute(tc.arguments(), new FunctionExecutionContext(userId, userMessage));
                    if (!"team_trip_plan".equals(toolName)) {
                        teamTripPlanService.recordToolResult(userId, toolName, result);
                    }
                    log.info("工具执行完成 | name={} | result={}", toolName, truncate(result, 200));
                }
                toolResults.add(result);
            }
            if (executedWebSearchInBatch) webSearchRounds++;

            // Step 2: 添加 ONE 条 assistant 消息（含本轮所有 tool_calls）
            if (toolCalls.size() == 1) {
                // 单 tool_call — 原有格式
                LLMResponse.ToolCall tc = toolCalls.get(0);
                messages.add(new Message("assistant", tc.arguments(),
                        null, null, null, tc.id(), tc.name(), response.getReasoningContent()));
            } else {
                // 多 tool_call — 合并为一条 assistant 消息，符合 OpenAI 并行调用规范
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
                    // 序列化失败时用法 fallback
                    combinedArgs = "[]";
                    log.warn("多 tool_call 参数序列化失败", e);
                }
                messages.add(new Message("assistant", combinedArgs,
                        null, null, null, ids.toString(), names.toString(),
                        response.getReasoningContent()));
                log.info("合并 {} 个并行工具调用 | ids={} | names={}",
                        toolCalls.size(), ids, names);
            }

            // Step 3: 添加所有 tool 结果消息
            for (int i = 0; i < toolCalls.size(); i++) {
                messages.add(new Message("tool", toolResults.get(i),
                        null, null, null, toolCalls.get(i).id(), null));
            }
            // 已进入必须等待用户输入的阶段时，只用精简快照生成原有展示结构。
            // 不携带搜索原文和完整工具历史，也不再开放任何工具。
            if (toolCallPolicy.shouldReplyWithoutTools(userId)) {
                String waitingReply = renderWaitingWithOriginalStructure(userId);
                if (waitingReply != null && !waitingReply.isBlank()) {
                    TeamTripPlanDraft draft = teamTripPlanService.getDraft(userId);
                    log.info("等待用户输入，返回原方案展示结构 | user={} | stage={}",
                            userId, draft != null ? draft.getStage() : "UNKNOWN");
                    contextStore.append(userId, "assistant", waitingReply);
                    return waitingReply;
                }
            }
            // 已进入等待用户阶段，或本轮所有调用都被策略/去重阻止时，
            // 下一轮不再提供工具，只允许模型使用已有结果回复用户。
            forceTextResponse = (!executedInBatch && !requestedUnavailableTool)
                    || toolCallPolicy.shouldReplyWithoutTools(userId);
            // 继续下一轮，让 LLM 基于工具结果生成最终回复
        }

        log.warn("工具调用循环达到上限 {} 轮 | user={}", MAX_ROUNDS, userId);
        return synthesizeWithExistingResults(userId, messages);
    }

    /**
     * 异常处理：保存错误回复到上下文并返回
     */
    private String handleError(String userId) {
        contextStore.append(userId, "assistant", ERROR_REPLY);
        return ERROR_REPLY;
    }

    /**
     * 工具轮次达到上限时停止提供工具，让模型使用已有结果给出最佳可用回复，
     * 避免已经完成查询后仍向用户返回统一超时提示。
     */
    private String synthesizeWithExistingResults(String userId, List<Message> messages) {
        List<Message> finalMessages = new ArrayList<>(messages);
        finalMessages.add(new Message("system",
                "工具调用轮次已结束。禁止继续调用工具。请仅根据已有工具结果回复："
                        + "若信息不足或正在等待选择/预算决定，提出一个明确问题；"
                        + "若信息已齐全，给出当前结果；缺失信息标记待确认，不得编造。"));
        LLMResponse response = llmClient.chatWithTools(finalMessages, List.of());
        if (response != null && response.getContent() != null
                && !response.getContent().isBlank()) {
            String reply = response.getContent();
            contextStore.append(userId, "assistant", reply);
            return reply;
        }
        if (response != null && response.isToolCall()) {
            // 内部轮次上限不是用户决策节点，不能要求用户回复“继续生成”。
            log.warn("最终汇总仍返回工具调用，直接输出当前最佳可用方案 | user={} | tools={}",
                    userId, response.getToolCalls().stream().map(LLMResponse.ToolCall::name).toList());
            String bestAvailable = teamTripPlanService.renderBestAvailableReply(userId);
            String reply = bestAvailable != null && !bestAvailable.isBlank()
                    ? bestAvailable : LIMIT_REPLY;
            contextStore.append(userId, "assistant", reply);
            return reply;
        }
        return handleError(userId);
    }

    /**
     * 截断字符串（日志用）
     */
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
                || message.content().contains("请回复“继续生成”")
                || message.content().contains("请回复\"继续生成\"");
    }

    /**
     * 快速判断用户消息是否需要调用工具。
     * <p>超短消息可能是对计划提议的回应，保守地返回 false 进入工具循环。
     * 实际判断由一次不带工具的 LLM 调用完成，只有 LLM 明确判断为 CHAT_ONLY 时才跳过。
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
     * 等待用户决策的阶段使用确定性状态转移，不把“是否调用状态工具”交给模型判断。
     * 无法明确识别的输入仍返回未处理，由正常 LLM 流程澄清。
     */
    private WaitingInputResult handleWaitingInput(String userId, String userMessage) {
        TeamTripPlanDraft draft = teamTripPlanService.getDraft(userId);
        if (draft == null || userMessage == null || userMessage.isBlank()) {
            return WaitingInputResult.unhandled();
        }

        String stage = draft.getStage();
        if ("AWAITING_OPTION_SELECTION".equals(stage)) {
            var args = objectMapper.createObjectNode();
            args.put("action", "select_option");
            args.put("selected_option_id", userMessage.trim());
            JsonNode result = teamTripPlanService.handle(userId, args);
            if ("INVALID_ARGUMENT".equals(result.path("status").asText())) {
                return WaitingInputResult.unhandled();
            }

            TeamTripPlanDraft updated = teamTripPlanService.getDraft(userId);
            log.info("等待阶段直接记录方案选择 | user={} | input={} | selected={} | stage={}",
                    userId, userMessage, updated.getSelectedOptionId(), updated.getStage());
            if (toolCallPolicy.shouldReplyWithoutTools(userId)) {
                return WaitingInputResult.replied(renderWaitingWithOriginalStructure(userId));
            }
            if ("FINALIZABLE".equals(updated.getStage())) {
                return WaitingInputResult.replied(renderFinalizedWithOriginalStructure(userId));
            }
            return WaitingInputResult.transitioned(
                    "用户的方案选择已经写入结构化状态。继续处理所选方案，禁止重新收集需求或生成其他候选方案。");
        }

        if ("AWAITING_BUDGET_DECISION".equals(stage)) {
            String decision = parseBudgetDecision(userMessage);
            if (decision == null) return WaitingInputResult.unhandled();

            var args = objectMapper.createObjectNode();
            args.put("action", "budget_decision");
            args.put("budget_decision", decision);
            JsonNode result = teamTripPlanService.handle(userId, args);
            if ("INVALID_ARGUMENT".equals(result.path("status").asText())) {
                return WaitingInputResult.unhandled();
            }

            TeamTripPlanDraft updated = teamTripPlanService.getDraft(userId);
            log.info("等待阶段直接记录预算决定 | user={} | decision={} | stage={}",
                    userId, decision, updated.getStage());
            if ("ACCEPT_OVERRUN".equals(decision)) {
                return WaitingInputResult.replied(renderFinalizedWithOriginalStructure(userId));
            }
            if (toolCallPolicy.shouldReplyWithoutTools(userId)) {
                return WaitingInputResult.replied(teamTripPlanService.renderWaitingReply(userId));
            }
            return WaitingInputResult.transitioned(
                    "用户的预算决定已经写入结构化状态。只继续处理所选方案，不得重新询问同一预算决定。");
        }

        if ("AWAITING_ADJUSTMENT_PREFERENCE".equals(stage)
                && containsAny(userMessage, "住宿", "活动", "餐饮", "交通")) {
            var args = objectMapper.createObjectNode();
            args.put("action", "budget_decision");
            args.put("budget_decision", "REVISE_TO_BUDGET");
            args.put("adjustment_preferences", userMessage.trim());
            teamTripPlanService.handle(userId, args);
            log.info("等待阶段直接记录调整偏好 | user={} | preference={}", userId, userMessage);
            return WaitingInputResult.transitioned(
                    "用户的调整偏好已经写入结构化状态。只修改并重新核算所选方案，不得展示其他候选方案。");
        }

        return WaitingInputResult.unhandled();
    }

    private String parseBudgetDecision(String userMessage) {
        String normalized = userMessage.replaceAll("[\\s，。！!？?]", "");
        if (containsAny(normalized, "先看看", "有哪些调整", "调整选项")) {
            return "SHOW_ADJUSTMENT_OPTIONS";
        }
        if (containsAny(normalized, "不接受", "预算内", "控制预算", "调整", "太贵")) {
            return "REVISE_TO_BUDGET";
        }
        if (containsAny(normalized, "接受", "同意", "没问题", "按此方案", "就这个",
                "可以超预算")) {
            return "ACCEPT_OVERRUN";
        }
        return null;
    }

    private static boolean containsAny(String value, String... words) {
        if (value == null) return false;
        for (String word : words) {
            if (value.contains(word)) return true;
        }
        return false;
    }

    /** 用户完成方案和预算确认后，只渲染已选方案的完整最终版。 */
    private String renderFinalizedWithOriginalStructure(String userId) {
        String snapshot = teamTripPlanService.buildPresentationSnapshot(userId);
        List<Message> finalMessages = List.of(
                new Message("system",
                        "用户已经完成方案选择和预算确认。不得调用任何工具。"
                                + "只展示快照 options 中的已选方案，直接输出可执行的最终版，"
                                + "不得提及或补写其他候选方案。"
                                + "最终版必须包含方案亮点、按天行程、活动、交通、住宿、餐饮、"
                                + "当地美食推荐、注意事项和最终预算摘要。"
                                + "用户已经接受当前超预算金额，不得再次询问是否接受、是否调整、"
                                + "选择哪个方案或还有什么想法。"
                                + "不要输出 JSON、内部状态、快照说明或生成过程。"),
                new Message("user", "以下是已经确认的最终方案数据，请直接生成最终执行版：\n" + snapshot));
        LLMResponse response = llmClient.chatWithTools(finalMessages, List.of());
        if (response != null && !response.isToolCall()
                && response.getContent() != null && !response.getContent().isBlank()) {
            return response.getContent();
        }
        log.warn("最终方案渲染失败，返回已确认的结构化摘要 | user={}", userId);
        return teamTripPlanService.renderFinalizedReply(userId);
    }

    private String renderWaitingWithOriginalStructure(String userId) {
        String fallback = teamTripPlanService.renderWaitingReply(userId);
        TeamTripPlanDraft draft = teamTripPlanService.getDraft(userId);
        if (draft == null) return fallback;

        String stage = draft.getStage();
        // 补充信息、调整偏好和修订原因都只是一个简短问题，不应再次渲染候选方案。
        if (!"AWAITING_OPTION_SELECTION".equals(stage)
                && !"AWAITING_BUDGET_DECISION".equals(stage)) {
            return fallback;
        }

        String snapshot = teamTripPlanService.buildPresentationSnapshot(userId);
        boolean selectedOptionOnly = !"AWAITING_OPTION_SELECTION".equals(stage)
                && draft.getSelectedOptionId() != null
                && !draft.getSelectedOptionId().isBlank();
        String decisionInstruction = selectedOptionOnly
                ? "用户已经选定方案。只展示快照 options 中的已选方案，不得提及、复述或补写其他候选方案，"
                        + "也不得再次询问用户选择哪个方案；回复末尾只询问用户如何处理当前方案的预算。"
                : "当前仍在候选方案选择阶段。全部候选方案展示完后再询问用户选择；不要擅自替用户选择。";
        List<Message> presentationMessages = List.of(
                new Message("system",
                        "这是方案展示阶段，不得调用任何工具。保持原来的自然方案展示结构和友好语气，"
                                + "不要输出 JSON、DSML 或内部状态字段。"
                                + "直接从面向用户的方案内容开始，不要说“基于快照”“根据已核算快照”"
                                + "“以下是展示回复”，也不要解释内容的生成过程。"
                                + "沿用加入预算功能前的旅游规划风格：自然介绍后，分别按天展示每个方案的"
                                + "方案亮点、行程、活动、交通、住宿、餐饮、美食推荐和注意事项。"
                                + "美食推荐需给出当地特色菜、适合团队的餐厅类型或特色用餐体验，"
                                + "不能只写早餐、午餐、晚餐等泛化安排。"
                                + "不要先做大型预算对比表，也不要输出十段式固定报告。"
                                + "预算只作为每个方案末尾的补充，写预计总费用、人均、预算差额和待确认价格。"
                                + decisionInstruction),
                new Message("user", "请根据以下已核算快照生成面向用户的回复：\n" + snapshot));
        LLMResponse response = llmClient.chatWithTools(presentationMessages, List.of());
        if (response != null && !response.isToolCall()
                && response.getContent() != null && !response.getContent().isBlank()) {
            return response.getContent();
        }
        log.warn("原结构展示生成失败，使用固定格式兜底 | user={}", userId);
        return fallback;
    }

    /**
     * 团建流程按阶段只暴露下一步需要的工具，避免模型在证据已经够用后继续搜索。
     * 普通聊天或尚未建立团建草稿时仍保留全部工具。
     */
    private List<ToolDefinition> selectToolsForStage(String userId,
                                                     List<ToolDefinition> allTools,
                                                     int webSearchRounds) {
        TeamTripPlanDraft draft = teamTripPlanService.getDraft(userId);
        if (draft == null) return allTools;
        if (toolCallPolicy.shouldReplyWithoutTools(userId)) {
            return allTools.stream()
                    // “等待用户”只表示上一轮应停下来提问；用户的新回复到达后，
                    // 必须重新开放状态工具，才能记录选择、预算决定或调整偏好。
                    .filter(t -> "team_trip_plan".equals(t.name())
                            || ALWAYS_AVAILABLE_TOOLS.contains(t.name()))
                    .toList();
        }

        String stage = draft.getStage();
        Set<String> exactNames = new HashSet<>();
        boolean allowMapTools = false;

        switch (stage) {
            case "READY_FOR_DATE", "READY_FOR_DATE_CONTEXT" -> {
                exactNames.add("time_query");
                exactNames.add("team_trip_plan");
                if ("NOT_CALLED".equals(draft.getMapStatus())) allowMapTools = true;
            }
            case "READY_FOR_CONTEXT", "READY_FOR_HOLIDAY" -> {
                if ("NOT_CALLED".equals(draft.getHolidayStatus())) {
                    exactNames.add("holiday_check");
                }
                if ("NOT_CALLED".equals(draft.getMapStatus())) allowMapTools = true;
            }
            case "READY_FOR_MAP" -> allowMapTools = true;
            case "READY_FOR_DISTANCE" -> exactNames.add("map_distance_calculate");
            case "READY_FOR_ROUTE" -> exactNames.add("map_route_planning");
            case "MAP_INSUFFICIENT" -> exactNames.add("web_search");
            case "WEATHER_INSUFFICIENT" ->
                    exactNames.add("web_search");
            case "MAP_READY" -> {
                allowMapTools = true;
                exactNames.add("weather_query");
            }
            case "WEATHER_READY", "READY_FOR_TRANSPORT" ->
                    exactNames.add("transport_recommend");
            case "TRANSPORT_READY", "EVIDENCE_READY" -> {
                exactNames.add("team_trip_plan");
                if (webSearchRounds < MAX_WEB_SEARCH_ROUNDS) {
                    exactNames.add("web_search");
                }
            }
            case "OPTIONS_READY_FOR_COSTING" -> {
                exactNames.add("team_trip_plan");
                exactNames.add("budget_calculator");
                if (webSearchRounds < MAX_WEB_SEARCH_ROUNDS) {
                    exactNames.add("web_search");
                }
            }
            case "COST_PARTIAL", "COST_ERROR", "OPTION_REVISION_REQUIRED" -> {
                exactNames.add("team_trip_plan");
                exactNames.add("budget_calculator");
                if (webSearchRounds < MAX_WEB_SEARCH_ROUNDS) {
                    exactNames.add("web_search");
                }
            }
            default -> {
                return allTools;
            }
        }

        boolean finalAllowMapTools = allowMapTools;
        List<ToolDefinition> selected = allTools.stream()
                .filter(tool -> exactNames.contains(tool.name())
                        || (finalAllowMapTools && tool.name().startsWith("map_"))
                        || ALWAYS_AVAILABLE_TOOLS.contains(tool.name()))
                .toList();
        log.debug("按团建阶段筛选工具 | user={} | stage={} | tools={}",
                userId, stage, selected.stream().map(ToolDefinition::name).toList());
        return selected;
    }

    /**
     * 兼容少数模型把 tool_call id 错放到 function.name 的情况。
     * 仅根据参数结构推断已注册工具，且最终仍受当前阶段允许列表约束。
     */
    private String resolveToolName(LLMResponse.ToolCall call, Set<String> allowedToolNames) {
        if (functionRegistry.find(call.name()) != null) return call.name();
        if (call.name() == null || !call.name().startsWith("call_")) return call.name();

        try {
            JsonNode args = objectMapper.readTree(call.arguments());
            String inferred = null;
            if (args.has("action")) {
                String action = args.path("action").asText();
                if (Set.of("collect", "save_options", "select_option", "combine_options",
                        "revise_option", "budget_decision", "revise", "reset").contains(action)) {
                    inferred = "team_trip_plan";
                } else if (Set.of("get_current_time", "date_calculate", "date_diff").contains(action)) {
                    inferred = "time_query";
                }
            } else if (args.has("plans")) {
                inferred = "budget_calculator";
            } else if (args.has("query")) {
                inferred = "web_search";
            } else if (args.has("city") || args.has("location")) {
                inferred = "weather_query";
            }
            if (inferred != null && functionRegistry.find(inferred) != null) {
                log.warn("根据参数恢复异常工具名 | original={} | inferred={}", call.name(), inferred);
                return inferred;
            }
        } catch (Exception e) {
            log.debug("异常工具名参数无法解析 | name={}", call.name());
        }
        return call.name();
    }

    private record WaitingInputResult(boolean handled, String reply, String transitionSummary) {
        private static WaitingInputResult unhandled() {
            return new WaitingInputResult(false, null, null);
        }

        private static WaitingInputResult replied(String reply) {
            return new WaitingInputResult(true, reply, null);
        }

        private static WaitingInputResult transitioned(String summary) {
            return new WaitingInputResult(true, null, summary);
        }
    }
}
