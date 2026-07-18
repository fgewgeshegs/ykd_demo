package com.youkeda.exercise.claw.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.stream.Collectors;

/**
 * LLM 客户端
 *
 * 封装大模型 HTTP 调用，兼容 OpenAI 协议格式
 */
@Slf4j
@Component
public class LLMClient {

    private static final int TIMEOUT_SECONDS = 30;
    private static final String SYSTEM_PROMPT_PATH = "prompts/system-prompt.txt";
    private static final String DEFAULT_SYSTEM_PROMPT = "你是 Claw助手，一个智能AI助手。";

    private final LLMProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private String systemPrompt;

    public LLMClient(LLMProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 加载系统提示词
     */
    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource(SYSTEM_PROMPT_PATH);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                this.systemPrompt = reader.lines().collect(Collectors.joining("\n"));
            }
            log.info("系统提示词加载完成，共 {} 字符", systemPrompt.length());
        } catch (Exception e) {
            log.error("加载系统提示词失败，使用默认提示词", e);
            this.systemPrompt = DEFAULT_SYSTEM_PROMPT;
        }
    }

    /**
     * 调用大模型生成回复
     *
     * @param userId 用户标识（用于日志）
     * @param text   用户消息
     * @return 模型回复内容，调用失败时返回 null
     */
    public String chat(String userId, String text) {
        return callLLM(systemPrompt, text);
    }

    /**
     * 使用自定义系统提示词调用大模型
     *
     * @param systemPrompt 自定义系统提示词
     * @param text         用户消息
     * @return 模型回复内容，调用失败时返回 null
     */
    public String chatWithSystemPrompt(String systemPrompt, String text) {
        return callLLM(systemPrompt, text);
    }

    /**
     * 调用大模型（内部方法）
     */
    private String callLLM(String systemPrompt, String text) {
        try {
            // 1. 构建请求体
            String requestBody = buildRequestBody(systemPrompt, text);
            log.info("调用LLM，message={}", text);

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
    private String buildRequestBody(String systemPrompt, String text) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.getModel());

        ArrayNode messages = root.putArray("messages");

        // system prompt
        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);

        // user message
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
}
