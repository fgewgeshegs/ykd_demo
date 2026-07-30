package com.youkeda.exercise.claw.scout.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.tool.FunctionExecutionContext;
import com.youkeda.exercise.claw.agent.tool.LLMFunction;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import com.youkeda.exercise.claw.scout.planner.SearchTask;
import com.youkeda.exercise.claw.scout.processor.InformationItem;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RSS 采集工具（独立调用）
 *
 * 从配置的 RSS 订阅源获取最新文章
 */
@Component
@ConditionalOnProperty(name = "scout.rss.enabled", havingValue = "true")
public class RssCollectFunction implements LLMFunction {

    private static final Logger log = LoggerFactory.getLogger(RssCollectFunction.class);

    private final RssCollector collector;
    private final ObjectMapper objectMapper;
    private final LLMFunctionRegistry registry;

    public RssCollectFunction(RssCollector collector,
                                ObjectMapper objectMapper,
                                LLMFunctionRegistry registry) {
        this.collector = collector;
        this.objectMapper = objectMapper;
        this.registry = registry;
    }

    @PostConstruct
    public void init() {
        registry.register(this);
        log.info("RssCollectFunction 已注册");
    }

    @Override
    public String getName() {
        return "scout_rss";
    }

    @Override
    public String getDescription() {
        return "从 RSS 订阅源获取最新文章。订阅源包括 OpenAI Blog、Spring Blog、GitHub Blog、Hacker News 等。"
                + "当用户说「看看订阅源有什么新文章」「RSS 更新」时调用。";
    }

    @Override
    public JsonNode getParameters() {
        return objectMapper.createObjectNode()
                .put("type", "object")
                .<ObjectNode>set("properties", objectMapper.createObjectNode());
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            SearchTask task = SearchTask.of("RSS latest", "BLOG", "用户查看 RSS 订阅", 5);
            List<InformationItem> items = collector.collect(task);

            ObjectNode result = objectMapper.createObjectNode();
            result.put("status", "SUCCESS");
            result.put("count", items.size());
            var arr = result.putArray("items");
            for (InformationItem item : items) {
                ObjectNode node = arr.addObject();
                node.put("title", item.getTitle());
                node.put("content", item.getContent() != null
                        ? (item.getContent().length() > 200
                                ? item.getContent().substring(0, 200) + "..."
                                : item.getContent())
                        : "");
                node.put("source", item.getSource() != null ? item.getSource() : "");
            }

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("RSS 采集工具执行失败", e);
            return "{\"status\":\"ERROR\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }
}
