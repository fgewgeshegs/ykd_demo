package com.youkeda.exercise.claw.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 计划提议工具
 *
 * <p>通用计划提议工具。当 LLM 判断用户请求需要多个工具协作完成时，调用此工具
 * 先生成执行计划、展示给用户确认，等待用户反馈后再执行。避免 LLM 擅自执行多步操作
 * 导致用户失去控制感。</p>
 *
 * <p>适合场景：
 * <ul>
 *   <li>多步骤请求（如查天气 → 搜地图 → 预算计算 → 生成PDF）</li>
 *   <li>需要用户选择关键参数的请求（推荐多个方案让用户选）</li>
 *   <li>执行成本较高的请求（先确认再执行，避免浪费）</li>
 * </ul>
 *
 * <p>调用约定：LLM 调用此工具后，必须停止继续调用其他工具，
 * 将计划以自然语言展示给用户，等待用户回复确认或修改。</p>
 */
@Component
public class PlanProposalFunction implements LLMFunction {

    private static final Logger log = LoggerFactory.getLogger(PlanProposalFunction.class);

    private final ObjectMapper objectMapper;
    private final LLMFunctionRegistry functionRegistry;

    public PlanProposalFunction(ObjectMapper objectMapper,
                                LLMFunctionRegistry functionRegistry) {
        this.objectMapper = objectMapper;
        this.functionRegistry = functionRegistry;
    }

    @PostConstruct
    public void init() {
        functionRegistry.register(this);
        log.info("PlanProposalFunction 已注册到 LLMFunctionRegistry");
    }

    @Override
    public String getName() {
        return "plan_proposal";
    }

    @Override
    public String getDescription() {
        return "当你判断用户请求需要多个工具协作完成"
                + "（如同时需要查天气、搜地图、预算计算、生成文件等），"
                + "或需要用户确认关键参数后才能继续执行时，"
                + "调用此工具生成执行计划给用户确认。"
                + "调用后必须停止调用其他工具，将计划展示给用户并等待回复。"
                + "如果用户请求很简单（只需一个工具调用或纯对话），不需要调用此工具。";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "object");

        ObjectNode properties = root.putObject("properties");

        ObjectNode goal = properties.putObject("goal");
        goal.put("type", "string");
        goal.put("description", "本次任务的总体目标，如：为周末团建做攻略");

        ObjectNode steps = properties.putObject("steps");
        steps.put("type", "array");
        steps.put("description", "执行步骤列表（按顺序），至少 2 步");

        ObjectNode stepItems = steps.putObject("items");
        stepItems.put("type", "object");
        ObjectNode stepProps = stepItems.putObject("properties");

        ObjectNode stepName = stepProps.putObject("step");
        stepName.put("type", "string");
        stepName.put("description", "步骤名称，如：查询天气");

        ObjectNode stepDesc = stepProps.putObject("description");
        stepDesc.put("type", "string");
        stepDesc.put("description", "步骤说明，如：查看目的地周末天气情况");

        ArrayNode stepRequired = stepItems.putArray("required");
        stepRequired.add("step");

        ArrayNode required = root.putArray("required");
        required.add("goal");
        required.add("steps");

        return root;
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            String goal = args.has("goal") ? args.get("goal").asText() : "(未说明)";

            log.info("PlanProposal 执行 | goal={} | steps={}",
                    goal, args.has("steps") ? args.get("steps").size() : 0);

            // 返回结果中携带完整计划信息，LLM 将据此生成面向用户的展示文本
            ObjectNode result = objectMapper.createObjectNode();
            result.put("status", "proposed");
            result.put("message",
                    "计划已记录。请现在以自然语言将上述执行计划展示给用户，"
                            + "明确告知用户计划包含哪些步骤，"
                            + "并询问用户是否同意执行、需要调整哪一步、或取消。"
                            + "在用户回复确认之前，不得继续调用其他任何工具。");
            result.set("plan", args);

            return result.toString();

        } catch (Exception e) {
            log.error("PlanProposal 执行失败 | args={} | error={}", argumentsJson, e.getMessage());
            return "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }
}
