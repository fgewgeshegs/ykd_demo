package com.youkeda.exercise.claw.agent.memory.longterm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** OpenAI-compatible embedding client with timeout and recoverable circuit breaker. */
@Component
public class EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);
    /** Primary client connection timeout. Local Ollama is on localhost — 5s is generous. */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    enum CircuitState { CLOSED, OPEN, HALF_OPEN }

    private final EmbeddingProperties props;
    private final HttpClient httpClient;
    private final HttpClient healthCheckClient;
    private final ObjectMapper objectMapper;
    private final EmbeddingCacheStore l2Cache;
    private final Clock clock;

    /** Embedding 向量缓存：key=原始文本，value=向量。同一请求链路中 SkillKnowledge + LongTermMemory
     *  对同一 userMessage 各调用一次 embed()，缓存消除重复 API 调用。 */
    private final Cache<String, float[]> embeddingCache = Caffeine.newBuilder()
            .maximumSize(200)
            .expireAfterWrite(Duration.ofMinutes(10))
            .recordStats()
            .build();

    private CircuitState circuitState = CircuitState.CLOSED;
    private int consecutiveFailures;
    private Instant openUntil = Instant.EPOCH;
    private boolean halfOpenProbeInFlight;

    @org.springframework.beans.factory.annotation.Autowired
    public EmbeddingClient(EmbeddingProperties props,
                           ObjectMapper objectMapper,
                           EmbeddingCacheStore l2Cache) {
        this(props, objectMapper, l2Cache,
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .connectTimeout(CONNECT_TIMEOUT)
                        .build(),
                HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_2)
                        .connectTimeout(CONNECT_TIMEOUT)
                        .build(),
                Clock.systemUTC());
    }

    EmbeddingClient(EmbeddingProperties props,
                    ObjectMapper objectMapper,
                    EmbeddingCacheStore l2Cache,
                    HttpClient httpClient,
                    HttpClient healthCheckClient,
                    Clock clock) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.l2Cache = l2Cache;
        this.httpClient = httpClient;
        this.healthCheckClient = healthCheckClient;
        this.clock = clock;
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        checkHealth();
    }

    private void checkHealth() {
        // 非本地服务（有 apiKey）则跳过本地诊断
        if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
            log.info("Embedding 使用远程服务 | baseUrl={} | model={}", props.getBaseUrl(), props.getModel());
            return;
        }

        String baseUrl = props.getBaseUrl();

        // 1. 检查 Ollama 服务可达性
        try {
            HttpRequest tagsReq = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl.replaceAll("/+$", "") + "/api/tags"))
                    .timeout(CONNECT_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> tagsResp = healthCheckClient.send(
                    tagsReq, HttpResponse.BodyHandlers.ofString());
            if (tagsResp.statusCode() != 200) {
                log.warn("⚠️  Ollama 服务不可达 | url={} | status={} | 请确认 Ollama 已启动：ollama serve",
                        baseUrl, tagsResp.statusCode());
                openCircuit();
                return;
            }

            // 2. 检查模型是否已拉取（Ollama 返回的模型名带 tag 如 "bge-m3:latest"，匹配前缀即可）
            String model = props.getModel();
            boolean modelPulled = tagsResp.body() != null
                    && (tagsResp.body().contains("\"" + model + "\"")
                        || tagsResp.body().contains("\"" + model + ":"));
            if (!modelPulled) {
                log.warn("⚠️  Embedding 模型未拉取 | model={} | 请运行：ollama pull {}",
                        model, model);
                openCircuit();
                return;
            }

            // 3. 模型已就绪，用实际 embedding 请求做最终验证
            HttpRequest pingReq = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint()))
                    .timeout(props.getRequestTimeout())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"model\":\"" + model + "\",\"input\":[\"ping\"]}"))
                    .build();
            HttpResponse<String> pingResp = healthCheckClient.send(
                    pingReq, HttpResponse.BodyHandlers.ofString());
            if (pingResp.statusCode() >= 200 && pingResp.statusCode() < 300) {
                onSuccess();
                int l2Count = l2Cache != null ? l2Cache.count() : 0;
                log.info("✅ Embedding 服务就绪 | url={} | model={} | L2缓存条目={}",
                        baseUrl, model, l2Count);
            } else {
                openCircuit();
                log.warn("⚠️  Embedding API 异常 | status={} | 熔断器将在冷却后自动探测",
                        pingResp.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            openCircuit();
            log.warn("Embedding 服务预检查被中断，已暂时打开熔断器");
        } catch (Exception e) {
            openCircuit();
            log.warn("⚠️  Embedding 服务预检查失败 | error={} | 请确认 Ollama 已启动：ollama serve",
                    e.getMessage());
        }
    }

    /**
     * 获取单条文本的 Embedding 向量（带缓存）。
     *
     * <p>缓存 key = 原始文本，value = float[] 向量（防御性拷贝存储），
     * maxSize=200，TTL=10 分钟。缓存异常自动 fallback 到 API 调用。
     */
    public float[] embed(String text) {
        // 缓存读取（异常安全：任何缓存异常都 fallback 到 API）
        float[] cached = null;
        try {
            cached = embeddingCache.getIfPresent(text);
        } catch (Exception e) {
            log.warn("Embedding cache read failed, falling back to API | textLen={} | error={}",
                    text != null ? text.length() : 0, e.getMessage());
        }
        if (cached != null) {
            log.debug("Embedding cache hit | textLen={}", text.length());
            return copyOf(cached);
        }

        // 缓存未命中：调 API，结果由 embedBatch 回填缓存
        return embedBatch(List.of(text)).get(0);
    }

    /**
     * 批量获取 Embedding 向量（带缓存）。
     *
     * <p>分批策略：先查缓存命中部分直接返回（防御性拷贝），仅未命中部分调 API，
     * 结果回填缓存（存储防御性拷贝防止调用方篡改）。
     */
    public List<float[]> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) return List.of();
        if (texts.stream().anyMatch(text -> text == null || text.isBlank())) {
            throw new IllegalArgumentException("Embedding text must not be blank");
        }

        // ==== Phase 1: 查缓存，分离命中与未命中 ====
        int cacheHits = 0;
        float[][] results = new float[texts.size()][];
        List<Integer> missIndices = new ArrayList<>();
        List<String> missTexts = new ArrayList<>();

        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            float[] cached = null;
            try {
                cached = embeddingCache.getIfPresent(text);
            } catch (Exception e) {
                log.warn("Embedding cache read failed for batch item, falling back | textLen={} | error={}",
                        text.length(), e.getMessage());
            }
            if (cached != null) {
                results[i] = copyOf(cached);
                cacheHits++;
            } else {
                missIndices.add(i);
                missTexts.add(text);
            }
        }

        // 全部命中 → 直接返回，无需 API 调用
        if (missTexts.isEmpty()) {
            log.debug("Embedding cache L1 full hit | count={}", texts.size());
            return Arrays.asList(results);
        }

        // ==== Phase 1.5: L1 未命中的文本查 L2 SQLite 持久化缓存 ====
        int l2Hits = 0;
        List<Integer> stillMissIndices = new ArrayList<>();
        List<String> stillMissTexts = new ArrayList<>();

        for (int k = 0; k < missTexts.size(); k++) {
            String text = missTexts.get(k);
            int originalIndex = missIndices.get(k);
            float[] l2Vector = null;
            try {
                l2Vector = l2Cache.get(text);
            } catch (Exception e) {
                log.debug("Embedding L2 cache read failed, falling back | textLen={} | error={}",
                        text.length(), e.getMessage());
            }
            if (l2Vector != null) {
                results[originalIndex] = l2Vector;
                // L2 命中 → 回填 L1（下次走快速路径）
                try {
                    embeddingCache.put(text, copyOf(l2Vector));
                } catch (Exception e) {
                    log.debug("Embedding L1 backfill failed | textLen={}", text.length());
                }
                l2Hits++;
            } else {
                stillMissIndices.add(originalIndex);
                stillMissTexts.add(text);
            }
        }

        // L1 + L2 全部命中 → 无需 API 调用
        if (stillMissTexts.isEmpty()) {
            log.debug("Embedding cache L1+L2 full hit | total={} | l1Hit={} | l2Hit={}",
                    texts.size(), cacheHits, l2Hits);
            return Arrays.asList(results);
        }

        // 更新为仍需 API 的文本列表
        missIndices = stillMissIndices;
        missTexts = stillMissTexts;

        // ==== Phase 2: 仅对 L1+L2 均未命中的文本调 API ====
        beforeRequest();

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint()))
                    .timeout(props.getRequestTimeout())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildRequest(missTexts)));
            if (props.getApiKey() != null && !props.getApiKey().isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + props.getApiKey());
            }

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Embedding API returned status " + response.statusCode());
            }

            List<float[]> missVectors = parseResponse(response.body(), missTexts.size());
            onSuccess();

            // ==== Phase 3: 结果回填 L1 + L2 缓存，组装返回值 ====
            Map<String, float[]> l2Batch = new HashMap<>();
            for (int j = 0; j < missTexts.size(); j++) {
                float[] vector = missVectors.get(j);
                int originalIndex = missIndices.get(j);
                results[originalIndex] = vector;

                // L1 缓存（防御性拷贝）
                try {
                    embeddingCache.put(missTexts.get(j), copyOf(vector));
                } catch (Exception e) {
                    log.warn("Embedding L1 cache write failed | textLen={} | error={}",
                            missTexts.get(j).length(), e.getMessage());
                }
                // 收集待批量写入 L2
                l2Batch.put(missTexts.get(j), vector);
            }
            // L2 持久化缓存批量写入
            try {
                l2Cache.putBatch(l2Batch);
            } catch (Exception e) {
                log.warn("Embedding L2 batch write failed | error={}", e.getMessage());
            }

            log.debug("Embedding batch | total={} | l1Hit={} | l2Hit={} | apiCall={} | cacheStats={}",
                    texts.size(), cacheHits, l2Hits, missTexts.size(), statsSummary());
            return Arrays.asList(results);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            onFailure();
            throw new IllegalStateException("Embedding call interrupted", e);
        } catch (IllegalArgumentException | IllegalStateException e) {
            onFailure();
            log.debug("Embedding call failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            onFailure();
            log.warn("Embedding call failed", e);
            throw new IllegalStateException("Embedding call failed", e);
        }
    }

    // ==================== 缓存工具方法 ====================

    /** 防御性拷贝：防止调用方修改返回值后污染缓存中的向量。 */
    private static float[] copyOf(float[] vector) {
        return vector.clone();
    }

    /** 缓存统计摘要（日志用）。 */
    private String statsSummary() {
        try {
            CacheStats s = embeddingCache.stats();
            return String.format("hits=%d misses=%d hitRate=%.2f evictions=%d",
                    s.hitCount(), s.missCount(), s.hitRate(), s.evictionCount());
        } catch (Exception e) {
            return "stats unavailable";
        }
    }

    synchronized CircuitState circuitState() {
        refreshOpenState();
        return circuitState;
    }

    public synchronized String circuitStateName() {
        refreshOpenState();
        return circuitState.name();
    }

    private synchronized void beforeRequest() {
        refreshOpenState();
        if (circuitState == CircuitState.OPEN) {
            throw new IllegalStateException("Embedding circuit is open until " + openUntil);
        }
        if (circuitState == CircuitState.HALF_OPEN) {
            if (halfOpenProbeInFlight) {
                throw new IllegalStateException("Embedding circuit half-open probe is in progress");
            }
            halfOpenProbeInFlight = true;
        }
    }

    private synchronized void refreshOpenState() {
        if (circuitState == CircuitState.OPEN && !clock.instant().isBefore(openUntil)) {
            circuitState = CircuitState.HALF_OPEN;
            halfOpenProbeInFlight = false;
            log.info("Embedding circuit transitioned to HALF_OPEN");
        }
    }

    private synchronized void onSuccess() {
        circuitState = CircuitState.CLOSED;
        consecutiveFailures = 0;
        halfOpenProbeInFlight = false;
    }

    private synchronized void onFailure() {
        halfOpenProbeInFlight = false;
        if (circuitState == CircuitState.HALF_OPEN) {
            openCircuit();
            return;
        }
        consecutiveFailures++;
        if (consecutiveFailures >= Math.max(1, props.getCircuit().getFailureThreshold())) {
            openCircuit();
        }
    }

    private synchronized void openCircuit() {
        circuitState = CircuitState.OPEN;
        halfOpenProbeInFlight = false;
        openUntil = clock.instant().plus(props.getCircuit().getOpenDuration());
        log.warn("Embedding circuit transitioned to OPEN | retryAfter={}", openUntil);
    }

    private String endpoint() {
        return props.getBaseUrl().replaceAll("/+$", "") + "/v1/embeddings";
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
}
