package com.youkeda.exercise.claw.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.teamtrip.TeamTripPlanService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 保存团建候选方案工具。
 *
 * <p>当一个或多个候选方案已成形，需要进入比较和选择阶段时调用。
 * 方案必须有明确标识、名称、定位和行程概要。
 */
@Component
public class TeamTripSaveOptionsFunction implements LLMFunction {

    private static final Logger log = LoggerFactory.getLogger(TeamTripSaveOptionsFunction.class);

    private final TeamTripPlanService planService;
    private final ObjectMapper objectMapper;
    private final LLMFunctionRegistry registry;

    public TeamTripSaveOptionsFunction(TeamTripPlanService planService,
                                       ObjectMapper objectMapper,
                                       LLMFunctionRegistry registry) {
        this.planService = planService;
        this.objectMapper = objectMapper;
        this.registry = registry;
    }

    @PostConstruct
    public void init() {
        registry.register(this);
    }

    @Override
    public String getName() {
        return "team_trip_save_options";
    }

    @Override
    public String getDescription() {
        return "保存已生成的候选团建方案。"
                + "当已有一个或多个完整的差异化方案，需要进入比较和选择阶段时调用。"
                + "用户未指定数量时默认生成3个方案，明确指定时按指定数量生成，最多5个。"
                + "调用前应先生成各方案的行程和费用项目，调用后再用 budget_calculator 核算总费用。";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "object");
        ObjectNode p = root.putObject("properties");

        property(p, "option_count", "integer", "候选方案数量，必须与当前方案数量一致；用户未指定时默认3，明确指定时最多5");

        ObjectNode options = p.putObject("options");
        options.put("type", "array");
        options.put("description", "待保存的候选方案列表，至少1个，最多5个");
        ObjectNode option = options.putObject("items");
        option.put("type", "object");
        ObjectNode op = option.putObject("properties");
        property(op, "option_id", "string", "稳定的内部方案标识，如 plan_a、plan_b");
        property(op, "display_name", "string", "面向用户的方案名称，如方案A、方案B");
        property(op, "positioning", "string", "方案定位：经济型、均衡型、体验型等");
        property(op, "highlights", "string", "方案主要亮点，区别于其他方案的核心特色");
        property(op, "itinerary_summary", "string", "方案行程概要，概述每天的主要活动和安排");
        option.putArray("required").add("option_id").add("display_name")
                .add("positioning").add("itinerary_summary");

        root.putArray("required").add("options");
        return root;
    }

    @Override
    public String execute(String argumentsJson) {
        return execute(argumentsJson, new FunctionExecutionContext("anonymous", ""));
    }

    @Override
    public String execute(String argumentsJson, FunctionExecutionContext context) {
        try {
            ObjectNode args = (ObjectNode) objectMapper.readTree(argumentsJson);
            args.put("action", "save_options");
            String userId = context != null && context.userId() != null
                    ? context.userId() : "anonymous";
            return objectMapper.writeValueAsString(planService.handle(userId, args));
        } catch (Exception e) {
            log.error("team_trip_save_options 执行失败 | error={}", e.getMessage());
            return error("保存候选方案失败: " + e.getMessage());
        }
    }

    private void property(ObjectNode properties, String name, String type, String description) {
        ObjectNode node = properties.putObject(name);
        node.put("type", type);
        node.put("description", description);
    }

    private String error(String message) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("status", "ERROR");
        node.put("error", message);
        return node.toString();
    }
}
