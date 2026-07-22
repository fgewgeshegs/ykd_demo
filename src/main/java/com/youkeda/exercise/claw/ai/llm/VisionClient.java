package com.youkeda.exercise.claw.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.common.PromptLoader;
import com.youkeda.exercise.claw.ai.llm.VisionProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 视觉模型客户端
 *
 * 负责调用多模态大模型（如 Qwen3-VL）进行图片理解
 * 请求格式兼容 OpenAI 多模态协议
 */
@Slf4j
@Component
public class VisionClient {

    private static final int TIMEOUT_SECONDS = 60;
    private static final String SYSTEM_PROMPT_PATH = "prompts/vision-system-prompt.txt";
    private static final String DEFAULT_SYSTEM_PROMPT = "你是 Claw助手的视觉理解模块，请客观描述图片中的内容。";

    private final VisionProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final PromptLoader promptLoader;

    private String systemPrompt;

    public VisionClient(VisionProperties properties, ObjectMapper objectMapper, PromptLoader promptLoader) {
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
     * 分析图片并返回描述
     *
     * @param imageUrl 图片 URL
     * @param text     用户对图片的提问（可选，为空则使用默认描述提示）
     * @return 图片分析结果，调用失败时返回 null
     */
    public String analyzeImage(String imageUrl, String text) {
        try {
            String requestBody = buildRequestBody(imageUrl, text);
            log.info("调用视觉模型 model={}, imageUrlLen={}", properties.getModel(), imageUrl != null ? imageUrl.length() : 0);

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

            String reply = parseResponse(response.body());
            if (reply != null && !reply.isEmpty()) {
                log.info("图片分析成功");
            } else {
                log.warn("视觉模型返回内容为空");
            }
            return reply;

        } catch (Exception e) {
            log.error("视觉模型调用失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 构建多模态请求 JSON 体
     *
     * 格式：
     * {
     *   "model": "...",
     *   "messages": [
     *     { "role": "system", "content": "..." },
     *     {
     *       "role": "user",
     *       "content": [
     *         { "type": "image_url", "image_url": { "url": "..." } },
     *         { "type": "text", "text": "..." }
     *       ]
     *     }
     *   ]
     * }
     */
    private String buildRequestBody(String imageUrl, String text) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.getModel());

        ArrayNode messages = root.putArray("messages");

        // system prompt
        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);

        // user message with multimodal content array
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");

        ArrayNode contentArray = userMsg.putArray("content");

        // image_url part
        ObjectNode imagePart = contentArray.addObject();
        imagePart.put("type", "image_url");
        ObjectNode imageUrlNode = imagePart.putObject("image_url");
        imageUrlNode.put("url", imageUrl);

        // text part
        String userText = (text != null && !text.isEmpty()) ? text : "请描述这张图片";
        ObjectNode textPart = contentArray.addObject();
        textPart.put("type", "text");
        textPart.put("text", userText);

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
                    String text = content.asText();
                    return text.isEmpty() ? null : text;
                }
            }
        }

        log.warn("视觉模型响应格式异常: {}", responseBody);
        return null;
    }
}
