package com.youkeda.exercise.claw.scout.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.tool.LLMFunction;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import com.youkeda.exercise.claw.scout.planner.SearchTask;
import com.youkeda.exercise.claw.scout.processor.InformationItem;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 比赛信息采集工具（独立调用）
 */
@Component
public class CompetitionCollectFunction implements LLMFunction {

    private static final Logger log = LoggerFactory.getLogger(CompetitionCollectFunction.class);

    private final CompetitionCollector collector;
    private final ObjectMapper objectMapper;
    private final LLMFunctionRegistry registry;

    public CompetitionCollectFunction(CompetitionCollector collector,
                                        ObjectMapper objectMapper,
                                        LLMFunctionRegistry registry) {
        this.collector = collector;
        this.objectMapper = objectMapper;
        this.registry = registry;
    }

    @PostConstruct
    public void init() {
        registry.register(this);
        log.info("CompetitionCollectFunction 已注册");
    }

    @Override
    public String getName() {
        return "scout_competition";
    }

    @Override
    public String getDescription() {
        return "搜索最新编程比赛、黑客松、AI 竞赛、创新大赛信息。"
                + "当用户说「有什么比赛」「编程竞赛」「黑客松」「AI 比赛」时调用。";
    }

    @Override
    public JsonNode getParameters() {
        return objectMapper.createObjectNode()
                .put("type", "object")
                .<ObjectNode>set("properties", objectMapper.createObjectNode()
                        .<ObjectNode>set("query", objectMapper.createObjectNode()
                                .put("type", "string")
                                .put("description", "搜索关键词，如 AI竞赛、Kaggle、黑客松")))
                .set("required", objectMapper.createArrayNode().add("query"));
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            String query = args.path("query").asText("AI competition hackathon");

            SearchTask task = SearchTask.of(query, "COMPETITION", "用户查询比赛", 5);
            List<InformationItem> items = collector.collect(task);

            ObjectNode result = objectMapper.createObjectNode();
            result.put("status", "SUCCESS");
            result.put("count", items.size());
            var arr = result.putArray("items");
            for (InformationItem item : items) {
                ObjectNode node = arr.addObject();
                node.put("title", item.getTitle());
                node.put("content", item.getContent() != null
                        ? (item.getContent().length() > 300
                                ? item.getContent().substring(0, 300) + "..."
                                : item.getContent())
                        : "");
                node.put("source", item.getSource() != null ? item.getSource() : "");
            }

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("比赛工具执行失败", e);
            return "{\"status\":\"ERROR\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }
}
