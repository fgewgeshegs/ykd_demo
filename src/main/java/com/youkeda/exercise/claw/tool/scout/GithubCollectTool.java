package com.youkeda.exercise.claw.tool.scout;
import com.youkeda.exercise.claw.feature.scout.collector.GithubCollector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.runtime.AbstractTool;
import com.youkeda.exercise.claw.agent.runtime.ToolExecutionContext;
import com.youkeda.exercise.claw.agent.runtime.ToolRegistry;
import com.youkeda.exercise.claw.feature.scout.planner.SearchTask;
import com.youkeda.exercise.claw.feature.scout.processor.InformationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * GitHub 采集工具（独立调用）
 *
 * 搜索 GitHub 近期热门项目
 */
@Component
public class GithubCollectTool extends AbstractTool {

    private static final Logger log = LoggerFactory.getLogger(GithubCollectTool.class);

    private final GithubCollector collector;

    public GithubCollectTool(GithubCollector collector,
                                  ObjectMapper objectMapper,
                                  ToolRegistry registry) {
        super(registry, objectMapper);
        this.collector = collector;
    }

    @Override
    public String getName() {
        return "scout_github";
    }

    @Override
    public String getDescription() {
        return "搜索 GitHub 近期热门开源项目（按 Star 数排序，筛选最近 7 天新建项目）。"
                + "当用户说「GitHub 有什么新项目」「最近有什么热门开源」「GitHub trending」时调用。";
    }

    @Override
    public JsonNode getParameters() {
        return schema()
                .string("query", "搜索关键词，如 AI agent、Spring Boot、LLM", true)
                .build();
    }

    @Override
    public String execute(String argumentsJson, ToolExecutionContext context) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            String query = args.path("query").asText("AI");

            SearchTask task = SearchTask.of(query, "GITHUB", "用户查看 GitHub 热门项目", 5);
            List<InformationItem> items = collector.collect(task);

            ObjectNode result = objectMapper.createObjectNode();
            result.put("status", "SUCCESS");
            result.put("count", items.size());
            var arr = result.putArray("items");
            for (InformationItem item : items) {
                ObjectNode node = arr.addObject();
                node.put("title", item.getTitle());
                node.put("content", item.getContent() != null ? item.getContent() : "");
                node.put("source", item.getSource() != null ? item.getSource() : "");
            }

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("GitHub 采集工具执行失败", e);
            return "{\"status\":\"ERROR\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }
}
