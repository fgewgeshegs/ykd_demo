package com.youkeda.exercise.claw.tool.scout;
import com.youkeda.exercise.claw.scout.collector.JobCollector;

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
 * 招聘信息采集工具（独立调用）
 */
@Component
public class JobCollectTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(JobCollectTool.class);

    private final JobCollector collector;
    private final ObjectMapper objectMapper;
    private final ToolRegistry registry;

    public JobCollectTool(JobCollector collector,
                                ObjectMapper objectMapper,
                                ToolRegistry registry) {
        this.collector = collector;
        this.objectMapper = objectMapper;
        this.registry = registry;
    }

    @PostConstruct
    public void init() {
        registry.register(this);
        log.info("JobCollectTool 已注册");
    }

    @Override
    public String getName() {
        return "scout_job";
    }

    @Override
    public String getDescription() {
        return "搜索最新招聘信息、岗位需求、薪资趋势。"
                + "当用户说「有什么招聘」「找工作」「岗位需求」「哪些公司在招人」时调用。";
    }

    @Override
    public JsonNode getParameters() {
        return objectMapper.createObjectNode()
                .put("type", "object")
                .<ObjectNode>set("properties", objectMapper.createObjectNode()
                        .<ObjectNode>set("query", objectMapper.createObjectNode()
                                .put("type", "string")
                                .put("description", "搜索关键词，如 AI工程师、Java后端、产品经理")))
                .set("required", objectMapper.createArrayNode().add("query"));
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            String query = args.path("query").asText("AI engineer");

            SearchTask task = SearchTask.of(query, "JOB", "用户查询招聘", 5);
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
            log.error("招聘工具执行失败", e);
            return "{\"status\":\"ERROR\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }
}
