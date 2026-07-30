package com.youkeda.exercise.claw.scout.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.scout.ScoutProperties;
import com.youkeda.exercise.claw.scout.planner.SearchTask;
import com.youkeda.exercise.claw.scout.processor.InformationItem;
import com.youkeda.exercise.claw.websearch.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 比赛信息采集器
 *
 * 通过 WebSearch 搜索国内外编程比赛、黑客松、AI 竞赛
 */
@Component
public class CompetitionCollector implements Collector {

    private static final Logger log = LoggerFactory.getLogger(CompetitionCollector.class);

    private final SearchService searchService;
    private final ScoutProperties props;
    private final ObjectMapper objectMapper;

    public CompetitionCollector(SearchService searchService, ScoutProperties props, ObjectMapper objectMapper) {
        this.searchService = searchService;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getType() {
        return "COMPETITION";
    }

    @Override
    public List<InformationItem> collect(SearchTask task) {
        List<InformationItem> items = new ArrayList<>();
        try {
            // 构造中英文双语搜索词
            String query = buildQuery(task.query());
            String resultJson = searchService.searchByDate(query, props.getMaxResultsPerTask());
            items = parseResults(task, resultJson);
            log.info("比赛采集完成 | query={} | count={}", task.query(), items.size());
        } catch (Exception e) {
            log.error("比赛采集失败 | query={}", task.query(), e);
        }
        return items;
    }

    private String buildQuery(String query) {
        return "site:kaggle.com OR site:tianchi.aliyun.com OR site:codeforces.com OR site:leetcode.cn " + query + " 比赛 竞赛";
    }

    private List<InformationItem> parseResults(SearchTask task, String resultJson) {
        List<InformationItem> items = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(resultJson);
            JsonNode results = root.get("results");
            if (results == null || !results.isArray()) return items;

            for (JsonNode r : results) {
                String title = safeText(r, "title");
                String content = safeText(r, "content");
                String url = safeText(r, "url");

                if (title.isEmpty() && content.isEmpty()) continue;

                InformationItem item = InformationItem.create(
                        title, content, url, getType(), task.category()
                );
                items.add(item);
            }
        } catch (Exception e) {
            log.error("比赛结果解析失败", e);
        }
        return items;
    }

    private static String safeText(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : "";
    }
}
