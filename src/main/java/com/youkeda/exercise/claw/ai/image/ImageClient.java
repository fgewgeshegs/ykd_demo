package com.youkeda.exercise.claw.ai.image;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.ai.config.ImageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 图片生成客户端
 *
 * 负责调用图片生成模型（如 Qwen-Image-2.0）生成图片
 * 请求格式兼容 OpenAI 图片生成 API，同时也支持 DashScope 原生接口
 */
@Slf4j
@Component
public class ImageClient {

    private static final int TIMEOUT_SECONDS = 120;

    private final ImageProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ImageClient(ImageProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 生成图片
     *
     * @param prompt 图片描述提示词
     * @return 生成的图片 URL，失败时返回 null
     */
    public String generateImage(String prompt) {
        try {
            String requestBody = buildRequestBody(prompt);
            log.info("调用图片生成模型 | model={} | prompt={}", properties.getModel(), prompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildUrl()))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            String imageUrl = parseResponse(response.body());
            log.info("图片生成成功 | imageUrl={}", imageUrl);
            return imageUrl;

        } catch (Exception e) {
            log.error("图片生成失败 | error={}", e.getMessage());
            return null;
        }
    }

    /**
     * 构建完整的请求 URL
     *
     * OpenAI 兼容接口需要追加 /images/generations 路径；
     * DashScope 原生接口（wanx-v1）的 baseUrl 已是完整端点，直接使用。
     */
    private String buildUrl() {
        String baseUrl = properties.getBaseUrl();
        // DashScope 原生端点已有具体路径（以 /image-synthesis 结尾），直接使用
        if (baseUrl.endsWith("/image-synthesis")) {
            return baseUrl;
        }
        // OpenAI 兼容接口：追加 /images/generations
        if (!baseUrl.endsWith("/images/generations")) {
            return baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "images/generations";
        }
        return baseUrl;
    }

    /**
     * 构建 OpenAI 兼容的图片生成请求体
     *
     * 格式：
     * {
     *   "model": "Qwen-Image-2.0",
     *   "prompt": "...",
     *   "n": 1,
     *   "size": "1024x1024"
     * }
     */
    private String buildRequestBody(String prompt) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.getModel());
        root.put("prompt", prompt);
        root.put("n", 1);
        root.put("size", "1024x1024");
        return objectMapper.writeValueAsString(root);
    }

    /**
     * 解析 OpenAI 兼容的图片生成响应，提取图片 URL
     *
     * 响应格式：
     * { "data": [{ "url": "..." }] }
     */
    private String parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        // OpenAI 兼容格式：data[].url
        JsonNode data = root.get("data");
        if (data != null && data.isArray() && data.size() > 0) {
            JsonNode url = data.get(0).get("url");
            if (url != null) {
                return url.asText();
            }
        }

        log.warn("图片生成响应格式异常: {}", responseBody);
        return null;
    }
}