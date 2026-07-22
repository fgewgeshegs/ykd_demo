package com.youkeda.exercise.claw.websearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.tool.LLMFunction;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 网页搜索函数（LLM Function Calling 适配器）
 *
 * <p>包装 {@link SearchService}，使其可供 LLM 通过工具调用方式使用。
 * LLM 生成 {"query": "云南旅游最新政策"} 参数，本函数执行后返回结构化搜索结果。
 *
 * <p>返回结果包含：
 * <ul>
 *   <li>{@code answer} — AI 摘要，LLM 可直接引用来组织回答</li>
 *   <li>{@code results[]} — 搜索结果列表，每项含 title / url / content / score</li>
 * </ul>
 *
 * <p>定位：当 LLM 发现已有专业工具无法满足的信息需求时调用。
 */
@Component
public class WebSearchFunction implements LLMFunction {

    private static final Logger log = LoggerFactory.getLogger(WebSearchFunction.class);

    private final SearchService searchService;
    private final ObjectMapper objectMapper;
    private final LLMFunctionRegistry functionRegistry;

    public WebSearchFunction(SearchService searchService,
                             ObjectMapper objectMapper,
                             LLMFunctionRegistry functionRegistry) {
        this.searchService = searchService;
        this.objectMapper = objectMapper;
        this.functionRegistry = functionRegistry;
    }

    @PostConstruct
    public void init() {
        functionRegistry.register(this);
        log.info("WebSearchFunction 已注册到 LLMFunctionRegistry");
    }

    @Override
    public String getName() {
        return "web_search";
    }

    @Override
    public String getDescription() {
        return "搜索互联网获取实时信息。必须在以下场景调用："
                + "旅游景点推荐、行程规划、美食攻略、交通方式、门票价格、开放时间、"
                + "酒店住宿、旅游政策、天气穿衣、当地习俗等。"
                + "禁止凭记忆回答旅游相关问题，必须先搜索再回答。";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("type", "object");

        ObjectNode properties = params.putObject("properties");
        ObjectNode query = properties.putObject("query");
        query.put("type", "string");
        query.put("description", "搜索关键词，简洁明确的中文关键词组合。如：云南旅游最新政策、丽江景区开放情况");

        ObjectNode count = properties.putObject("count");
        count.put("type", "integer");
        count.put("description", "返回结果条数，默认5条，最多10条");

        params.putArray("required").add("query");

        return params;
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            JsonNode queryNode = args.get("query");
            if (queryNode == null) {
                return "{\"error\": \"缺少必填参数: query\"}";
            }

            String query = queryNode.asText();
            int count = args.has("count") ? args.get("count").asInt() : 5;

            log.info("WebSearchFunction 执行 | query={} | count={}", query, count);

            return searchService.search(query, count);

        } catch (Exception e) {
            log.error("WebSearchFunction 执行失败 | args={} | error={}", argumentsJson, e.getMessage());
            return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }
}
