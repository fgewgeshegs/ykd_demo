package com.youkeda.exercise.claw.scout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.scout.ScoutTaskManager;
import com.youkeda.exercise.claw.agent.skill.WorkflowRegistry;
import com.youkeda.exercise.claw.agent.skill.WorkflowRequest;
import com.youkeda.exercise.claw.agent.skill.WorkflowWorker;
import com.youkeda.exercise.claw.agent.tool.FunctionExecutionContext;
import com.youkeda.exercise.claw.agent.tool.LLMFunction;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
@ConditionalOnProperty(name = "scout.enabled", havingValue = "true")
public class ScoutFunction implements LLMFunction {

    private static final Logger log = LoggerFactory.getLogger(ScoutFunction.class);

    private final ScoutOrchestrator orchestrator;
    private final LLMFunctionRegistry functionRegistry;
    private final ObjectMapper objectMapper;
    private final ScoutTaskManager taskManager;
    private final WorkflowRegistry workflowRegistry;

    public ScoutFunction(ScoutOrchestrator orchestrator,
                          LLMFunctionRegistry functionRegistry,
                          ObjectMapper objectMapper,
                          ScoutTaskManager taskManager,
                          WorkflowRegistry workflowRegistry) {
        this.orchestrator = orchestrator;
        this.functionRegistry = functionRegistry;
        this.objectMapper = objectMapper;
        this.taskManager = taskManager;
        this.workflowRegistry = workflowRegistry;
    }

    @PostConstruct
    public void init() {
        functionRegistry.register(this);
        log.info("ScoutFunction 已注册到 LLMFunctionRegistry");
    }

    @Override
    public String getName() {
        return "information_scout";
    }

    @Override
    public String getDescription() {
        return "触发信息猎手 Agent，根据用户兴趣、项目和目标主动发现高价值信息并推荐。"
                + "仅当当前用户消息明确要求查找信息时调用，例如「帮我找找」「有什么新消息」"
                + "「搜搜看」「今天有什么值得关注的」「启动信息猎手」。"
                + "用户只是在介绍兴趣、回答问题或补充情况时严禁调用；不得从历史对话推断触发意图。";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("type", "object");
        ObjectNode properties = params.putObject("properties");
        ObjectNode query = properties.putObject("query");
        query.put("type", "string");
        query.put("description", "指定主题模式下的搜索主题；画像发现模式必须省略此字段");
        return params;
    }

    @Override
    public boolean isAvailable(FunctionExecutionContext context) {
        if (context == null || ScoutTriggerPolicy.isCancellation(context.currentMessage())) {
            return false;
        }
        if (ScoutTriggerPolicy.hasExplicitRequest(context.currentMessage())) {
            return true;
        }
        return context.skillSession() != null
                && "information-scout".equals(context.skillSession().activeSkill())
                && context.skillSession().hasPendingAction("START_INFORMATION_SCOUT");
    }

    @Override
    public String execute(String argumentsJson) {
        return "{\"status\":\"ERROR\",\"message\":\"需要用户上下文\"}";
    }

    @Override
    public String getUnavailableReason(FunctionExecutionContext context) {
        return "用户当前消息没有明确要求查找信息，禁止调用信息猎手。请直接回应用户当前内容。";
    }

    @Override
    public String execute(String argumentsJson, FunctionExecutionContext context) {
        try {
            String query = extractQuery(argumentsJson, context);

            // Check for duplicate running tasks
            if (taskManager.isDuplicate()) {
                return "{\"status\":\"duplicate\",\"message\":\"已有正在运行的信息猎手任务，请等待完成后再试\"}";
            }

            // Get workflow worker
            Optional<WorkflowWorker> worker = workflowRegistry.getWorker("scoutWorkflow");
            if (worker.isEmpty()) {
                return "{\"status\":\"error\",\"message\":\"信息猎手暂不可用\"}";
            }

            // Create task and launch workflow async
            String taskId = UUID.randomUUID().toString();
            taskManager.createTask(taskId, query);

            CompletableFuture.runAsync(() -> {
                try {
                    WorkflowRequest wfRequest = new WorkflowRequest(
                            taskId, "scoutWorkflow", query, Instant.now());
                    worker.get().execute(wfRequest);
                } catch (Exception e) {
                    log.error("Async scout workflow failed", e);
                }
            });

            String queryDisplay = (query != null && !query.isBlank()) ? "「" + query + "」" : "";
            return String.format(
                "{\"status\":\"started\",\"taskId\":\"%s\",\"message\":\"已开始查找%s，完成后会推送结果给你\"}",
                taskId, queryDisplay);

        } catch (Exception e) {
            log.error("信息猎手执行失败", e);
            return "{\"status\":\"ERROR\",\"message\":\"信息猎手执行失败: " + e.getMessage() + "\"}";
        }
    }

    private String extractQuery(String argumentsJson, FunctionExecutionContext context) {
        if (argumentsJson != null && !argumentsJson.isBlank()) {
            try {
                JsonNode args = objectMapper.readTree(argumentsJson);
                if (args.has("query") && !args.get("query").asText().isBlank()) {
                    return args.get("query").asText();
                }
                return "";
            } catch (Exception ignored) {}
        }
        if (context != null && context.currentMessage() != null) {
            return context.currentMessage();
        }
        return "";
    }
}
