package com.youkeda.exercise.claw.map.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pexels 图片搜索 Provider
 *
 * <p>通过 Pexels API 获取真实地点照片并下载为字节数组。
 * Pexels 提供免费额度（200 req/hour），图片为高质量摄影作品。
 *
 * <p>搜索策略：
 * <ol>
 *   <li>构造搜索词：{keyword} {city}</li>
 *   <li>调用 Pexels Search API 获取照片列表</li>
 *   <li>逐一下载每张照片的原始字节</li>
 *   <li>返回可直接发送的图片字节列表</li>
 * </ol>
 *
 * <p>通过 {@code place-image.provider=pexels} 启用。
 */
@Component
@ConditionalOnProperty(name = "place-image.provider", havingValue = "pexels", matchIfMissing = false)
public class PexelsImageProvider implements PlaceImageProvider {

    private static final Logger log = LoggerFactory.getLogger(PexelsImageProvider.class);

    private static final int MAX_RESULTS = 5;
    private static final int MAX_DOWNLOAD = 3;

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    private final PexelsProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PexelsImageProvider(PexelsProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public List<String> searchImages(String keyword, String city) {
        // 保留 URL 返回的兼容实现：先调用 API 获取 URL 列表
        if (!properties.isConfigured()) {
            log.warn("Pexels API Key 未配置，无法搜索图片");
            return List.of();
        }
        try {
            return searchPhotoUrls(keyword, city);
        } catch (Exception e) {
            log.error("Pexels URL 搜索失败 | keyword={} | city={} | error={}",
                    keyword, city, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<byte[]> searchImageBytes(String keyword, String city) {
        log.info("PexelsImageProvider | keyword={} | city={}", keyword, city);

        if (!properties.isConfigured()) {
            log.warn("Pexels API Key 未配置，无法搜索图片");
            return List.of();
        }

        try {
            // Step 1: 搜索照片 URL
            List<String> photoUrls = searchPhotoUrls(keyword, city);
            if (photoUrls.isEmpty()) {
                log.info("Pexels 未搜索到图片 | keyword={} | city={}", keyword, city);
                return List.of();
            }

            // Step 2: 下载每张照片
            List<byte[]> results = new ArrayList<>();
            for (String url : photoUrls) {
                if (results.size() >= MAX_DOWNLOAD) break;
                try {
                    byte[] bytes = downloadPhoto(url);
                    if (bytes != null && bytes.length > 0) {
                        results.add(bytes);
                    }
                } catch (Exception e) {
                    log.debug("Pexels 图片下载失败，跳过 | url={}", url);
                }
            }

            log.info("PexelsImageProvider 完成 | keyword={} | city={} | downloaded={}",
                    keyword, city, results.size());
            return results;

        } catch (Exception e) {
            log.error("PexelsImageProvider 搜索失败 | keyword={} | city={} | error={}",
                    keyword, city, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 调用 Pexels Search API 获取照片 URL 列表
     */
    private List<String> searchPhotoUrls(String keyword, String city) throws Exception {
        String query = buildQuery(keyword, city);
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = properties.getBaseUrl() + "/search?query=" + encodedQuery
                + "&per_page=" + MAX_RESULTS + "&orientation=landscape&size=medium";

        log.debug("Pexels 搜索请求 | query={} | url={}", query, url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", properties.getApiKey())
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Pexels API HTTP {} | url={} | body={}",
                    response.statusCode(), url,
                    response.body().substring(0, Math.min(200, response.body().length())));
            return List.of();
        }

        List<String> photoUrls = new ArrayList<>();
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode photos = root.path("photos");
        if (photos.isArray()) {
            for (JsonNode photo : photos) {
                // 优先取 large，其次 original，最后 medium
                String photoUrl = photo.path("src").path("large").asText("");
                if (photoUrl.isBlank()) {
                    photoUrl = photo.path("src").path("original").asText("");
                }
                if (photoUrl.isBlank()) {
                    photoUrl = photo.path("src").path("medium").asText("");
                }
                if (!photoUrl.isBlank()) {
                    photoUrls.add(photoUrl);
                }
            }
        }

        log.debug("Pexels 搜索完成 | query={} | urls={}", query, photoUrls.size());
        return photoUrls;
    }

    /**
     * 下载单张 Pexels 照片
     */
    private byte[] downloadPhoto(String photoUrl) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(photoUrl))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        }
        return null;
    }

    /**
     * 构造搜索词：{keyword} {city}
     */
    private static String buildQuery(String keyword, String city) {
        StringBuilder sb = new StringBuilder();
        if (keyword != null && !keyword.isBlank()) {
            sb.append(keyword);
        }
        if (city != null && !city.isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(city);
        }
        return sb.toString().trim();
    }
}
