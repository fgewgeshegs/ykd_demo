package com.youkeda.exercise.claw.agent.scout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.tool.FunctionExecutionContext;
import com.youkeda.exercise.claw.agent.tool.LLMFunction;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ScoutTaskManageFunction implements LLMFunction {

    private static final Logger log = LoggerFactory.getLogger(ScoutTaskManageFunction.class);

    private final LLMFunctionRegistry registry;
    private final ScoutTaskManager taskManager;
    private final ObjectMapper objectMapper;

    public ScoutTaskManageFunction(LLMFunctionRegistry registry,
                                   ScoutTaskManager taskManager,
                                   ObjectMapper objectMapper) {
        this.registry = registry;
        this.taskManager = taskManager;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        registry.register(this);
    }

    @Override
    public String getName() {
        return "scout_task_manage";
    }

    @Override
    public String getDescription() {
        return "管理信息猎手任务：查看状态、取消、重试、列出所有任务";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("type", "object");
        ObjectNode properties = params.putObject("properties");
        ObjectNode action = properties.putObject("action");
        action.put("type", "string");
        action.putArray("enum").add("status").add("cancel").add("list").add("retry");
        action.put("description", "操作类型");
        ObjectNode taskId = properties.putObject("taskId");
        taskId.put("type", "string");
        taskId.put("description", "任务ID（status/cancel/retry时需要）");
        ObjectNode userId = properties.putObject("userId");
        userId.put("type", "string");
        userId.put("description", "用户ID（list时需要）");
        params.putArray("required").add("action");
        return params;
    }

    @Override
    public String execute(String argumentsJson, FunctionExecutionContext context) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            String action = args.get("action").asText();
            return switch (action) {
                case "status" -> handleStatus(args);
                case "cancel" -> handleCancel(args);
                case "list" -> handleList(args);
                case "retry" -> "{\"status\":\"retry not yet implemented\"}";
                default -> "{\"error\":\"unknown action: " + action + "\"}";
            };
        } catch (Exception e) {
            log.error("scout_task_manage failed", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    private String handleStatus(JsonNode args) {
        if (!args.has("taskId")) return "{\"error\":\"taskId is required\"}";
        return taskManager.getTask(args.get("taskId").asText())
                .map(t -> String.format(
                    "{\"taskId\":\"%s\",\"status\":\"%s\",\"query\":\"%s\",\"summary\":\"%s\"}",
                    t.taskId(), t.status().name(),
                    t.query() != null ? t.query() : "",
                    t.summary() != null ? t.summary() : ""))
                .orElse("{\"error\":\"task not found\"}");
    }

    private String handleCancel(JsonNode args) {
        if (!args.has("taskId")) return "{\"error\":\"taskId is required\"}";
        return taskManager.cancelTask(args.get("taskId").asText())
                ? "{\"status\":\"cancelled\"}"
                : "{\"error\":\"task not found or not running\"}";
    }

    private String handleList(JsonNode args) {
        String userId = args.has("userId") ? args.get("userId").asText() : "default";
        List<ScoutTask> tasks = taskManager.listTasks(userId);
        String tasksJson = tasks.stream()
                .map(t -> String.format(
                    "{\"taskId\":\"%s\",\"query\":\"%s\",\"status\":\"%s\"}",
                    t.taskId(), t.query() != null ? t.query() : "", t.status().name()))
                .collect(Collectors.joining(",", "[", "]"));
        return "{\"tasks\":" + tasksJson + "}";
    }

    @Override
    public String execute(String argumentsJson) {
        return execute(argumentsJson, null);
    }
}
