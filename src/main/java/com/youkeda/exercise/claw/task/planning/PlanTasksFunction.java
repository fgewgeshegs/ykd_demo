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
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 任务规划 LLM Function
 *
 * <p>注册名称：{@code plan_tasks}
 *
 * <p>将用户的高层目标拆解为多个有顺序的定时任务计划。
 * 默认仅生成预览（mode=preview），不实际创建定时任务。
 * 用户确认后，由 {@link ExecutePlanTasksFunction} 执行。
 *
 * <p>流程：
 * <pre>
 * 用户目标 → LLM 拆解 → plan_tasks(mode=preview) → 保存 TaskPlan(PREVIEW)
 *      → 展示给用户确认 → execute_plan_tasks(plan_id) → 批量创建 ScheduledTask
 * </pre>
 */
@Component
public class PlanTasksFunction implements LLMFunction {

    private static final Logger log = LoggerFactory.getLogger(PlanTasksFunction.class);

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper;
    private final LLMFunctionRegistry functionRegistry;
    private final TaskPlanRepository planRepository;

    public PlanTasksFunction(ObjectMapper objectMapper,
                             LLMFunctionRegistry functionRegistry,
                             TaskPlanRepository planRepository) {
        this.objectMapper = objectMapper;
        this.functionRegistry = functionRegistry;
        this.planRepository = planRepository;
    }

    @PostConstruct
    public void init() {
        functionRegistry.register(this);
        log.info("PlanTasksFunction 已注册到 LLMFunctionRegistry");
    }

    @Override
    public String getName() {
        return "plan_tasks";
    }

    @Override
    public String getDescription() {
        return "规划定时任务。当用户提出需要拆解为多个步骤的目标时调用此函数。\n"
                + "例如：「帮我准备软件杯比赛」「安排今晚的学习计划」「帮忙规划出差准备」。\n\n"
                + "使用方式：\n"
                + "1. 先调用 time_query 获取当前时间作为参考\n"
                + "2. 将用户目标拆解为多个有顺序的子任务\n"
                + "3. 调用此函数传入 goal（目标）和 tasks（任务列表）\n\n"
                + "每个任务需要指定：\n"
                + "- order: 序号（从1开始）\n"
                + "- content: 提醒内容\n"
                + "- delay_minutes 或 execute_time: 时间（参照已获取的当前时间推算）\n"
                + "- repeat_type: ONCE/DAILY/WEEKLY（可选，默认ONCE）\n\n"
                + "此函数默认只生成预览，不创建实际提醒。用户确认后需调用 execute_plan_tasks。";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("type", "object");

        ObjectNode properties = params.putObject("properties");

        // goal — 目标描述
        ObjectNode goal = properties.putObject("goal");
        goal.put("type", "string");
        goal.put("description", "用户的高层目标描述，如「准备软件杯比赛」「今晚的学习计划」。");

        // tasks — 任务数组
        ObjectNode tasks = properties.putObject("tasks");
        tasks.put("type", "array");
        tasks.put("description", "拆解后的任务列表（按顺序），至少 1 个，最多 10 个。");

        ObjectNode taskItems = tasks.putObject("items");
        taskItems.put("type", "object");
        ObjectNode itemProps = taskItems.putObject("properties");

        ObjectNode order = itemProps.putObject("order");
        order.put("type", "integer");
        order.put("description", "任务序号，从 1 开始递增。");

        ObjectNode content = itemProps.putObject("content");
        content.put("type", "string");
        content.put("description", "任务内容，简洁明确。");

        ObjectNode delay = itemProps.putObject("delay_minutes");
        delay.put("type", "integer");
        delay.put("description", "【相对时间】从当前起多少分钟后执行此任务，与 execute_time 二选一。");

        ObjectNode execTime = itemProps.putObject("execute_time");
        execTime.put("type", "string");
        execTime.put("description", "【绝对时间】执行时间，格式 yyyy-MM-dd HH:mm:ss，与 delay_minutes 二选一。");

        ObjectNode repeatType = itemProps.putObject("repeat_type");
        repeatType.put("type", "string");
        repeatType.put("description", "【可选，默认ONCE】周期类型。ONCE=一次性, DAILY=每天, WEEKLY=每周。");
        repeatType.putArray("enum").add("NONE").add("ONCE").add("DAILY").add("WEEKLY").add("MONTHLY");

        ArrayNode taskRequired = taskItems.putArray("required");
        taskRequired.add("order");
        taskRequired.add("content");

        // required
        params.putArray("required").add("goal").add("tasks");

        return params;
    }

    @Override
    public String execute(String argumentsJson) {
        return "{\"error\": \"缺少用户上下文，无法创建任务计划\"}";
    }

    @Override
    public String execute(String argumentsJson, FunctionExecutionContext context) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            String userId = context.userId();
            if (userId == null || userId.isBlank()) {
                return "{\"error\": \"缺少用户ID\"}";
            }

            // 解析参数
            String goal = args.has("goal") ? args.get("goal").asText().strip() : "";
            if (goal.isEmpty()) {
                return "{\"error\": \"缺少目标描述(goal)\"}";
            }

            JsonNode tasks = args.get("tasks");
            if (tasks == null || !tasks.isArray() || tasks.isEmpty()) {
                return "{\"error\": \"缺少任务列表(tasks)\"}";
            }
            if (tasks.size() > 10) {
                return "{\"error\": \"任务数量不能超过 10 个\"}";
            }

            // 校验每个任务
            for (JsonNode task : tasks) {
                if (!task.has("content") || task.get("content").asText().isBlank()) {
                    return "{\"error\": \"任务缺少 content 字段\"}";
                }
                boolean hasDelay = task.has("delay_minutes") && task.get("delay_minutes").canConvertToExactIntegral();
                boolean hasTime = task.has("execute_time") && !task.get("execute_time").asText().isBlank();
                if (!hasDelay && !hasTime) {
                    int order = task.has("order") ? task.get("order").asInt() : 0;
                    return "{\"error\": \"任务 " + order + " 缺少时间参数（delay_minutes 或 execute_time）\"}";
                }
            }

            // 保存计划
            String tasksJson = objectMapper.writeValueAsString(tasks);
            TaskPlan plan = new TaskPlan(userId, goal, tasksJson);
            planRepository.save(plan);

            log.info("任务计划已创建 | id={} | userId={} | goal={} | taskCount={}",
                    plan.getId(), userId, goal, tasks.size());

            // 构建预览回复
            StringBuilder sb = new StringBuilder();
            sb.append("已为您规划「").append(goal).append("」").append(tasks.size()).append("个步骤：\n\n");
            for (JsonNode task : tasks) {
                int order = task.has("order") ? task.get("order").asInt() : 0;
                String content = task.get("content").asText();
                String timeInfo = buildTimeInfo(task);
                String repeatInfo = buildRepeatInfo(task);
                sb.append("  ").append(order).append(". ").append(content)
                        .append(" — ").append(timeInfo).append(repeatInfo).append("\n");
            }
            sb.append("\n如需执行请回复确认，我将批量创建这些提醒。");

            // 构建返回
            ObjectNode result = objectMapper.createObjectNode();
            result.put("status", "preview");
            result.put("plan_id", plan.getId());
            result.put("goal", goal);
            result.put("task_count", tasks.size());
            result.put("message", sb.toString());

            return result.toString();

        } catch (Exception e) {
            log.error("创建任务计划失败 | args={} | error={}", argumentsJson, e.getMessage(), e);
            return "{\"error\": \"创建任务计划失败: " + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    private String buildTimeInfo(JsonNode task) {
        if (task.has("delay_minutes")) {
            int min = task.get("delay_minutes").asInt();
            if (min < 60) return min + "分钟后";
            return (min / 60) + "小时" + (min % 60 > 0 ? min % 60 + "分" : "") + "后";
        }
        if (task.has("execute_time")) {
            return "于 " + task.get("execute_time").asText();
        }
        return "";
    }

    private String buildRepeatInfo(JsonNode task) {
        if (task.has("repeat_type")) {
            String rt = task.get("repeat_type").asText().toUpperCase();
            return switch (rt) {
                case "DAILY" -> "（每天）";
                case "WEEKLY" -> "（每周）";
                case "MONTHLY" -> "（每月）";
                default -> "";
            };
        }
        return "";
    }
}