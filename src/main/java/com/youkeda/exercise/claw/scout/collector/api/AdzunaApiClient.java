package com.youkeda.exercise.claw.scout.collector.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.scout.ScoutProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Adzuna 招聘 API 客户端
 *
 * 文档：https://developer.adzuna.com/overview
 * 接口：GET https://api.adzuna.com/v1/api/jobs/{country}/search/{page}
 * 认证：URL 参数 app_id + app_key
 * 免费额度：250 请求/月
 */
@Component
public class AdzunaApiClient {

    private static final Logger log = LoggerFactory.getLogger(AdzunaApiClient.class);
    private static final String BASE_URL = "https://api.adzuna.com/v1/api/jobs";
    private static final int TIMEOUT_SECONDS = 15;

    private final ScoutProperties props;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AdzunaApiClient(ScoutProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    /**
     * 搜索招聘岗位
     *
     * @param query   搜索关键词
     * @param country 国家代码：cn/us/gb
     * @param limit   返回数量
     * @return 岗位列表
     */
    public List<JobListing> searchJobs(String query, String country, int limit) {
        String appId = props.getAdzuna().getAppId();
        String appKey = props.getAdzuna().getAppKey();

        if (appId == null || appId.isBlank() || appKey == null || appKey.isBlank()) {
            log.warn("Adzuna API 未配置 appId 或 appKey");
            return List.of();
        }

        if (country == null || country.isBlank()) {
            country = props.getAdzuna().getCountry();
        }

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = BASE_URL + "/" + country + "/search/1"
                    + "?app_id=" + appId
                    + "&app_key=" + appKey
                    + "&what=" + encodedQuery
                    + "&results_per_page=" + Math.min(limit, 50)
                    + "&sort_by=date"
                    + "&max_days_old=14";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Adzuna API 请求失败 | status={}", response.statusCode());
                return List.of();
            }

            return parseJobs(response.body(), limit);
        } catch (Exception e) {
            log.error("Adzuna API 调用失败", e);
            return List.of();
        }
    }

    /**
     * 解析岗位列表
     */
    private List<JobListing> parseJobs(String json, int limit) {
        List<JobListing> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode results = root.get("results");
            if (results == null || !results.isArray()) return result;

            for (JsonNode node : results) {
                if (result.size() >= limit) break;

                JobListing job = new JobListing();
                job.title = safeText(node, "title");
                job.description = safeText(node, "description");
                job.url = safeText(node, "redirect_url");

                // 公司信息
                JsonNode company = node.get("company");
                job.company = company != null ? safeText(company, "display_name") : "";

                // 地点信息
                JsonNode location = node.get("location");
                if (location != null) {
                    JsonNode area = location.get("area");
                    if (area != null && area.isArray() && area.size() > 0) {
                        job.location = area.get(area.size() - 1).asText("");
                    }
                }

                // 薪资信息
                if (node.has("salary_min") && !node.get("salary_min").isNull()) {
                    job.salaryMin = node.get("salary_min").asDouble();
                }
                if (node.has("salary_max") && !node.get("salary_max").isNull()) {
                    job.salaryMax = node.get("salary_max").asDouble();
                }

                // 发布时间
                job.created = safeText(node, "created");

                // 分类
                JsonNode category = node.get("category");
                job.category = category != null ? safeText(category, "label") : "";

                result.add(job);
            }

            log.info("Adzuna 岗位解析完成 | count={}", result.size());
        } catch (Exception e) {
            log.error("Adzuna 岗位解析失败", e);
        }
        return result;
    }

    private static String safeText(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : "";
    }

    /**
     * 招聘岗位数据结构
     */
    public static class JobListing {
        public String title = "";
        public String description = "";
        public String company = "";
        public String location = "";
        public String url = "";
        public String category = "";
        public String created = "";
        public double salaryMin;
        public double salaryMax;
    }
}
