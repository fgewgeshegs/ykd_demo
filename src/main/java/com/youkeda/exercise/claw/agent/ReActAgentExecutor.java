package com.youkeda.exercise.claw.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.youkeda.exercise.claw.agent.memory.ContextStore;
import com.youkeda.exercise.claw.agent.memory.Message;
import com.youkeda.exercise.claw.agent.tool.LLMFunction;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.ai.llm.LLMResponse;
import com.youkeda.exercise.claw.ai.llm.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ReAct 模式 Agent 执行器
 *
 * <p>核心调度器：接收用户消息，通过 LLM + Function Calling 的循环自主决定调用哪些工具，
 * 最终给出回复。替代了旧的 {@code LLMIntentClassifier} + 硬编码路由。
 *
 * <p>执行流程：
 * <ol>
 *   <li>取对话历史 + 当前用户消息</li>
 *   <li>调 LLM（带所有已注册的 {@link LLMFunction} 定义）</li>
 *   <li>LLM 返回文本 → 结束，保存回复到上下文</li>
 *   <li>LLM 返回 tool_calls → 逐个执行 → 结果追加到消息列表 → 回到步骤 2</li>
 *   <li>达到最大轮次 → 返回超时提示</li>
 * </ol>
 *
 * <p>对应架构演进路线中的 {@code ReActAgentExecutor} 阶段：
 * {@code SimpleAgentExecutor → IntentAgentExecutor → ReActAgentExecutor}
 */
@Component
public class ReActAgentExecutor implements AgentExecutor {

    private static final Logger log = LoggerFactory.getLogger(ReActAgentExecutor.class);

    /** 每次请求携带的最大历史消息条数 */
    private static final int MAX_HISTORY = 20;

    /** 工具调用循环最大轮次 */
    private static final int MAX_ROUNDS = 10;

    private static final String ERROR_REPLY = "抱歉，处理请求超时，请稍后再试。";

    private final LLMClient llmClient;
    private final LLMFunctionRegistry functionRegistry;
    private final ContextStore contextStore;
    private final ObjectMapper objectMapper;

    public ReActAgentExecutor(LLMClient llmClient,
                               LLMFunctionRegistry functionRegistry,
                               ContextStore contextStore,
                               ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.functionRegistry = functionRegistry;
        this.contextStore = contextStore;
        this.objectMapper = objectMapper;
    }

    @Override
    public String execute(AgentContext context) {
        String userId = context.getUserId();
        String userMessage = context.getMessage();

        log.info("ReActAgentExecutor 执行 | user={} | message={}", userId, userMessage);

        // 1. 历史 + 当前消息
        List<Message> history = contextStore.getHistory(userId, MAX_HISTORY);
        List<Message> messages = new ArrayList<>(history);
        messages.add(new Message("user", userMessage));

        // 2. 所有可用工具定义
        List<ToolDefinition> tools = functionRegistry.getAllDefinitions();
        log.debug("可用工具: {}", tools.stream().map(ToolDefinition::name).toList());

        // 3. tool-calling 循环
        for (int round = 0; round < MAX_ROUNDS; round++) {
            log.info("工具调用循环第 {} 轮 | user={} | messages={}", round + 1, userId, messages.size());

            LLMResponse response = llmClient.chatWithTools(messages, tools);
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
            List<String> toolResults = new ArrayList<>();
            for (LLMResponse.ToolCall tc : toolCalls) {
                log.info("工具调用 | name={} | args={} | id={}",
                        tc.name(), tc.arguments(), tc.id());

                LLMFunction fn = functionRegistry.find(tc.name());
                String result;
                if (fn == null) {
                    log.warn("未找到函数: {}", tc.name());
                    result = "{\"error\":\"未知工具: " + tc.name() + "\"}";
                } else {
                    result = fn.execute(tc.arguments());
                    log.info("工具执行完成 | name={} | result={}", tc.name(), truncate(result, 200));
                }
                toolResults.add(result);
            }

            // Step 2: 添加 ONE 条 assistant 消息（含本轮所有 tool_calls）
            if (toolCalls.size() == 1) {
                // 单 tool_call — 原有格式
                LLMResponse.ToolCall tc = toolCalls.get(0);
                messages.add(new Message("assistant", tc.arguments(),
                        null, null, null, tc.id(), tc.name()));
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
                        null, null, null, ids.toString(), names.toString()));
                log.info("合并 {} 个并行工具调用 | ids={} | names={}",
                        toolCalls.size(), ids, names);
            }

            // Step 3: 添加所有 tool 结果消息
            for (int i = 0; i < toolCalls.size(); i++) {
                messages.add(new Message("tool", toolResults.get(i),
                        null, null, null, toolCalls.get(i).id(), null));
            }
            // 继续下一轮，让 LLM 基于工具结果生成最终回复
        }

        log.warn("工具调用循环达到上限 {} 轮 | user={}", MAX_ROUNDS, userId);
        return handleError(userId);
    }

    /**
     * 异常处理：保存错误回复到上下文并返回
     */
    private String handleError(String userId) {
        contextStore.append(userId, "assistant", ERROR_REPLY);
        return ERROR_REPLY;
    }

    /**
     * 截断字符串（日志用）
     */
    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
