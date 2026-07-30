package com.youkeda.exercise.claw.scout.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.runtime.Tool;
import com.youkeda.exercise.claw.agent.runtime.ToolRegistry;
import com.youkeda.exercise.claw.scout.planner.SearchTask;
import com.youkeda.exercise.claw.scout.processor.InformationItem;
import jakarta.annotation.PostConstruct;
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
public class GithubCollectFunction implements Tool {

    private static final Logger log = LoggerFactory.getLogger(GithubCollectFunction.class);

    private final GithubCollector collector;
    private final ObjectMapper objectMapper;
    private final ToolRegistry registry;

    public GithubCollectFunction(GithubCollector collector,
                                  ObjectMapper objectMapper,
                                  ToolRegistry registry) {
        this.collector = collector;
        this.objectMapper = objectMapper;
        this.registry = registry;
    }

    @PostConstruct
    public void init() {
        registry.register(this);
        log.info("GithubCollectFunction 已注册");
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
        return objectMapper.createObjectNode()
                .put("type", "object")
                .<ObjectNode>set("properties", objectMapper.createObjectNode()
                        .<ObjectNode>set("query", objectMapper.createObjectNode()
                                .put("type", "string")
                                .put("description", "搜索关键词，如 AI agent、Spring Boot、LLM")))
                .set("required", objectMapper.createArrayNode().add("query"));
    }

    @Override
    public String execute(String argumentsJson) {
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
