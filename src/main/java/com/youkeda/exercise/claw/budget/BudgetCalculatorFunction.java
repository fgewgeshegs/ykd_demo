package com.youkeda.exercise.claw.budget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.tool.LLMFunction;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/** 将方案成本核算服务注册为可供 LLM 调用的独立工具。 */
@Component
public class BudgetCalculatorFunction implements LLMFunction {

    private final BudgetCalculatorService calculatorService;
    private final ObjectMapper objectMapper;
    private final LLMFunctionRegistry registry;

    public BudgetCalculatorFunction(BudgetCalculatorService calculatorService,
                                    ObjectMapper objectMapper,
                                    LLMFunctionRegistry registry) {
        this.calculatorService = calculatorService;
        this.objectMapper = objectMapper;
        this.registry = registry;
    }

    @PostConstruct
    public void init() {
        registry.register(this);
    }

    @Override
    public String getName() {
        return "budget_calculator";
    }

    @Override
    public String getDescription() {
        return "根据一个或多个已设计方案的具体费用项目、计费方式、数量和单价，"
                + "核算方案预计总费用、人均费用、分类明细和预算差额。"
                + "本工具不按固定行业比例分配用户预算，也不生成或猜测单价。"
                + "价格缺失时必须传 price_status=MISSING，工具会返回 PARTIAL。"
                + "完整团建方案中的所有乘法、房间/车辆取整和费用汇总必须使用本工具。";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "object");
        ObjectNode p = root.putObject("properties");
        property(p, "headcount", "integer", "参加人数，正整数");
        property(p, "days", "integer", "出行天数，正整数");
        property(p, "nights", "integer", "住宿晚数；未提供时默认 days-1");
        property(p, "city", "string", "目的地城市，仅用于方案上下文，不使用虚构城市系数");
        property(p, "target_total_budget", "number", "用户总预算上限；与人均预算至少提供一个，仅用于超支比较");
        property(p, "target_per_person_budget", "number", "用户人均预算上限；与总预算至少提供一个，仅用于超支比较");
        property(p, "contingency_rate", "number", "可选：机动费用比例，0到1，默认0.10");

        ObjectNode plans = p.putObject("plans");
        plans.put("type", "array");
        plans.put("description", "待核算的一个或多个候选方案");
        ObjectNode plan = plans.putObject("items");
        plan.put("type", "object");
        ObjectNode pp = plan.putObject("properties");
        property(pp, "plan_id", "string", "稳定的内部方案标识");
        property(pp, "plan_name", "string", "面向用户的方案名称");
        property(pp, "plan_version", "integer", "方案版本，默认1");

        ObjectNode items = pp.putObject("items");
        items.put("type", "array");
        items.put("description", "该方案的费用项目");
        ObjectNode item = items.putObject("items");
        item.put("type", "object");
        ObjectNode ip = item.putObject("properties");
        property(ip, "category", "string", "TRANSPORT、ACCOMMODATION、MEAL、TICKET、ACTIVITY、INSURANCE、MATERIAL、VENUE、SERVICE 或 OTHER");
        property(ip, "item_name", "string", "具体费用项目名称");
        property(ip, "pricing_mode", "string", "FIXED、PER_PERSON、PER_PERSON_PER_DAY、PER_PERSON_PER_OCCURRENCE、PER_ROOM_PER_NIGHT、PER_VEHICLE、PER_TABLE 或 PER_UNIT");
        property(ip, "unit_price", "number", "确定单价；有价格区间时不填写");
        property(ip, "min_unit_price", "number", "最低参考单价");
        property(ip, "max_unit_price", "number", "最高参考单价");
        property(ip, "quantity", "number", "明确数量；PER_UNIT 必填，也可覆盖车辆、房间或桌数的自动计算");
        property(ip, "occurrences", "integer", "餐次、活动次数等");
        property(ip, "capacity", "integer", "每辆车、每间房或每桌容纳人数，用于向上取整");
        property(ip, "applicable_headcount", "integer", "该项目实际计费人数；未提供时使用总人数");
        property(ip, "price_source", "string", "官方、商家、专业工具或网页来源");
        property(ip, "price_status", "string", "CONFIRMED、ESTIMATED 或 MISSING");
        property(ip, "notes", "string", "价格假设和补充说明");
        item.putArray("required").add("category").add("item_name")
                .add("pricing_mode").add("price_status");
        plan.putArray("required").add("plan_id").add("plan_name").add("items");
        root.putArray("required").add("headcount").add("days").add("plans");
        return root;
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            BatchPlanCostRequest request = objectMapper.readValue(argumentsJson, BatchPlanCostRequest.class);
            return objectMapper.writeValueAsString(calculatorService.calculate(request));
        } catch (Exception e) {
            ObjectNode result = objectMapper.createObjectNode();
            result.put("status", "INVALID_ARGUMENT");
            result.putArray("warnings").add("成本核算参数解析失败：" + e.getMessage());
            return result.toString();
        }
    }

    private void property(ObjectNode properties, String name, String type, String description) {
        ObjectNode node = properties.putObject(name);
        node.put("type", type);
        node.put("description", description);
    }
}
