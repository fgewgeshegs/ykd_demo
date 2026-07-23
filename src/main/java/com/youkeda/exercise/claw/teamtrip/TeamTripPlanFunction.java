package com.youkeda.exercise.claw.teamtrip;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.tool.FunctionExecutionContext;
import com.youkeda.exercise.claw.agent.tool.LLMFunction;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/** 团建/出游方案需求收集、流程控制和修订工具。 */
@Component
public class TeamTripPlanFunction implements LLMFunction {

    private final TeamTripPlanService planService;
    private final ObjectMapper objectMapper;
    private final LLMFunctionRegistry registry;

    public TeamTripPlanFunction(TeamTripPlanService planService, ObjectMapper objectMapper,
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
        return "team_trip_plan";
    }

    @Override
    public String getDescription() {
        return "团建、公司出游、部门活动、集体旅行或完整多人行程的需求收集、候选方案和用户决策工具。"
                + "新方案缺少出发地、人数、日期、天数、目的地或范围、数值预算时，"
                + "应先直接询问用户，不调用本工具；必填信息基本齐全后才首次调用。"
                + "已有方案状态时，用本工具记录用户补充或修改的信息。"
                + "普通景点问答和简单地点推荐不调用。"
                + "工具会记住多轮需求、多个候选方案、用户选择、超预算确认和方案修订。"
                + "本工具不计算费用；方案具体金额必须由独立的 budget_calculator 根据费用项目核算。"
                + "调用时只传本轮新获得或被用户修改的信息，不要猜测缺失内容。";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "object");
        ObjectNode p = root.putObject("properties");
        property(p, "action", "string", "collect=收集/更新需求，save_options=保存候选方案，"
                + "select_option=选择方案，combine_options=组合方案，revise_option=修改指定方案，"
                + "budget_decision=记录超预算决定，revise=通用修订，reset=重新开始");
        ((ObjectNode) p.get("action")).putArray("enum")
                .add("collect").add("save_options").add("select_option")
                .add("combine_options").add("revise_option")
                .add("budget_decision").add("revise").add("reset");
        property(p, "departure_city", "string", "出发城市或集合地点");
        property(p, "participant_count", "integer", "参加人数，正整数");
        property(p, "travel_date", "string", "出行日期或时间范围");
        property(p, "duration", "string", "出行时长，如2天1晚");
        property(p, "days", "integer", "标准化出行天数");
        property(p, "nights", "integer", "住宿晚数");
        property(p, "option_count", "integer", "候选方案数量；用户未指定时默认3个，明确指定时最多5个");
        ((ObjectNode) p.get("option_count")).put("minimum", 1);
        ((ObjectNode) p.get("option_count")).put("maximum", 5);
        property(p, "budget_total", "number", "用户可接受的团队总预算上限，单位元；与人均预算至少提供一个");
        property(p, "budget_per_person", "number", "用户可接受的人均预算上限，单位元；与总预算至少提供一个");
        property(p, "budget_level", "string", "可选：经济型、标准型或品质型偏好，不能代替数值预算");
        property(p, "max_overrun_amount", "number", "可选：用户提前允许的最大超预算金额");
        property(p, "max_overrun_rate", "number", "可选：用户提前允许的最大超预算比例，百分数");
        property(p, "destination", "string", "确定的目的地");
        property(p, "travel_scope", "string", "目的地未定时可接受的范围");
        property(p, "team_goal", "string", "团建目标");
        property(p, "activity_preferences", "string", "活动偏好");
        property(p, "participant_profile", "string", "年龄、体力和人员构成");
        property(p, "transport_preference", "string", "交通偏好");
        property(p, "accommodation_preference", "string", "住宿要求");
        property(p, "meal_preferences", "string", "餐饮、忌口或过敏");
        property(p, "special_requirements", "string", "安全、无障碍、会议室、发票等要求");
        ObjectNode priorities = p.putObject("priorities");
        priorities.put("type", "array");
        priorities.put("description", "用户明确提出的优先因素，按重要程度排列；未提出时不要填写");
        priorities.putObject("items").put("type", "string");
        ObjectNode options = p.putObject("options");
        options.put("type", "array");
        options.put("description", "save_options 使用；未指定数量时必须保存3个，明确指定时按 option_count 保存，最多5个");
        ObjectNode option = options.putObject("items");
        option.put("type", "object");
        ObjectNode optionProperties = option.putObject("properties");
        property(optionProperties, "option_id", "string", "稳定的内部方案标识");
        property(optionProperties, "display_name", "string", "方案A、方案B等用户可见名称");
        property(optionProperties, "positioning", "string", "经济型、均衡型、体验型等定位");
        property(optionProperties, "highlights", "string", "方案主要亮点");
        property(optionProperties, "itinerary_summary", "string", "方案行程概要");
        option.putArray("required").add("option_id").add("display_name")
                .add("positioning").add("itinerary_summary");
        property(p, "option_id", "string", "需要选择或修改的候选方案标识");
        property(p, "selected_option_id", "string", "select_option 使用的候选方案标识");
        ObjectNode sourceOptions = p.putObject("source_option_ids");
        sourceOptions.put("type", "array");
        sourceOptions.put("description", "combine_options 使用，被组合的候选方案标识");
        sourceOptions.putObject("items").put("type", "string");
        property(p, "display_name", "string", "组合或修订后的方案名称");
        property(p, "positioning", "string", "组合或修订后的方案定位");
        property(p, "highlights", "string", "组合或修订后的方案亮点");
        property(p, "itinerary_summary", "string", "组合或修订后的行程概要");
        property(p, "budget_decision", "string", "ACCEPT_OVERRUN、REVISE_TO_BUDGET、UPDATE_BUDGET_LIMIT 或 SHOW_ADJUSTMENT_OPTIONS");
        ((ObjectNode) p.get("budget_decision")).putArray("enum")
                .add("ACCEPT_OVERRUN").add("REVISE_TO_BUDGET")
                .add("UPDATE_BUDGET_LIMIT").add("SHOW_ADJUSTMENT_OPTIONS");
        property(p, "new_budget_total", "number", "用户更新后的总预算上限");
        property(p, "new_budget_per_person", "number", "用户更新后的人均预算上限");
        property(p, "adjustment_preferences", "string", "用户希望保留或优先调整的内容");
        property(p, "feedback", "string", "用户对旧方案的不满意或修改意见，仅 revise 使用");
        root.putArray("required").add("action");
        return root;
    }

    @Override
    public String execute(String argumentsJson) {
        return execute(argumentsJson, new FunctionExecutionContext("anonymous", ""));
    }

    @Override
    public String execute(String argumentsJson, FunctionExecutionContext context) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            if (args == null || !args.isObject()) return error("参数必须是 JSON 对象");
            normalizeAliases((ObjectNode) args);
            String userId = context != null && context.userId() != null ? context.userId() : "anonymous";
            return objectMapper.writeValueAsString(planService.handle(userId, args));
        } catch (Exception e) {
            return error("团建方案参数解析失败: " + e.getMessage());
        }
    }

    private void property(ObjectNode properties, String name, String type, String description) {
        ObjectNode node = properties.putObject(name);
        node.put("type", type);
        node.put("description", description);
    }

    /**
     * 兼容模型偶尔生成的常见别名和驼峰字段，统一转换为工具契约中的 snake_case。
     */
    private void normalizeAliases(ObjectNode args) {
        copyAlias(args, "departure_city", "origin", "departureCity");
        copyAlias(args, "participant_count", "people", "headcount", "participantCount");
        copyAlias(args, "travel_date", "start_date", "travelDate", "startDate");
        copyAlias(args, "budget_total", "budget", "total_budget", "budgetTotal");
        copyAlias(args, "budget_per_person", "per_person_budget", "budgetPerPerson");
        copyAlias(args, "selected_option_id", "selectedOptionId");

        JsonNode options = args.get("options");
        if (options != null && options.isArray()) {
            for (JsonNode option : options) {
                if (!(option instanceof ObjectNode object)) continue;
                copyAlias(object, "option_id", "optionId", "plan_id", "planId");
                copyAlias(object, "display_name", "displayName", "plan_name", "planName");
                copyAlias(object, "itinerary_summary", "itinerarySummary");
            }
        }
    }

    private void copyAlias(ObjectNode object, String canonical, String... aliases) {
        if (object.has(canonical)) return;
        for (String alias : aliases) {
            JsonNode value = object.get(alias);
            if (value != null && !value.isNull()) {
                object.set(canonical, value);
                return;
            }
        }
    }

    private String error(String message) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("status", "ERROR");
        node.put("error", message);
        return node.toString();
    }
}
