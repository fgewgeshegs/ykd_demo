package com.youkeda.exercise.claw.transport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.tool.LLMFunction;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import com.youkeda.exercise.claw.transport.model.TransportRequest;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 交通方式推荐函数（LLM Function Calling 适配器）
 *
 * <p>将交通方式推荐能力以 LLM Function Calling 方式暴露给 ReActAgentExecutor。
 * 注册函数：{@code transport_recommend}。
 *
 * <p>函数被调用后自动由 {@link LLMFunctionRegistry} 管理，
 * ReActAgentExecutor 在 tool-calling 循环中自动发现并调用。
 *
 * <p>核心链路：
 * <ol>
 *   <li>LLM 提取参数：from、to、people、budget</li>
 *   <li>调用 {@link TransportService#recommend} 执行推荐</li>
 *   <li>返回结构化 JSON 结果</li>
 * </ol>
 */
@Component
public class TransportRecommendFunction implements LLMFunction {

    private static final Logger log = LoggerFactory.getLogger(TransportRecommendFunction.class);

    private final TransportService transportService;
    private final ObjectMapper objectMapper;
    private final LLMFunctionRegistry functionRegistry;

    public TransportRecommendFunction(TransportService transportService,
                                       ObjectMapper objectMapper,
                                       LLMFunctionRegistry functionRegistry) {
        this.transportService = transportService;
        this.objectMapper = objectMapper;
        this.functionRegistry = functionRegistry;
    }

    @PostConstruct
    public void init() {
        functionRegistry.register(this);
        log.info("TransportRecommendFunction 已注册到 LLMFunctionRegistry");
    }

    @Override
    public String getName() {
        return "transport_recommend";
    }

    @Override
    public String getDescription() {
        return "根据出发地、目的地、人数，对比不同交通方式（大巴、自驾、高铁、飞机）的成本和时间，"
                + "并推荐适合团建的交通方案。"
                + "适合用户问「怎么去」「交通方式」「团建出行」时使用。"
                + "返回各方式的总费用、人均费用、耗时和综合推荐。";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("type", "object");

        ObjectNode properties = params.putObject("properties");

        ObjectNode from = properties.putObject("from");
        from.put("type", "string");
        from.put("description", "出发地，城市名称或具体地址，如：上海、杭州、北京");

        ObjectNode to = properties.putObject("to");
        to.put("type", "string");
        to.put("description", "目的地，城市名称或具体地址，如：杭州、南京、苏州");

        ObjectNode people = properties.putObject("people");
        people.put("type", "integer");
        people.put("description", "出行人数，如：30、5、100");

        ObjectNode budget = properties.putObject("budget");
        budget.put("type", "integer");
        budget.put("description", "可选总预算（元），如：50000。不填则不考虑预算限制");

        params.putArray("required").add("from").add("to").add("people");

        return params;
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);

            String from = args.path("from").asText("");
            String to = args.path("to").asText("");
            int people = args.path("people").asInt(1);
            Integer budget = args.has("budget") && !args.get("budget").isNull()
                    ? args.get("budget").asInt() : null;

            // 参数校验
            if (from.isBlank()) {
                return "{\"error\": \"缺少必填参数: from（出发地）\"}";
            }
            if (to.isBlank()) {
                return "{\"error\": \"缺少必填参数: to（目的地）\"}";
            }
            if (people <= 0) {
                return "{\"error\": \"参数 people 必须大于 0\"}";
            }

            log.info("TransportRecommendFunction 执行 | from={} | to={} | people={} | budget={}",
                    from, to, people, budget);

            return transportService.recommend(new TransportRequest(from, to, people, budget));

        } catch (Exception e) {
            log.error("TransportRecommendFunction 执行失败 | args={} | error={}",
                    argumentsJson, e.getMessage());
            return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }
}