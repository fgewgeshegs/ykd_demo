package com.youkeda.exercise.claw.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.teamtrip.TeamTripPlanService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 团建需求收集工具。
 *
 * <p>当用户需要制定团建/出游方案且缺少关键信息（出发地、人数、日期、天数、目的地、预算）时调用。
 * 信息不足时返回 NEED_MORE_INFORMATION 和具体缺失字段，LLM 应据此追问。
 */
@Component
public class TeamTripCollectFunction implements LLMFunction {

    private static final Logger log = LoggerFactory.getLogger(TeamTripCollectFunction.class);

    private final TeamTripPlanService planService;
    private final ObjectMapper objectMapper;
    private final LLMFunctionRegistry registry;

    public TeamTripCollectFunction(TeamTripPlanService planService,
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
        return "team_trip_collect";
    }

    @Override
    public String getDescription() {
        return "收集和更新团建规划需求。"
                + "当用户需要制定团建、公司出游、部门活动、集体旅行或完整多人行程方案时调用。"
                + "传入用户已提供的信息（出发地、人数、日期、天数、目的地/范围、预算等）；"
                + "必要字段缺失时返回 NEED_MORE_INFORMATION 和缺失字段列表，LLM 应逐一追问。"
                + "新方案首次调用前，若明显缺少必填信息（缺3项以上），应先用文字一次性追问，不调用此工具。"
                + "已有方案状态时，用此工具记录用户补充或修改的信息。"
                + "普通景点问答和简单地点推荐不调用。";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "object");
        ObjectNode p = root.putObject("properties");

        property(p, "departure_city", "string", "出发城市或集合地点");
        property(p, "participant_count", "integer", "参加人数，正整数");
        property(p, "travel_date", "string", "出行日期或时间范围");
        property(p, "duration", "string", "出行时长，如2天1晚");
        property(p, "days", "integer", "标准化出行天数");
        property(p, "nights", "integer", "住宿晚数");
        property(p, "option_count", "integer", "候选方案数量；用户未指定时默认3个，明确指定时最多5个");
        property(p, "budget_total", "number", "用户可接受的团队总预算上限，单位元；与人均预算至少提供一个");
        property(p, "budget_per_person", "number", "用户可接受的人均预算上限，单位元；与总预算至少提供一个");
        property(p, "budget_level", "string", "可选：经济型、标准型或品质型偏好，不能代替数值预算");
        property(p, "max_overrun_amount", "number", "可选：用户提前允许的最大超预算金额");
        property(p, "max_overrun_rate", "number", "可选：用户提前允许的最大超预算比例，百分数");
        property(p, "destination", "string", "确定的目的地");
        property(p, "travel_scope", "string", "目的地未定时可接受的范围");
        property(p, "team_goal", "string", "团建目标");
        property(p, "activity_preferences", "string", "活动偏好，如户外、室内、水上、文化体验");
        property(p, "participant_profile", "string", "年龄、体力和人员构成");
        property(p, "transport_preference", "string", "交通偏好");
        property(p, "accommodation_preference", "string", "住宿要求");
        property(p, "meal_preferences", "string", "餐饮、忌口或过敏");
        property(p, "special_requirements", "string", "安全、无障碍、会议室、发票等要求");

        ObjectNode priorities = p.putObject("priorities");
        priorities.put("type", "array");
        priorities.put("description", "用户明确提出的优先因素，按重要程度排列；未提出时不要填写");
        priorities.putObject("items").put("type", "string");

        return root;
    }

    @Override
    public String execute(String argumentsJson) {
        return execute(argumentsJson, new FunctionExecutionContext("", ""));
    }

    @Override
    public String execute(String argumentsJson, FunctionExecutionContext context) {
        try {
            ObjectNode args = (ObjectNode) objectMapper.readTree(argumentsJson);
            return objectMapper.writeValueAsString(planService.handle(args));
        } catch (Exception e) {
            log.error("team_trip_collect 执行失败 | error={}", e.getMessage());
            return error("团建需求收集失败: " + e.getMessage());
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
