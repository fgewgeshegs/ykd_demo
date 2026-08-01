package com.youkeda.exercise.claw.agent.memory.longterm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

/** OpenAI-compatible embedding client used by the long-term memory pipeline. */
@Component
public class EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);
    private static final int TIMEOUT_SECONDS = 120;
    private static final int HEALTH_CHECK_TIMEOUT_SECONDS = 5;

    private final EmbeddingProperties props;
    private final HttpClient httpClient;
    private final HttpClient healthCheckClient;
    private final ObjectMapper objectMapper;

    /** 服务是否可用（启动时检查，不可用则跳过后续所有调用） */
    private volatile boolean available = true;

    public EmbeddingClient(EmbeddingProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
        this.healthCheckClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(HEALTH_CHECK_TIMEOUT_SECONDS))
                .build();
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        checkHealth();
    }

    /**
     * 启动时检查 Embedding 服务是否可用。
     * 不可用时标记为不可用，避免每次用户消息都发连接失败的错误日志。
     */
    private void checkHealth() {
        try {
            // 尝试连接 embedding 服务根路径，看是否能连上
            String url = props.getBaseUrl().replaceAll("/+$", "") + "/v1/embeddings";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(HEALTH_CHECK_TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"model\":\"" + props.getModel() + "\",\"input\":[\"ping\"]}"))
                    .build();
            if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
                // 需要 API Key 的服务跳过预检查
                this.available = true;
                return;
            }

            HttpResponse<String> response = healthCheckClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Embedding 服务连接成功 | url={} | status={}", url, response.statusCode());
                this.available = true;
            } else {
                log.warn("Embedding 服务返回异常状态码 | url={} | status={} | body={}",
                        url, response.statusCode(), truncate(response.body(), 200));
                this.available = false;
            }
        } catch (Exception e) {
            log.warn("Embedding 服务不可用，已禁用向量嵌入。后续将静默跳过 Embedding 调用。"
                    + " 如需启用请确认 Ollama/Xinference 已运行 | url={} | error={}",
                    props.getBaseUrl(), e.getMessage());
            this.available = false;
        }
    }

    public float[] embed(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    /**
     * Embeds all texts in request order. Failures are explicit so zero vectors can
     * never be searched or persisted as valid memory data.
     */
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) return List.of();
        if (texts.stream().anyMatch(text -> text == null || text.isBlank())) {
            throw new IllegalArgumentException("Embedding text must not be blank");
        }
        if (!available) {
            throw new IllegalStateException("Embedding service is not available (startup health check failed)");
        }

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(props.getBaseUrl() + "/v1/embeddings"))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequest(texts)));
            if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + props.getApiKey());
            }

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Embedding API returned status " + response.statusCode());
            }

            List<float[]> vectors = parseResponse(response.body(), texts.size());
            log.debug("Embedding succeeded | count={} | dimension={}",
                    vectors.size(), vectors.get(0).length);
            return vectors;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Embedding call interrupted", e);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.debug("Embedding call skipped: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.warn("Embedding call failed (embedding service may be unavailable)", e);
            throw new IllegalStateException("Embedding call failed", e);
        }
    }

    private String buildRequest(List<String> texts) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", props.getModel());
        ArrayNode input = root.putArray("input");
        texts.forEach(input::add);
        return objectMapper.writeValueAsString(root);
    }

    private List<float[]> parseResponse(String responseBody, int expectedCount) throws Exception {
        JsonNode data = objectMapper.readTree(responseBody).get("data");
        if (data == null || !data.isArray()) {
            throw new IllegalStateException("Embedding response has no data array");
        }

        float[][] ordered = new float[expectedCount][];
        int fallbackIndex = 0;
        for (JsonNode item : data) {
            JsonNode embedding = item.get("embedding");
            if (embedding == null || !embedding.isArray()) {
                throw new IllegalStateException("Embedding response contains an invalid vector");
            }

            int index = item.has("index") ? item.path("index").asInt(-1) : fallbackIndex;
            fallbackIndex++;
            if (index < 0 || index >= expectedCount || ordered[index] != null) {
                throw new IllegalStateException("Invalid embedding response index: " + index);
            }

            float[] vector = new float[embedding.size()];
            double norm = 0d;
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = (float) embedding.get(i).asDouble();
                if (!Float.isFinite(vector[i])) {
                    throw new IllegalStateException("Embedding contains a non-finite value");
                }
                norm += vector[i] * vector[i];
            }
            if (vector.length != props.getDimension()) {
                throw new IllegalStateException("Embedding dimension mismatch: expected="
                        + props.getDimension() + ", actual=" + vector.length);
            }
            if (norm == 0d) {
                throw new IllegalStateException("Embedding API returned a zero vector");
            }
            ordered[index] = vector;
        }

        List<float[]> vectors = new ArrayList<>(expectedCount);
        for (float[] vector : ordered) {
            if (vector == null) {
                throw new IllegalStateException("Embedding response count mismatch");
            }
            vectors.add(vector);
        }
        return vectors;
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
