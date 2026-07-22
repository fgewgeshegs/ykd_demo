package com.youkeda.exercise.claw.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.youkeda.exercise.claw.common.PromptLoader;
import com.youkeda.exercise.claw.agent.memory.Message;
import com.youkeda.exercise.claw.ai.llm.LLMResponse;
import com.youkeda.exercise.claw.ai.llm.ToolDefinition;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * LLM 客户端
 *
 * 封装大模型 HTTP 调用，兼容 OpenAI 协议格式
 */
@Component
public class LLMClient {

    private static final int TIMEOUT_SECONDS = 30;
    private static final String SYSTEM_PROMPT_PATH = "prompts/system-prompt.txt";
    private static final String DEFAULT_SYSTEM_PROMPT = "你是 Claw助手，一个智能AI助手。";

    private static final Logger log = LoggerFactory.getLogger(LLMClient.class);

    private final LLMProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final PromptLoader promptLoader;

    private String systemPrompt;

    public LLMClient(LLMProperties properties, ObjectMapper objectMapper, PromptLoader promptLoader) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.promptLoader = promptLoader;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    @PostConstruct
    public void init() {
        this.systemPrompt = promptLoader.load(SYSTEM_PROMPT_PATH, DEFAULT_SYSTEM_PROMPT);
    }

    /**
     * 调用大模型生成回复（无历史消息，单轮对话）
     *
     * @param userId 用户标识（用于日志）
     * @param text   用户消息
     * @return 模型回复内容，调用失败时返回 null
     */
    public String chat(String userId, String text) {
        return chat(userId, text, List.of());
    }

    /**
     * 调用大模型生成回复（带历史消息，多轮对话）
     *
     * @param userId  用户标识（用于日志）
     * @param text    用户消息
     * @param history 历史消息列表（按时间正序）
     * @return 模型回复内容，调用失败时返回 null
     */
    public String chat(String userId, String text, List<Message> history) {
        return callLLM(systemPrompt, text, history);
    }

    /**
     * 使用自定义系统提示词调用大模型（带历史消息）
     *
     * @param systemPrompt 自定义系统提示词
     * @param text         用户消息
     * @param history      历史消息列表（按时间正序），为空时等价于无历史调用
     * @return 模型回复内容，调用失败时返回 null
     */
    public String chatWithSystemPrompt(String systemPrompt, String text, List<Message> history) {
        return callLLM(systemPrompt, text, history != null ? history : List.of());
    }

    /**
     * 使用自定义系统提示词调用大模型（无历史消息）
     *
     * @param systemPrompt 自定义系统提示词
     * @param text         用户消息
     * @return 模型回复内容，调用失败时返回 null
     */
    public String chatWithSystemPrompt(String systemPrompt, String text) {
        return callLLM(systemPrompt, text, List.of());
    }

    /**
     * 调用大模型（内部方法）
     */
    private String callLLM(String systemPrompt, String text, List<Message> history) {
        try {
            // 1. 构建请求体
            String requestBody = buildRequestBody(systemPrompt, text, history);
            log.info("调用LLM，message={}，historySize={}", text, history.size());

            // 2. 发送 HTTP 请求
            String url = properties.getBaseUrl() + "/chat/completions";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            // 3. 解析响应
            String reply = parseResponse(response.body());
            log.info("LLM响应成功");
            return reply;

        } catch (Exception e) {
            log.error("LLM调用失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 构建请求 JSON 体
     */
    private String buildRequestBody(String systemPrompt, String text, List<Message> history) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.getModel());

        ArrayNode messages = root.putArray("messages");

        // system prompt
        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);

        // history messages
        for (Message msg : history) {
            ObjectNode historyMsg = messages.addObject();
            historyMsg.put("role", msg.role());
            historyMsg.put("content", msg.content());
        }

        // current user message
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", text);

        return objectMapper.writeValueAsString(root);
    }

    /**
     * 解析 LLM 响应 JSON，提取回复文本
     */
    private String parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        JsonNode choices = root.get("choices");
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode message = choices.get(0).get("message");
            if (message != null) {
                JsonNode content = message.get("content");
                if (content != null) {
                    return content.asText();
                }
            }
        }

        log.warn("LLM 响应格式异常: {}", responseBody);
        return null;
    }

    // ==================== Tool Calling 支持 ====================

    /**
     * 带工具定义的 LLM 调用
     *
     * @param messages 完整消息列表（已包含 system prompt 之外的所有 user/assistant/tool 消息）
     * @param tools    工具定义列表（为空时等价于普通 chat）
     * @return 结构化响应（可能包含 {@link LLMResponse.ToolCall}），失败返回 null
     */
    public LLMResponse chatWithTools(List<Message> messages, List<ToolDefinition> tools) {
        try {
            String requestBody = buildRequestBodyWithTools(messages, tools);
            log.debug("LLM 请求（含 {} 个工具定义）", tools != null ? tools.size() : 0);

            String url = properties.getBaseUrl() + "/chat/completions";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            String responseBody = response.body();
            LLMResponse result = parseStructuredResponse(responseBody);
            if (result == null) {
                log.warn("LLM 响应解析失败 | status={} | body={}",
                        response.statusCode(), truncate(responseBody, 500));
            }
            return result;

        } catch (Exception e) {
            log.error("LLM 调用失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 构建含 tools 参数的请求 JSON 体
     */
    private String buildRequestBodyWithTools(List<Message> messages,
                                              List<ToolDefinition> tools) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.getModel());

        ArrayNode msgArray = root.putArray("messages");

        // system prompt
        ObjectNode sysNode = msgArray.addObject();
        sysNode.put("role", "system");
        sysNode.put("content", systemPrompt);

        // 消息列表（按 role 分三种序列化）
        for (Message msg : messages) {
            msgArray.add(serializeMessage(msg));
        }

        // tools 定义
        if (tools != null && !tools.isEmpty()) {
            ArrayNode toolsArray = root.putArray("tools");
            for (ToolDefinition def : tools) {
                ObjectNode toolNode = toolsArray.addObject();
                toolNode.put("type", "function");
                ObjectNode func = toolNode.putObject("function");
                func.put("name", def.name());
                func.put("description", def.description());
                func.set("parameters", def.parameters());
            }
        }

        return objectMapper.writeValueAsString(root);
    }

    /**
     * 将单条 Message 序列化为 LLM 协议的 JSON 节点
     * <p>按 role 分三种序列化策略：
     * <ul>
     *   <li>{@code "user"} — 常规内容</li>
     *   <li>{@code "assistant"} — 可能携带 {@code tool_calls}</li>
     *   <li>{@code "tool"} — 工具调用结果，带 {@code tool_call_id}</li>
     * </ul>
     */
    private ObjectNode serializeMessage(Message msg) {
        ObjectNode node = objectMapper.createObjectNode();

        switch (msg.role()) {
            case "user" -> {
                node.put("role", "user");
                node.put("content", msg.content() != null ? msg.content() : "");
            }
            case "assistant" -> {
                node.put("role", "assistant");
                if (msg.isToolCall()) {
                    node.putNull("content");
                    ArrayNode tcs = node.putArray("tool_calls");

                    String tcId = msg.toolCallId();
                    if (tcId != null && tcId.contains(",")) {
                        // 多 tool_call：解析逗号分隔的 ID 列表和 JSON 数组参数
                        String[] ids = tcId.split(",", -1);
                        String[] names = msg.toolName() != null
                                ? msg.toolName().split(",", -1)
                                : new String[ids.length];
                        JsonNode argsArray;
                        try {
                            argsArray = objectMapper.readTree(msg.content());
                        } catch (Exception e) {
                            log.warn("多 tool_call 参数解析失败: {}", e.getMessage());
                            // 降级：回退到单 tool_call 逻辑
                            ObjectNode tc = tcs.addObject();
                            tc.put("id", ids[0].trim());
                            tc.put("type", "function");
                            ObjectNode func = tc.putObject("function");
                            func.put("name", msg.toolName());
                            func.put("arguments", msg.content());
                            break;
                        }

                        for (int i = 0; i < ids.length; i++) {
                            ObjectNode tc = tcs.addObject();
                            tc.put("id", ids[i].trim());
                            tc.put("type", "function");
                            ObjectNode func = tc.putObject("function");
                            func.put("name", i < names.length ? names[i].trim() : "unknown");
                            JsonNode argNode = i < argsArray.size() ? argsArray.get(i) : null;
                            func.put("arguments", argNode != null
                                    ? (argNode instanceof TextNode ? ((TextNode) argNode).asText() : argNode.toString())
                                    : "{}");
                        }
                    } else {
                        // 单 tool_call（原有逻辑）
                        ObjectNode tc = tcs.addObject();
                        tc.put("id", tcId);
                        tc.put("type", "function");
                        ObjectNode func = tc.putObject("function");
                        func.put("name", msg.toolName());
                        func.put("arguments", msg.content());
                    }
                } else {
                    node.put("content", msg.content() != null ? msg.content() : "");
                }
            }
            case "tool" -> {
                node.put("role", "tool");
                node.put("content", msg.content() != null ? msg.content() : "");
                node.put("tool_call_id", msg.toolCallId());
            }
            default -> {
                // 旧格式兼容（如 media 等自定义角色）
                node.put("role", msg.role());
                node.put("content", msg.content() != null ? msg.content() : "");
            }
        }
        return node;
    }

    /**
     * 解析 LLM 响应，支持 tool_calls
     */
    private LLMResponse parseStructuredResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.size() == 0) {
            log.warn("LLM 响应无 choices: {}", responseBody);
            return null;
        }

        JsonNode message = choices.get(0).get("message");
        if (message == null) {
            log.warn("LLM 响应无 message: {}", responseBody);
            return null;
        }

        String finishReason = choices.get(0).has("finish_reason")
                ? choices.get(0).get("finish_reason").asText() : "stop";

        // content（tool_calls 时可能为 null）
        String content = message.has("content") && !message.get("content").isNull()
                ? message.get("content").asText() : null;

        // tool_calls
        List<LLMResponse.ToolCall> toolCalls = new ArrayList<>();
        JsonNode tcs = message.get("tool_calls");
        if (tcs != null && tcs.isArray()) {
            for (JsonNode tc : tcs) {
                JsonNode func = tc.get("function");
                if (func != null) {
                    toolCalls.add(new LLMResponse.ToolCall(
                            tc.get("id").asText(),
                            tc.has("type") ? tc.get("type").asText() : "function",
                            func.get("name").asText(),
                            func.get("arguments").asText()));
                }
            }
        }

        return new LLMResponse(content, toolCalls, finishReason);
    }

    /**
     * 截断字符串（日志用）
     */
    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
