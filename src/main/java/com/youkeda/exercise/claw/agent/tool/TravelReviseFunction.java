package com.youkeda.exercise.claw.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.travel.TravelPlanService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 修订旅游方案工具。
 *
 * <p>当用户对已有方案不满意、需要修改、组合方案或指定修订某个方案时调用。
 * 支持通用修订（反馈原话）、指定方案修订、多方案组合三种模式。
 */
@Component
public class TravelReviseFunction implements LLMFunction {

    private static final Logger log = LoggerFactory.getLogger(TravelReviseFunction.class);

    private final TravelPlanService planService;
    private final ObjectMapper objectMapper;
    private final LLMFunctionRegistry registry;

    public TravelReviseFunction(TravelPlanService planService,
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
        return "travel_revise";
    }

    @Override
    public String getDescription() {
        return "根据用户反馈修订旅游方案。"
                + "用户对已有方案不满意、要求修改、组合方案或指定调整某个方案时调用。"
                + "传入 source_option_ids 时为组合模式——从多个源方案生成一个新方案；"
                + "传入 option_id 加其他修改字段时为指定方案修订模式；"
                + "只传入 feedback 时为通用修订模式。"
                + "修订后旧成本失效，需重新调用 budget_calculator 核算。";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "object");
        ObjectNode p = root.putObject("properties");

        property(p, "feedback", "string", "用户对旧方案的不满意或修改意见原文；通用修订时使用");

        property(p, "option_id", "string", "需要修改的候选方案标识；指定方案修订时使用");
        property(p, "display_name", "string", "修订后的方案名称");
        property(p, "positioning", "string", "修订后的方案定位");
        property(p, "highlights", "string", "修订后的方案亮点");
        property(p, "itinerary_summary", "string", "修订后的行程概要");

        ObjectNode sourceOptions = p.putObject("source_option_ids");
        sourceOptions.put("type", "array");
        sourceOptions.put("description", "组合模式——被组合的源方案标识列表，至少2个");
        sourceOptions.putObject("items").put("type", "string");

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

            // 根据参数判断路由
            JsonNode sourceIds = args.get("source_option_ids");
            if (sourceIds != null && sourceIds.isArray() && sourceIds.size() >= 2) {
                args.put("action", "combine_options");
            } else if (args.has("option_id") && !args.get("option_id").asText().isBlank()
                    && (args.has("display_name") || hasAny(args, "positioning", "highlights", "itinerary_summary"))) {
                args.put("action", "revise_option");
            } else {
                args.put("action", "revise");
            }

            return objectMapper.writeValueAsString(planService.handle(args));
        } catch (Exception e) {
            log.error("travel_revise 执行失败 | error={}", e.getMessage());
            return error("修订方案失败: " + e.getMessage());
        }
    }

    private boolean hasAny(ObjectNode node, String... fields) {
        for (String f : fields) {
            JsonNode v = node.get(f);
            if (v != null && !v.isNull() && !v.asText().isBlank()) return true;
        }
        return false;
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
