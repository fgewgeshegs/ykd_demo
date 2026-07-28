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
 * 选择团建方案并处理预算决策工具。
 *
 * <p>当用户从多个候选方案中明确选择一个时调用。
 * 如果用户选择的方案超预算，本工具同时支持记录用户的超预算决定
 * （接受超支、修改到预算内、更新预算上限、查看调整选项）。
 */
@Component
public class TeamTripSelectOptionFunction implements LLMFunction {

    private static final Logger log = LoggerFactory.getLogger(TeamTripSelectOptionFunction.class);

    private final TeamTripPlanService planService;
    private final ObjectMapper objectMapper;
    private final LLMFunctionRegistry registry;

    public TeamTripSelectOptionFunction(TeamTripPlanService planService,
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
        return "team_trip_select_option";
    }

    @Override
    public String getDescription() {
        return "记录用户选择的团建方案和处理超预算决定。"
                + "当用户从候选方案中明确选择一个时调用（如选第一个、方案B等）。"
                + "如果用户选择的方案超出预算，后续可再次调用本工具来记录用户的超预算决定："
                + "ACCEPT_OVERRUN（接受超支）、REVISE_TO_BUDGET（调整到预算内）、"
                + "UPDATE_BUDGET_LIMIT（更新预算上限）、SHOW_ADJUSTMENT_OPTIONS（查看调整选项）。"
                + "用户未选择方案时不要调用。";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "object");
        ObjectNode p = root.putObject("properties");

        property(p, "selected_option_id", "string", "用户选择的候选方案标识，如 plan_a");

        property(p, "budget_decision", "string",
                "可选：用户对超预算的决定。"
                + "ACCEPT_OVERRUN=用户接受超出金额，REVISE_TO_BUDGET=用户要求调整到预算内，"
                + "UPDATE_BUDGET_LIMIT=用户更新了预算上限，SHOW_ADJUSTMENT_OPTIONS=用户要求查看可调整项目");
        ((ObjectNode) p.get("budget_decision")).putArray("enum")
                .add("ACCEPT_OVERRUN").add("REVISE_TO_BUDGET")
                .add("UPDATE_BUDGET_LIMIT").add("SHOW_ADJUSTMENT_OPTIONS");

        property(p, "new_budget_total", "number", "用户更新后的总预算上限，单位元；与人均预算至少提供一个");
        property(p, "new_budget_per_person", "number", "用户更新后的人均预算上限，单位元；与总预算至少提供一个");
        property(p, "adjustment_preferences", "string", "用户希望保留或优先调整的内容");

        return root;
    }

    @Override
    public String execute(String argumentsJson) {
        return execute(argumentsJson, new FunctionExecutionContext(""));
    }

    @Override
    public String execute(String argumentsJson, FunctionExecutionContext context) {
        try {
            ObjectNode args = (ObjectNode) objectMapper.readTree(argumentsJson);

            // budget_decision 存在时路由到 budget_decision action
            if (args.has("budget_decision") && !args.get("budget_decision").asText().isBlank()) {
                args.put("action", "budget_decision");
            } else {
                args.put("action", "select_option");
            }
            return objectMapper.writeValueAsString(planService.handle(args));
        } catch (Exception e) {
            log.error("team_trip_select_option 执行失败 | error={}", e.getMessage());
            return error("选择方案失败: " + e.getMessage());
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
