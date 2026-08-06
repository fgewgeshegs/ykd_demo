package com.youkeda.exercise.claw.agent.memory.longterm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * EmbeddingClient 缓存层测试。
 *
 * <p>验证 Caffeine 缓存的命中/未命中/异常 fallback 行为。
 * 使用 mocked HttpClient 控制 API 调用次数，精确验证缓存是否生效。
 */
@DisplayName("EmbeddingClient 缓存测试")
class EmbeddingClientCacheTest {

    private EmbeddingProperties props;
    private HttpClient httpClient;
    private HttpResponse<String> httpResponse;
    private EmbeddingCacheStore l2Cache;
    private EmbeddingClient client;

    @BeforeEach
    void setUp() throws Exception {
        props = new EmbeddingProperties();
        props.setBaseUrl("http://localhost:11434");
        props.setDimension(4);

        httpClient = mock(HttpClient.class);
        httpResponse = mock(HttpResponse.class);
        when(httpResponse.statusCode()).thenReturn(200);

        l2Cache = mock(EmbeddingCacheStore.class);

        // 每次返回递增的向量值，便于区分不同 API 调用
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        client = new EmbeddingClient(
                props, new ObjectMapper(), l2Cache, httpClient, httpClient,
                Clock.fixed(Instant.parse("2026-08-03T00:00:00Z"), ZoneId.of("UTC")));
    }

    // ==================== 基础缓存命中 ====================

    @Nested
    @DisplayName("缓存命中测试")
    class CacheHit {

        @Test
        @DisplayName("相同文本 embed() 只调一次 API")
        void sameTextHitsCache() throws Exception {
            when(httpResponse.body()).thenReturn(
                    "{\"data\":[{\"index\":0,\"embedding\":[1.0,2.0,3.0,4.0]}]}");

            float[] first = client.embed("今天天气怎么样");
            float[] second = client.embed("今天天气怎么样");

            assertArrayEquals(first, second, "缓存命中应返回相同向量");
            // API 只被调了一次（第二次走缓存）
            verify(httpClient, times(1)).send(any(HttpRequest.class),
                    any(HttpResponse.BodyHandler.class));
        }

        @Test
        @DisplayName("不同文本各自调 API")
        void differentTextsMissCache() throws Exception {
            when(httpResponse.body())
                    .thenReturn("{\"data\":[{\"index\":0,\"embedding\":[1.0,0,0,0]}]}")
                    .thenReturn("{\"data\":[{\"index\":0,\"embedding\":[0,1.0,0,0]}]}");

            float[] first = client.embed("你好");
            float[] second = client.embed("再见");

            assertFalse(java.util.Arrays.equals(first, second),
                    "不同文本应返回不同向量");
            verify(httpClient, times(2)).send(any(HttpRequest.class),
                    any(HttpResponse.BodyHandler.class));
        }

        @Test
        @DisplayName("embedBatch() 相同文本批量缓存命中")
        void batchSameTextsPartialHit() throws Exception {
            // 第一批：embed "text-A" 和 "text-B"
            when(httpResponse.body()).thenReturn(
                    "{\"data\":[{\"index\":0,\"embedding\":[1.0,0,0,0]},"
                            + "{\"index\":1,\"embedding\":[0,1.0,0,0]}]}");

            client.embedBatch(List.of("text-A", "text-B"));
            verify(httpClient, times(1)).send(any(HttpRequest.class),
                    any(HttpResponse.BodyHandler.class));

            // 第二批："text-A" 缓存命中，"text-C" 未命中
            when(httpResponse.body()).thenReturn(
                    "{\"data\":[{\"index\":0,\"embedding\":[0,0,1.0,0]}]}");

            client.embedBatch(List.of("text-A", "text-C"));

            // 总共 2 次 API 调用（第一次 2 个文本，第二次仅 1 个未命中）
            verify(httpClient, times(2)).send(any(HttpRequest.class),
                    any(HttpResponse.BodyHandler.class));
        }
    }

    // ==================== 防御性拷贝 ====================

    @Nested
    @DisplayName("防御性拷贝测试")
    class DefensiveCopy {

        @Test
        @DisplayName("修改返回值不影响缓存中的向量")
        void mutationDoesNotAffectCache() throws Exception {
            when(httpResponse.body()).thenReturn(
                    "{\"data\":[{\"index\":0,\"embedding\":[1.0,2.0,3.0,4.0]}]}");

            float[] first = client.embed("测试文本");
            // 尝试修改返回值
            first[0] = 999.0f;

            // 再次获取 — 缓存应返回原始值，不受修改影响
            when(httpResponse.body()).thenReturn(
                    "{\"data\":[{\"index\":0,\"embedding\":[5.0,6.0,7.0,8.0]}]}");

            float[] second = client.embed("测试文本");
            assertEquals(1.0f, second[0], 0.001f,
                    "缓存应返回原始值，不受调用方修改影响");
        }

        @Test
        @DisplayName("批量接口返回值修改不影响缓存")
        void batchMutationDoesNotAffectCache() throws Exception {
            when(httpResponse.body()).thenReturn(
                    "{\"data\":[{\"index\":0,\"embedding\":[1.0,0,0,0]}]}");

            List<float[]> results = client.embedBatch(List.of("key1"));
            results.get(0)[0] = 999.0f;

            float[] cached = client.embed("key1");
            assertEquals(1.0f, cached[0], 0.001f,
                    "批量返回的向量修改不应污染缓存");
        }
    }

    // ==================== 缓存异常 fallback ====================

    @Nested
    @DisplayName("缓存异常自动 fallback")
    class CacheFallback {

        @Test
        @DisplayName("缓存正常工作时不影响结果正确性")
        void cacheDoesNotAffectCorrectness() throws Exception {
            when(httpResponse.body()).thenReturn(
                    "{\"data\":[{\"index\":0,\"embedding\":[1.0,2.0,3.0,4.0]}]}");

            float[] result = client.embed("正常文本");
            assertNotNull(result);
            assertEquals(4, result.length);
            assertEquals(1.0f, result[0], 0.001f);
            assertEquals(2.0f, result[1], 0.001f);
            assertEquals(3.0f, result[2], 0.001f);
            assertEquals(4.0f, result[3], 0.001f);
        }
    }

    // ==================== 批量全命中 ====================

    @Nested
    @DisplayName("批量全命中")
    class BatchFullHit {

        @Test
        @DisplayName("全部缓存命中时不调 API")
        void fullCacheHitSkipsApi() throws Exception {
            // 第一次调 API
            when(httpResponse.body()).thenReturn(
                    "{\"data\":[{\"index\":0,\"embedding\":[1.0,0,0,0]},"
                            + "{\"index\":1,\"embedding\":[0,1.0,0,0]}]}");

            client.embedBatch(List.of("a", "b"));
            verify(httpClient, times(1)).send(any(HttpRequest.class),
                    any(HttpResponse.BodyHandler.class));

            // 第二次全部命中
            List<float[]> results = client.embedBatch(List.of("a", "b"));
            assertEquals(2, results.size());
            // API 调用次数不变
            verify(httpClient, times(1)).send(any(HttpRequest.class),
                    any(HttpResponse.BodyHandler.class));
        }
    }

    // ==================== L2 持久化缓存 ====================

    @Nested
    @DisplayName("L2 持久化缓存测试")
    class L2Cache {

        @Test
        @DisplayName("L1 miss → L2 hit → 不调 API")
        void l2HitSkipsApi() throws Exception {
            // L2 返回缓存向量
            when(l2Cache.get("已缓存的文本")).thenReturn(new float[]{9.0f, 8.0f, 7.0f, 6.0f});

            float[] result = client.embed("已缓存的文本");

            assertArrayEquals(new float[]{9.0f, 8.0f, 7.0f, 6.0f}, result,
                    "L2 命中应返回缓存的向量");
            // API 未被调用
            verify(httpClient, never()).send(any(HttpRequest.class),
                    any(HttpResponse.BodyHandler.class));
        }

        @Test
        @DisplayName("L2 miss → 调 API → 结果写入 L2")
        void l2MissCallsApiAndWritesL2() throws Exception {
            when(l2Cache.get("新文本")).thenReturn(null);
            when(httpResponse.body()).thenReturn(
                    "{\"data\":[{\"index\":0,\"embedding\":[1.0, 2.0, 3.0, 4.0]}]}");

            float[] result = client.embed("新文本");

            assertNotNull(result);
            assertEquals(4, result.length);
            // API 被调了一次
            verify(httpClient, times(1)).send(any(HttpRequest.class),
                    any(HttpResponse.BodyHandler.class));
            // L2 写入了结果
            verify(l2Cache).putBatch(org.mockito.ArgumentMatchers.anyMap());
        }

        @Test
        @DisplayName("batch 混合：部分 L2 命中，部分调 API")
        void batchPartialL2Hit() throws Exception {
            // "text-A" 在 L2 有缓存，"text-B" 没有
            when(l2Cache.get("text-A")).thenReturn(new float[]{1.0f, 0, 0, 0});
            when(l2Cache.get("text-B")).thenReturn(null);

            // API 只返回 "text-B" 的结果（注意：JSON 中不能有 Java 的 f 后缀）
            when(httpResponse.body()).thenReturn(
                    "{\"data\":[{\"index\":0,\"embedding\":[0, 1.0, 0, 0]}]}");

            List<float[]> results = client.embedBatch(List.of("text-A", "text-B"));

            assertEquals(2, results.size());
            // "text-A" 来自 L2 缓存
            assertEquals(1.0f, results.get(0)[0], 0.001f);
            // "text-B" 来自 API
            assertEquals(1.0f, results.get(1)[1], 0.001f);
            // API 只被调了一次（仅 "text-B"）
            verify(httpClient, times(1)).send(any(HttpRequest.class),
                    any(HttpResponse.BodyHandler.class));
        }

        @Test
        @DisplayName("L2 异常时 fallback 到 API")
        void l2ExceptionFallsBackToApi() throws Exception {
            when(l2Cache.get("任意文本")).thenThrow(new RuntimeException("DB down"));
            when(httpResponse.body()).thenReturn(
                    "{\"data\":[{\"index\":0,\"embedding\":[1.0, 2.0, 3.0, 4.0]}]}");

            float[] result = client.embed("任意文本");

            assertNotNull(result);
            assertEquals(1.0f, result[0], 0.001f);
            // API 被正常调用（L2 异常不阻塞）
            verify(httpClient, times(1)).send(any(HttpRequest.class),
                    any(HttpResponse.BodyHandler.class));
        }
    }
}
