package com.youkeda.exercise.claw.agent.memory.longterm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * EmbeddingClient 健康检查（/api/tags 模型识别）测试。
 *
 * <p>修复背景：Ollama 对不带 tag 的 pull 会把模型存成 {@code bge-m3:latest}，而旧实现用
 * {@code contains("\"bge-m3\"")} 精确匹配裸名，永远匹配不上 → 每次启动都误报
 * 「模型未拉取」并打开熔断器。本测试用真实 tags 响应（带 :latest 后缀）验证健康检查
 * 能正确识别已拉取模型。
 */
class EmbeddingClientHealthCheckTest {

    @Test
    void healthCheckRecognizesModelWithTagSuffix() throws Exception {
        EmbeddingProperties props = new EmbeddingProperties();
        props.setBaseUrl("http://localhost:11434");
        props.setDimension(2);
        props.getCircuit().setOpenDuration(Duration.ofSeconds(10));

        HttpClient http = mock(HttpClient.class);
        HttpClient health = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> tagsResponse = mock(HttpResponse.class);
        when(tagsResponse.statusCode()).thenReturn(200);
        // Ollama /api/tags 真实响应：模型名带 :latest 后缀（用户 pull bge-m3 后即为此形态）
        when(tagsResponse.body()).thenReturn(
                "{\"models\":[{\"name\":\"bge-m3:latest\",\"model\":\"bge-m3:latest\"}]}");
        @SuppressWarnings("unchecked")
        HttpResponse<String> pingResponse = mock(HttpResponse.class);
        when(pingResponse.statusCode()).thenReturn(200);
        when(pingResponse.body()).thenReturn(
                "{\"data\":[{\"index\":0,\"embedding\":[1.0,2.0]}]}");
        when(health.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(tagsResponse)
                .thenReturn(pingResponse);

        EmbeddingCacheStore l2Cache = mock(EmbeddingCacheStore.class);
        EmbeddingClient client = new EmbeddingClient(
                props, new ObjectMapper(), l2Cache, http, health,
                Clock.fixed(Instant.parse("2026-08-03T00:00:00Z"), ZoneId.of("UTC")));

        client.init();

        assertEquals(EmbeddingClient.CircuitState.CLOSED, client.circuitState(),
                "健康检查应识别带 :latest 后缀的已拉取模型，不应误判为未拉取并打开熔断器");
    }

    @Test
    void healthCheckRejectsModelNotPulled() throws Exception {
        EmbeddingProperties props = new EmbeddingProperties();
        props.setBaseUrl("http://localhost:11434");
        props.setDimension(2);
        props.getCircuit().setOpenDuration(Duration.ofSeconds(10));

        HttpClient http = mock(HttpClient.class);
        HttpClient health = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> tagsResponse = mock(HttpResponse.class);
        when(tagsResponse.statusCode()).thenReturn(200);
        // 模型确实不存在（tags 里只有另一个模型）
        when(tagsResponse.body()).thenReturn(
                "{\"models\":[{\"name\":\"nomic-embed-text:latest\",\"model\":\"nomic-embed-text:latest\"}]}");
        when(health.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(tagsResponse);

        EmbeddingCacheStore l2Cache = mock(EmbeddingCacheStore.class);
        EmbeddingClient client = new EmbeddingClient(
                props, new ObjectMapper(), l2Cache, http, health,
                Clock.fixed(Instant.parse("2026-08-03T00:00:00Z"), ZoneId.of("UTC")));

        client.init();

        assertEquals(EmbeddingClient.CircuitState.OPEN, client.circuitState(),
                "模型未拉取时健康检查应打开熔断器");
    }
}
