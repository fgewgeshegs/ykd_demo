package com.youkeda.exercise.claw.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.youkeda.exercise.claw.infrastructure.common.PromptLoader;
import com.youkeda.exercise.claw.agent.memory.Message;
import com.youkeda.exercise.claw.agent.memory.MessageRole;
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
import java.util.function.Supplier;

/**
 * LLM 客户端
 *
 * 封装大模型 HTTP 调用，兼容 OpenAI 协议格式
 */
@Component
public class LLMClient {

    private static final int TIMEOUT_SECONDS = 60;
    private static final String SYSTEM_PROMPT_PATH = "prompts/system-prompt.txt";
    private static final String DEFAULT_SYSTEM_PROMPT = "你是 Claw助手，一个智能AI助手。";

    /** LLM 调用最大重试次数（指数退避） */
    private static final int MAX_RETRIES = 3;
    /** 重试初始退避延迟（毫秒） */
    private static final long RETRY_BASE_DELAY_MS = 300;

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
     * @param text   用户消息
     * @return 模型回复内容，调用失败时返回 null
     */
    public String chat(String text) {
        return chat(text, List.of());
    }

    /**
     * 调用大模型生成回复（带历史消息，多轮对话）
     *
     * @param text    用户消息
     * @param history 历史消息列表（按时间正序）
     * @return 模型回复内容，调用失败时返回 null
     */
    public String chat(String text, List<Message> history) {
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
     * 使用自定义系统提示词调用大模型，并限制本次输出 token。
     */
    public String chatWithSystemPrompt(String systemPrompt, String text, int maxTokens) {
        return callLLM(systemPrompt, text, List.of(), maxTokens);
    }

    /**
     * 调用大模型（内部方法）
     */
    private String callLLM(String systemPrompt, String text, List<Message> history) {
        return callLLM(systemPrompt, text, history, 0);
    }

    private String callLLM(String systemPrompt, String text, List<Message> history,
                           int maxTokens) {
        try {
            // 1. 构建请求体
            String requestBody = buildRequestBody(systemPrompt, text, history, maxTokens);
            log.info("调用LLM，message={}，historySize={}", text, history.size());

            // 2. 发送 + 解析（含重试）
            return retryExecute(() -> {
                try {
                    return doCallLLM(requestBody);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, MAX_RETRIES);

        } catch (Exception e) {
            log.error("LLM调用失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 单次 LLM HTTP 调用（不含重试）。非 2xx 抛 {@link LLMHttpException} 触发外层重试。
     */
    private String doCallLLM(String requestBody) throws Exception {
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
        checkHttpStatus(response.statusCode());

        String reply = parseResponse(response.body());
        if (reply == null) {
            log.warn("LLM响应不可用 | status={}", response.statusCode());
        } else if (reply.isBlank()) {
            log.warn("LLM响应成功但正文为空 | status={}", response.statusCode());
        } else {
            log.info("LLM响应成功 | contentLength={}", reply.length());
        }
        return reply;
    }

    /**
     * 构建请求 JSON 体
     */
    private String buildRequestBody(String systemPrompt, String text, List<Message> history) throws Exception {
        return buildRequestBody(systemPrompt, text, history, 0);
    }

    String buildRequestBody(String systemPrompt, String text, List<Message> history,
                            int maxTokens) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.getModel());
        if (maxTokens > 0) {
            root.put("max_tokens", maxTokens);
        }

        ArrayNode messages = root.putArray("messages");

        // system prompt
        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);

        // history messages
        for (Message msg : history) {
            ObjectNode historyMsg = messages.addObject();
            historyMsg.put("role", msg.role().value());
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
            JsonNode choice = choices.get(0);
            JsonNode message = choice.get("message");
            if (message != null) {
                JsonNode content = message.get("content");
                String reply = content != null && !content.isNull()
                        ? content.asText() : null;
                if (reply == null || reply.isBlank()) {
                    String finishReason = choice.path("finish_reason").asText("unknown");
                    int reasoningLength = message.path("reasoning_content")
                            .asText("").length();
                    log.warn("LLM 返回空正文 | finishReason={} | reasoningLength={}",
                            finishReason, reasoningLength);
                }
                return reply;
            }
        }

        log.warn("LLM 响应格式异常: {}", responseBody);
        return null;
    }

    // ==================== Tool Calling 支持 ====================

    /**
     * 获取启动时加载的 system prompt（供 ReActAgentExecutor 构建动态 prompt 使用）
     */
    public String getSystemPrompt() {
        return systemPrompt;
    }

    /**
     * 带工具定义和自定义 system prompt 的 LLM 调用
     * <p>此方法接受显式的 per-request system prompt，不会使用实例字段 {@link #systemPrompt}。
     * 适用于 Skill 场景（不同 Skill 有不同领域上下文）。
     *
     * @param systemPrompt 本次请求的完整 system prompt
     * @param messages     完整消息列表（不含 system prompt）
     * @param tools        工具定义列表
     * @return 结构化响应（可能包含 {@link LLMResponse.ToolCall}），失败返回 null
     */
    public LLMResponse chatWithTools(String systemPrompt, List<Message> messages, List<ToolDefinition> tools) {
        try {
            String requestBody = buildRequestBodyWithTools(systemPrompt, messages, tools);
            log.debug("LLM 请求（含 {} 个工具定义，自定义 system prompt）", tools != null ? tools.size() : 0);

            // 发送 + 解析（含重试）
            return retryExecute(() -> {
                try {
                    return doChatWithTools(requestBody);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, MAX_RETRIES);

        } catch (Exception e) {
            log.error("LLM 调用失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 单次带工具定义的 LLM 调用（不含重试）。非 2xx 抛 {@link LLMHttpException} 触发外层重试。
     */
    private LLMResponse doChatWithTools(String requestBody) throws Exception {
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
        checkHttpStatus(response.statusCode());

        String responseBody = response.body();
        log.debug("LLM 原始响应 | status={} | body={}",
                response.statusCode(), truncate(responseBody, 1000));
        LLMResponse result = parseStructuredResponse(responseBody);
        if (result == null) {
            log.warn("LLM 响应解析失败 | status={} | body={}",
                    response.statusCode(), truncate(responseBody, 500));
        }
        return result;
    }

    /**
     * 带工具定义的 LLM 调用（使用默认 system prompt）
     *
     * @param messages 完整消息列表（已包含 system prompt 之外的所有 user/assistant/tool 消息）
     * @param tools    工具定义列表（为空时等价于普通 chat）
     * @return 结构化响应（可能包含 {@link LLMResponse.ToolCall}），失败返回 null
     */
    public LLMResponse chatWithTools(List<Message> messages, List<ToolDefinition> tools) {
        return chatWithTools(systemPrompt, messages, tools);
    }

    /**
     * 构建含 tools 参数的请求 JSON 体（内部方法）
     */
    private String buildRequestBodyWithTools(List<Message> messages,
                                              List<ToolDefinition> tools) throws Exception {
        return buildRequestBodyWithTools(systemPrompt, messages, tools);
    }

    /**
     * 构建含 tools 参数的请求 JSON 体
     */
    private String buildRequestBodyWithTools(String systemPrompt, List<Message> messages,
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
            case USER -> {
                node.put("role", "user");
                node.put("content", msg.content() != null ? msg.content() : "");
            }
            case ASSISTANT -> {
                node.put("role", "assistant");
                if (msg.reasoningContent() != null && !msg.reasoningContent().isBlank()) {
                    node.put("reasoning_content", msg.reasoningContent());
                }
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
            case TOOL -> {
                node.put("role", "tool");
                node.put("content", msg.content() != null ? msg.content() : "");
                node.put("tool_call_id", msg.toolCallId());
            }
            case SYSTEM -> {
                node.put("role", "system");
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
        String reasoningContent = message.has("reasoning_content")
                && !message.get("reasoning_content").isNull()
                ? message.get("reasoning_content").asText() : null;

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

        // 部分兼容服务不会返回标准 tool_calls，而是把 DSML 工具标记写进 content。
        // 将它恢复成结构化调用，避免内部参数被当作助手正文展示给用户。
        if (toolCalls.isEmpty() && DsmlToolCallParser.containsMarkup(content)) {
            toolCalls = DsmlToolCallParser.parse(content, objectMapper);
            if (!toolCalls.isEmpty()) {
                log.info("已将 content 中的 DSML 转换为 {} 个结构化工具调用", toolCalls.size());
                content = null;
                finishReason = "tool_calls";
            } else {
                // DSML 标记存在但解析失败：清理标记后降级为文本回复
                log.warn("DSML 工具标记解析失败，降级为文本回复");
                if (content != null) {
                    content = content.replaceAll("<" + "｜｜DSML｜｜" + "[^>]*>", "")
                                     .replaceAll("</" + "｜｜DSML｜｜" + "[^>]*>", "")
                                     .trim();
                }
                finishReason = "stop";
            }
        }

        return new LLMResponse(content, toolCalls, finishReason, reasoningContent);
    }

    /**
     * 截断字符串（日志用）
     */
    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    // ==================== 重试支持（P0-3） ====================

    /**
     * 带指数退避的重试执行。
     * 只有 {@link #shouldRetry} 判定为可重试的异常才重试；不可重试（401/400 等）立即抛出。
     */
    private <T> T retryExecute(Supplier<T> supplier, int maxAttempts) {
        int attempt = 0;
        while (true) {
            try {
                return supplier.get();
            } catch (RuntimeException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                attempt++;
                if (attempt >= maxAttempts || !shouldRetry(cause)) {
                    throw e;
                }
                long delay = RETRY_BASE_DELAY_MS * (1L << (attempt - 1));
                log.warn("LLM 调用失败，第 {}/{} 次重试 | delay={}ms | cause={}",
                        attempt, maxAttempts, delay, cause.getMessage());
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试等待被中断", ie);
                }
            }
        }
    }

    /**
     * 判定异常是否值得重试：
     * <ul>
     *   <li>429 / 5xx —— 服务端过载或临时故障，可重试</li>
     *   <li>网络异常（超时/连接重置/IO）—— 可重试</li>
     *   <li>401 / 400 —— API key 错误或参数/schema 错误，重试无意义，不重试</li>
     * </ul>
     */
    private boolean shouldRetry(Throwable e) {
        if (e == null) return false;
        if (e instanceof LLMHttpException http) {
            return http.statusCode() == 429 || http.statusCode() >= 500;
        }
        return e instanceof java.net.http.HttpTimeoutException
                || e instanceof java.io.IOException
                || e instanceof java.net.ConnectException;
    }

    /** HTTP 非 2xx 时抛异常，触发外层重试逻辑 */
    private void checkHttpStatus(int statusCode) throws LLMHttpException {
        if (statusCode >= 400) {
            throw new LLMHttpException(statusCode, "LLM HTTP " + statusCode);
        }
    }

    /** LLM HTTP 错误异常，携带状态码供重试分类 */
    private static final class LLMHttpException extends RuntimeException {
        private final int statusCode;

        LLMHttpException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        int statusCode() {
            return statusCode;
        }
    }
}
