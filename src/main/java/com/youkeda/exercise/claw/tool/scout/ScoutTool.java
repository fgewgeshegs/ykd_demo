package com.youkeda.exercise.claw.tool.scout;
import com.youkeda.exercise.claw.feature.scout.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.runtime.ToolExecutionContext;
import com.youkeda.exercise.claw.agent.runtime.Tool;
import com.youkeda.exercise.claw.agent.runtime.ToolRegistry;
import com.youkeda.exercise.claw.skill.SkillsProperties;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "scout.enabled", havingValue = "true")
public class ScoutTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(ScoutTool.class);

    private final ToolRegistry functionRegistry;
    private final ObjectMapper objectMapper;
    private final ScoutSubmissionService submissionService;
    private final SkillsProperties skillsProperties;

    public ScoutTool(ToolRegistry functionRegistry,
                          ObjectMapper objectMapper,
                          ScoutSubmissionService submissionService,
                          SkillsProperties skillsProperties) {
        this.functionRegistry = functionRegistry;
        this.objectMapper = objectMapper;
        this.submissionService = submissionService;
        this.skillsProperties = skillsProperties;
    }

    @PostConstruct
    public void init() {
        functionRegistry.register(this);
        log.info("ScoutTool 已注册到 ToolRegistry");
    }

    @Override
    public String getName() {
        return "information_scout";
    }

    @Override
    public String getDescription() {
        return "创建\"信息猎手\"持续关注任务，后台定期发现高价值信息并推送。"
                + "仅当用户要建立持续关注关系时调用，例如「持续关注AI动态」「订阅科技资讯」「有消息通知我」。"
                + "用户只想当场查询（如「最近有什么新闻」「搜一下XX」）时严禁调用，应使用 web_search 即时回答。";
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
    public boolean isAvailable(ToolExecutionContext context) {
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
    public String getUnavailableReason(ToolExecutionContext context) {
        return "用户当前消息没有明确要求查找信息，禁止调用信息猎手。请直接回应用户当前内容。";
    }

    @Override
    public String execute(String argumentsJson, ToolExecutionContext context) {
        try {
            String query = extractQuery(argumentsJson, context);
            String workflowName = skillsProperties == null
                    ? null
                    : skillsProperties.getSkillWorkflowBindings().get("information-scout");
            ScoutSubmissionResult result = submissionService.submit(query, workflowName);
            return toJson(result);
        } catch (Exception e) {
            log.error("信息猎手执行失败", e);
            ObjectNode error = objectMapper.createObjectNode();
            error.put("status", "error");
            error.put("message", "信息猎手执行失败");
            return error.toString();
        }
    }

    private String toJson(ScoutSubmissionResult result) {
        ObjectNode response = objectMapper.createObjectNode();
        switch (result.status()) {
            case STARTED -> {
                response.put("status", "started");
                response.put("taskId", result.taskId());
            }
            case DUPLICATE -> response.put("status", "duplicate");
            case UNAVAILABLE, FAILED -> {
                response.put("status", "error");
                response.put("message", "信息猎手暂不可用");
            }
        }
        return response.toString();
    }

    private String extractQuery(String argumentsJson, ToolExecutionContext context) {
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
