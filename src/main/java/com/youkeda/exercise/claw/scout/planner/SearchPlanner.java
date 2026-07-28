package com.youkeda.exercise.claw.scout.planner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.scout.ScoutProperties;
import com.youkeda.exercise.claw.scout.context.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索规划器
 *
 * 根据用户画像，用 LLM 动态生成搜索任务列表
 */
@Service
public class SearchPlanner {

    private static final Logger log = LoggerFactory.getLogger(SearchPlanner.class);

    private static final String SYSTEM_PROMPT = """
            你是信息搜索规划专家。根据用户画像生成搜索任务。

            要求：
            1. 搜索词以英文为主（搜索效果更好），可附中文
            2. 每个任务对应一次搜索
            3. category 必须是以下之一：NEWS、BLOG、GITHUB、JOB、COMPETITION
            4. reason 简要说明为什么搜这个
            5. priority 1-5，5 最高
            6. **必须保证信息时效性**：搜索词中必须包含时间限定词，如 "latest"、"this week"、"recent"、"2026"、"new" 等，确保搜到的是最新信息
            7. 不要搜通用知识，只搜最新动态、更新、发布、趋势

            返回严格的 JSON 数组格式：
            [
              {
                "query": "AI Agent framework latest release 2026",
                "category": "NEWS",
                "reason": "用户关注AI Agent领域最新动态",
                "priority": 5
              }
            ]
            """;

    private final LLMClient llmClient;
    private final ScoutProperties props;
    private final ObjectMapper objectMapper;

    public SearchPlanner(LLMClient llmClient, ScoutProperties props, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    /**
     * 根据用户画像生成搜索任务
     */
    public List<SearchTask> plan(UserProfile profile) {
        String prompt = buildPrompt(profile);

        try {
            String json = llmClient.chatWithSystemPrompt(SYSTEM_PROMPT, prompt);
            if (json == null || json.isBlank()) {
                log.warn("LLM 返回空，使用默认搜索任务");
                return defaultTasks(profile);
            }

            List<SearchTask> tasks = parseTasks(json);
            if (tasks.isEmpty()) {
                return defaultTasks(profile);
            }

            log.info("搜索任务生成成功 | userId={} | count={}", profile.userId(), tasks.size());
            return tasks;
        } catch (Exception e) {
            log.error("搜索任务生成失败，使用默认任务 | userId={}", profile.userId(), e);
            return defaultTasks(profile);
        }
    }

    private String buildPrompt(UserProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为以下用户生成 ").append(props.getSearchTaskCount()).append(" 个搜索任务。\n\n");
        sb.append("用户画像：\n");
        sb.append(profile.toText());

        if (profile.interests().isEmpty() && profile.currentProjects().isEmpty()) {
            sb.append("\n（用户画像为空，请生成通用的 AI/技术/创业热门资讯搜索任务）\n");
        }

        return sb.toString();
    }

    private List<SearchTask> parseTasks(String json) {
        List<SearchTask> tasks = new ArrayList<>();
        try {
            String jsonStr = extractJson(json);
            JsonNode arr = objectMapper.readTree(jsonStr);
            if (!arr.isArray()) return tasks;

            for (JsonNode node : arr) {
                String query = node.path("query").asText("").trim();
                String category = node.path("category").asText("NEWS").trim().toUpperCase();
                String reason = node.path("reason").asText("").trim();
                int priority = node.path("priority").asInt(3);

                if (!query.isEmpty()) {
                    tasks.add(SearchTask.of(query, category, reason,
                            Math.max(1, Math.min(5, priority))));
                }
            }
        } catch (Exception e) {
            log.error("搜索任务 JSON 解析失败 | json={}", json, e);
        }
        return tasks;
    }

    /**
     * 画像为空时的默认搜索任务
     */
    private List<SearchTask> defaultTasks(UserProfile profile) {
        List<SearchTask> tasks = new ArrayList<>();
        tasks.add(SearchTask.of("AI Agent latest news 2026", SearchTask.NEWS,
                "AI Agent 领域最新动态", 5));
        tasks.add(SearchTask.of("Spring Boot latest release", SearchTask.BLOG,
                "Spring 生态更新", 4));
        tasks.add(SearchTask.of("trending AI open source projects", SearchTask.GITHUB,
                "热门 AI 开源项目", 4));
        return tasks;
    }

    private String extractJson(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('[');
            int end = trimmed.lastIndexOf(']');
            if (start >= 0 && end > start) {
                return trimmed.substring(start, end + 1);
            }
        }
        return trimmed;
    }
}
