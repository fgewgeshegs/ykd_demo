package org.example.wechatilink.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.wechatilink.config.ILinkConfigProperties;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * AI 对话服务，调用 OpenAI 兼容 API（SiliconFlow 等）。
 */
@Slf4j
@Service
public class AiChatService {

    private final ILinkConfigProperties config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AiChatService(ILinkConfigProperties config) {
        this.config = config;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 调用 AI 生成回复。失败时返回 null，由调用方降级到固定模板。
     */
    public String chat(String userMessage) {
        if (!config.isAiEnabled() || config.getAiApiKey().isBlank()) {
            return null;
        }

        try {
            var requestBody = Map.of(
                    "model", config.getAiModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", config.getAiSystemPrompt()),
                            Map.of("role", "user", "content", userMessage)
                    ),
                    "max_tokens", config.getAiMaxTokens(),
                    "stream", false
            );

            String json = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getAiBaseUrl() + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getAiApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                var node = objectMapper.readTree(response.body());
                String content = node.path("choices").get(0)
                        .path("message").path("content").asText();
                log.info("🤖 AI 回复成功: {}", content.length() > 50
                        ? content.substring(0, 50) + "..." : content);
                return content;
            } else {
                log.error("AI API 错误 {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("AI 调用异常", e);
        }

        return null; // 降级
    }
}
