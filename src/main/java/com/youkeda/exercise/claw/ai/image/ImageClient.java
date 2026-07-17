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
 * 负责调用图片生成模型（如阿里云 wanx-v1）生成图片
 * 请求格式兼容阿里云 DashScope 文本生成图像 API
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
                    .uri(URI.create(properties.getBaseUrl()))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .header("X-DashScope-Async", "enable")
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
     * 构建 DashScope 文本生成图像请求体
     *
     * 格式：
     * {
     *   "model": "wanx-v1",
     *   "input": { "prompt": "..." },
     *   "parameters": { "size": "1024*1024", "n": 1 }
     * }
     */
    private String buildRequestBody(String prompt) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.getModel());

        ObjectNode input = root.putObject("input");
        input.put("prompt", prompt);

        ObjectNode parameters = root.putObject("parameters");
        parameters.put("size", "1024*1024");
        parameters.put("n", 1);

        return objectMapper.writeValueAsString(root);
    }

    /**
     * 解析图片生成响应 JSON，提取图片 URL
     *
     * DashScope 同步响应格式：
     * { "output": { "results": [{ "url": "..." }] } }
     *
     * DashScope 异步响应（X-DashScope-Async: enable）：
     * { "output": { "task_status": "PENDING", "task_id": "..." } }
     */
    private String parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        // 同步响应：直接取 results
        JsonNode output = root.get("output");
        if (output != null) {
            JsonNode results = output.get("results");
            if (results != null && results.isArray() && results.size() > 0) {
                JsonNode url = results.get(0).get("url");
                if (url != null) {
                    return url.asText();
                }
            }
        }

        // 异步响应：返回 task_id 供后续查询
        JsonNode taskIdNode = root.path("output").path("task_id");
        if (!taskIdNode.isMissingNode()) {
            String taskId = taskIdNode.asText();
            log.info("图片生成任务已提交 | taskId={}", taskId);
            return "[异步任务] taskId=" + taskId;
        }

        log.warn("图片生成响应格式异常: {}", responseBody);
        return null;
    }
}