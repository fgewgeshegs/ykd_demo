package com.youkeda.exercise.claw.scout.collector.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.scout.ScoutProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Kaggle API 客户端
 *
 * 文档：https://github.com/Kaggle/kaggle-api
 * 接口：GET https://www.kaggle.com/api/v1/competitions/list
 * 认证：Basic Auth（username:api_key）
 */
@Component
public class KaggleApiClient {

    private static final Logger log = LoggerFactory.getLogger(KaggleApiClient.class);
    private static final String BASE_URL = "https://www.kaggle.com/api/v1";
    private static final int TIMEOUT_SECONDS = 15;

    private final ScoutProperties props;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public KaggleApiClient(ScoutProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    /**
     * 获取竞赛列表
     *
     * @param search  搜索关键词（可选）
     * @param sortBy  排序方式：prize/earliestDeadline/recentlyCreated/numberOfTeams
     * @param limit   返回数量
     * @return 竞赛列表
     */
    public List<KaggleCompetition> listCompetitions(String search, String sortBy, int limit) {
        String username = props.getKaggle().getUsername();
        String apiKey = props.getKaggle().getApiKey();

        if (username == null || username.isBlank() || apiKey == null || apiKey.isBlank()) {
            log.warn("Kaggle API 未配置用户名或密钥");
            return List.of();
        }

        try {
            StringBuilder url = new StringBuilder(BASE_URL + "/competitions/list?");
            if (search != null && !search.isBlank()) {
                url.append("search=").append(search).append("&");
            }
            if (sortBy != null && !sortBy.isBlank()) {
                url.append("sortBy=").append(sortBy).append("&");
            }
            url.append("page=1");

            String auth = Base64.getEncoder().encodeToString(
                    (username + ":" + apiKey).getBytes());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("Authorization", "Basic " + auth)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Kaggle API 请求失败 | status={} | body={}", response.statusCode(), response.body());
                return List.of();
            }

            return parseCompetitions(response.body(), limit);
        } catch (Exception e) {
            log.error("Kaggle API 调用失败", e);
            return List.of();
        }
    }

    /**
     * 解析竞赛列表
     */
    private List<KaggleCompetition> parseCompetitions(String json, int limit) {
        List<KaggleCompetition> result = new ArrayList<>();
        try {
            JsonNode arr = objectMapper.readTree(json);
            if (!arr.isArray()) return result;

            for (JsonNode node : arr) {
                if (result.size() >= limit) break;

                KaggleCompetition comp = new KaggleCompetition();
                comp.ref = safeText(node, "ref");
                comp.title = safeText(node, "title");
                comp.url = "https://www.kaggle.com/competitions/" + comp.ref;
                comp.description = safeText(node, "description");
                comp.category = safeText(node, "category");
                comp.reward = safeText(node, "reward");
                comp.teamCount = node.has("teamCount") ? node.get("teamCount").asInt() : 0;
                comp.deadline = safeText(node, "deadline");
                comp.kernelCount = node.has("kernelCount") ? node.get("kernelCount").asInt() : 0;

                // 只保留进行中的竞赛
                if ("open".equalsIgnoreCase(comp.category) || comp.category.isEmpty()) {
                    result.add(comp);
                }
            }

            log.info("Kaggle 竞赛解析完成 | count={}", result.size());
        } catch (Exception e) {
            log.error("Kaggle 竞赛解析失败", e);
        }
        return result;
    }

    private static String safeText(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : "";
    }

    /**
     * Kaggle 竞赛数据结构
     */
    public static class KaggleCompetition {
        public String ref;
        public String title;
        public String url;
        public String description;
        public String category;
        public String reward;
        public int teamCount;
        public String deadline;
        public int kernelCount;
    }
}
