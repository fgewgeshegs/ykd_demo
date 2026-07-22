package com.youkeda.exercise.claw.ai.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.ai.llm.LLMResponse;
import com.youkeda.exercise.claw.ai.llm.ToolCall;
import com.youkeda.exercise.claw.ai.llm.ToolResult;
import com.youkeda.exercise.claw.agent.memory.ContextStore;
import com.youkeda.exercise.claw.agent.memory.Message;
import com.youkeda.exercise.claw.agent.tool.FunctionTool;
import com.youkeda.exercise.claw.agent.tool.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天服务
 *
 * 职责：封装多轮对话的业务逻辑，通过 ContextStore 维护上下文，
 * 调用 LLMClient 完成模型交互
 */
@Slf4j
@Service
public class ChatService {

    /** 每次请求携带的最大历史消息条数 */
    private static final int MAX_HISTORY = 20;

    private final LLMClient llmClient;
    private final ContextStore contextStore;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    public ChatService(LLMClient llmClient, ContextStore contextStore,
                       ToolRegistry toolRegistry, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.contextStore = contextStore;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
    }

    /**
     * 生成对话回复（带上下文记忆）
     *
     * @param userId  用户标识
     * @param message 用户消息
     * @return 模型回复，失败时返回 null
     */
    public String chat(String userId, String message) {
        log.info("ChatService 开始处理 | user={} | text={}", userId, message);

        try {
            // 1. 获取历史上下文
            List<Message> history = contextStore.getHistory(userId, MAX_HISTORY);
            log.debug("获取历史消息 | user={} | historySize={}", userId, history.size());

            // 2. 调用 LLM（带历史）
            String reply = llmClient.chat(userId, message, history);
            if (reply == null || reply.isEmpty()) {
                log.warn("ChatService 回复为空 | user={}", userId);
                return null;
            }

            // 3. 保存回复到上下文（用户消息已由 WechatMessageService 统一存储）
            contextStore.append(userId, "assistant", reply);

            log.info("ChatService 处理完成 | user={}", userId);
            return reply;
        } catch (Exception e) {
            log.error("ChatService 处理异常 | user={} | error={}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * 带 Function Calling 的聊天
     *
     * 流程：
     * 1. 收集所有 FunctionTool 的工具定义
     * 2. 如果没有注册任何 FunctionTool，退化为普通 chat
     * 3. 第一轮 LLM 调用（带 tools）
     * 4. 如果 LLM 返回文本 → 直接回复
     * 5. 如果 LLM 返回 tool_calls → 执行工具 → 第二轮 LLM 调用 → 生成最终回复
     *
     * @param userId  用户标识
     * @param message 用户消息
     * @return 模型回复，失败时返回 null
     */
    public String chatWithTools(String userId, String message) {
        log.info("ChatService.chatWithTools 开始处理 | user={} | text={}", userId, message);

        try {
            // 1. 收集工具定义
            List<String> toolDefs = toolRegistry.getFunctionToolDefinitions();

            // 2. 没有 FunctionTool 则退化为普通 chat
            if (toolDefs.isEmpty()) {
                log.debug("无 FunctionTool 注册，退化为普通 chat");
                return chat(userId, message);
            }

            // 3. 获取历史
            List<Message> history = contextStore.getHistory(userId, MAX_HISTORY);

            // 4. 第一轮 LLM 调用
            LLMResponse llmResponse = llmClient.chatWithTools(userId, message, history, toolDefs);
            if (llmResponse == null) {
                log.warn("LLM 第一轮调用返回 null，退化为普通 chat");
                return chat(userId, message);
            }

            // 5. 直接文本回复
            if (llmResponse.isText()) {
                String reply = llmResponse.getText();
                if (reply != null && !reply.isEmpty()) {
                    contextStore.append(userId, "assistant", reply);
                    log.info("ChatService.chatWithTools 直接文本回复 | user={}", userId);
                }
                return reply;
            }

            // 6. 工具调用
            if (llmResponse.isToolCalls()) {
                List<ToolCall> toolCalls = llmResponse.getToolCalls();
                List<ToolResult> toolResults = new ArrayList<>();

                for (ToolCall call : toolCalls) {
                    log.info("执行工具调用 | tool={} | args={}", call.functionName(), call.arguments());
                    FunctionTool tool = toolRegistry.findFunctionTool(call.functionName());
                    if (tool != null) {
                        try {
                            JsonNode args = objectMapper.readTree(call.arguments());
                            String result = tool.executeFunction(args);
                            toolResults.add(new ToolResult(call.id(), result));
                            log.info("工具执行成功 | tool={} | result={}", call.functionName(), result);
                        } catch (Exception e) {
                            log.error("工具执行异常 | tool={} | error={}", call.functionName(), e.getMessage());
                            toolResults.add(new ToolResult(call.id(),
                                    "{\"error\": \"" + e.getMessage() + "\"}"));
                        }
                    } else {
                        log.warn("未找到工具: {}", call.functionName());
                        toolResults.add(new ToolResult(call.id(),
                                "{\"error\": \"工具不存在: " + call.functionName() + "\"}"));
                    }
                }

                // 7. 第二轮 LLM 调用
                String finalReply = llmClient.chatWithToolResults(
                        userId, message, history, toolDefs, toolCalls, toolResults);

                if (finalReply != null && !finalReply.isEmpty()) {
                    contextStore.append(userId, "assistant", finalReply);
                    log.info("ChatService.chatWithTools 工具调用后回复 | user={}", userId);
                }
                return finalReply;
            }

            return null;

        } catch (Exception e) {
            log.error("ChatService.chatWithTools 异常 | user={} | error={}", userId, e.getMessage());
            // 降级为普通 chat
            return chat(userId, message);
        }
    }
}
