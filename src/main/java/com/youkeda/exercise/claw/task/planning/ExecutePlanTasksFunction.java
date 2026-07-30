package com.youkeda.exercise.claw.task.planning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.tool.FunctionExecutionContext;
import com.youkeda.exercise.claw.agent.tool.LLMFunction;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import com.youkeda.exercise.claw.task.model.TaskPlan;
import com.youkeda.exercise.claw.task.repository.TaskPlanRepository;
import com.youkeda.exercise.claw.task.service.TaskCreator;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 执行任务计划 LLM Function
 *
 * <p>注册名称：{@code execute_plan_tasks}
 *
 * <p>根据 plan_id 找到之前由 {@link PlanTasksFunction} 生成的预览计划，
 * 批量创建所有 {@link ScheduledTask} 定时任务。
 *
 * <p>只允许执行属于自己的 PREVIEW 状态的计划。
 */
@Component
public class ExecutePlanTasksFunction implements LLMFunction {

    private static final Logger log = LoggerFactory.getLogger(ExecutePlanTasksFunction.class);

    private final ObjectMapper objectMapper;
    private final LLMFunctionRegistry functionRegistry;
    private final TaskPlanRepository planRepository;
    private final TaskCreator taskCreator;

    public ExecutePlanTasksFunction(ObjectMapper objectMapper,
                                    LLMFunctionRegistry functionRegistry,
                                    TaskPlanRepository planRepository,
                                    TaskCreator taskCreator) {
        this.objectMapper = objectMapper;
        this.functionRegistry = functionRegistry;
        this.planRepository = planRepository;
        this.taskCreator = taskCreator;
    }

    @PostConstruct
    public void init() {
        functionRegistry.register(this);
        log.info("ExecutePlanTasksFunction 已注册到 LLMFunctionRegistry");
    }

    @Override
    public String getName() {
        return "execute_plan_tasks";
    }

    @Override
    public String getDescription() {
        return "执行之前规划好的任务计划。\n"
                + "当用户确认了 plan_tasks 生成的计划后，调用此函数实际创建所有定时任务。\n"
                + "使用前需要先通过 plan_tasks 生成计划，获取 plan_id。\n"
                + "例如用户说「就这样安排」「好的，执行吧」「确认」时调用此函数。\n"
                + "参数只需传入 plan_id。";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("type", "object");

        ObjectNode properties = params.putObject("properties");

        ObjectNode planId = properties.putObject("plan_id");
        planId.put("type", "integer");
        planId.put("description", "任务计划 ID，由 plan_tasks 返回的 plan_id。");

        params.putArray("required").add("plan_id");

        return params;
    }

    @Override
    public String execute(String argumentsJson) {
        return "{\"error\": \"缺少用户上下文，无法执行计划\"}";
    }

    @Override
    public String execute(String argumentsJson, FunctionExecutionContext context) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            String userId = context.userId();
            if (userId == null || userId.isBlank()) {
                return "{\"error\": \"缺少用户ID\"}";
            }
            if (!args.has("plan_id") || !args.get("plan_id").canConvertToExactIntegral()) {
                return "{\"error\": \"缺少或无效的 plan_id\"}";
            }

            long planId = args.get("plan_id").asLong();

            // 查询计划
            TaskPlan plan = planRepository.findById(planId);
            if (plan == null) {
                return "{\"error\": \"未找到 ID 为 " + planId + " 的计划\"}";
            }
            if (!userId.equals(plan.getUserId())) {
                log.warn("用户尝试执行非自己的计划 | userId={} | planUserId={}", userId, plan.getUserId());
                return "{\"error\": \"无权执行该计划\"}";
            }
            if (!plan.isPreview()) {
                return "{\"error\": \"计划状态为「" + plan.getStatusDisplay() + "」，无法执行（仅可执行待确认的计划）\"}";
            }

            // 解析任务列表
            JsonNode tasksNode = objectMapper.readTree(plan.getTasksJson());
            if (tasksNode == null || !tasksNode.isArray() || tasksNode.isEmpty()) {
                return "{\"error\": \"计划中没有任务\"}";
            }

            // 批量创建任务
            List<JsonNode> createdTasks = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            for (JsonNode taskNode : tasksNode) {
                TaskCreator.CreateResult result = taskCreator.createFromNode(userId, taskNode);
                if (result.isSuccess()) {
                    ObjectNode created = objectMapper.createObjectNode();
                    created.put("task_id", result.getTask().getId());
                    created.put("content", result.getTask().getContent());
                    created.put("execute_time", result.getTask().getExecuteTimeAsString());
                    created.put("repeat_type", result.getTask().getRepeatType());
                    createdTasks.add(created);
                } else {
                    String order = taskNode.has("order") ? String.valueOf(taskNode.get("order").asInt()) : "?";
                    errors.add("任务" + order + ": " + result.getErrorMessage());
                }
            }

            // 更新计划状态
            int successCount = createdTasks.size();
            if (errors.isEmpty()) {
                planRepository.markDone(planId);
            } else {
                planRepository.markConfirmed(planId, userId);
            }

            log.info("计划执行完成 | planId={} | success={} | errors={}",
                    planId, successCount, errors.size());

            // 构建返回
            ObjectNode result = objectMapper.createObjectNode();
            result.put("status", "executed");
            result.put("plan_id", planId);
            result.put("goal", plan.getGoal());
            result.put("total_count", tasksNode.size());
            result.put("success_count", successCount);

            ArrayNode tasksArray = result.putArray("tasks");
            for (JsonNode t : createdTasks) {
                tasksArray.add(t);
            }

            StringBuilder msg = new StringBuilder();
            msg.append("已为您创建 ").append(successCount).append(" 个提醒");
            if (!errors.isEmpty()) {
                msg.append("，").append(errors.size()).append(" 个失败");
            }
            msg.append("。");
            result.put("message", msg.toString());

            if (!errors.isEmpty()) {
                result.set("errors", objectMapper.valueToTree(errors));
            }

            return result.toString();

        } catch (Exception e) {
            log.error("执行计划失败 | args={} | error={}", argumentsJson, e.getMessage(), e);
            return "{\"error\": \"执行计划失败: " + e.getMessage().replace("\"", "'") + "\"}";
        }
    }
}