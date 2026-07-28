package com.youkeda.exercise.claw.campus.function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.tool.FunctionExecutionContext;
import com.youkeda.exercise.claw.agent.tool.LLMFunction;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import com.youkeda.exercise.claw.campus.model.CampusConfig;
import com.youkeda.exercise.claw.campus.store.CampusConfigStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "campus.enabled", havingValue = "true")
public class ExamReminderFunction implements LLMFunction {

    private static final Logger log = LoggerFactory.getLogger(ExamReminderFunction.class);

    private final CampusConfigStore configStore;
    private final LLMFunctionRegistry functionRegistry;
    private final ObjectMapper objectMapper;

    public ExamReminderFunction(CampusConfigStore configStore,
                                 LLMFunctionRegistry functionRegistry,
                                 ObjectMapper objectMapper) {
        this.configStore = configStore;
        this.functionRegistry = functionRegistry;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        functionRegistry.register(this);
        log.info("ExamReminderFunction 已注册到 LLMFunctionRegistry");
    }

    @Override
    public String getName() {
        return "exam_reminder_setup";
    }

    @Override
    public String getDescription() {
        return "设置/修改考试提醒。包括：配置学校、班级、推送偏好。"
            + "当用户说'设置考试提醒'、'我是X学校X班的'、"
            + "'帮我关注考试通知'、'补考也提醒我'、'考试设置'时调用。";
    }

    @Override
    public JsonNode getParameters() {
        var root = objectMapper.createObjectNode();
        root.put("type", "object");

        var properties = root.putObject("properties");

        var schoolProp = properties.putObject("school");
        schoolProp.put("type", "string");
        schoolProp.put("description", "学校名称，如'南京邮电大学'");

        var classProp = properties.putObject("className");
        classProp.put("type", "string");
        classProp.put("description", "班级号，如'B250405'");

        var actionProp = properties.putObject("action");
        actionProp.put("type", "string");
        actionProp.put("description", "操作类型：setup=首次设置, modify=修改, query=查询, disable=关闭");
        actionProp.set("enum", objectMapper.createArrayNode()
            .add("setup").add("modify").add("query").add("disable"));

        root.set("required", objectMapper.createArrayNode().add("action"));

        return root;
    }

    @Override
    public String execute(String argumentsJson) {
        return "{\"status\":\"ERROR\",\"message\":\"需要用户上下文\"}";
    }

    @Override
    public String execute(String argumentsJson, FunctionExecutionContext context) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            String action = args.path("action").asText("query");

            return switch (action) {
                case "setup", "modify" -> handleSetup(args);
                case "query" -> handleQuery();
                case "disable" -> handleDisable();
                default -> "{\"status\":\"ERROR\",\"message\":\"未知操作: " + action + "\"}";
            };

        } catch (Exception e) {
            log.error("ExamReminderFunction 执行失败", e);
            return "{\"status\":\"ERROR\",\"message\":\"执行失败: " + e.getMessage() + "\"}";
        }
    }

    private String handleSetup(JsonNode args) throws Exception {
        String school = args.path("school").asText("");
        String className = args.path("className").asText("");

        if (school.isBlank()) {
            return "{\"status\":\"ERROR\",\"message\":\"请提供学校名称\"}";
        }

        CampusConfig config = new CampusConfig();
        config.setSchool(school);
        config.setClassName(className);
        config.setEnabled(true);
        configStore.save(config);

        String result = objectMapper.writeValueAsString(Map.of(
            "status", "SUCCESS",
            "message", "已设置考试提醒！学校: " + school
                + (className.isBlank() ? "" : ", 班级: " + className)
                + "。每天08:00自动检查教务处通知，期末考试和四六级通知会直接推送给你。"
        ));
        log.info("考试提醒设置成功 | school={} | class={}", school, className);
        return result;
    }

    private String handleQuery() throws Exception {
        CampusConfig config = configStore.get();
        if (config == null) {
            return "{\"status\":\"SUCCESS\",\"message\":\"尚未设置考试提醒。你可以说'我是南邮B250405班的'来设置。\"}";
        }

        return objectMapper.writeValueAsString(Map.of(
            "status", "SUCCESS",
            "school", config.getSchool(),
            "className", config.getClassName(),
            "enabled", config.isEnabled(),
            "autoPushTypes", config.getPreferences().getAutoPushTypes()
        ));
    }

    private String handleDisable() throws Exception {
        CampusConfig config = configStore.get();
        if (config != null) {
            config.setEnabled(false);
            configStore.save(config);
        }
        return "{\"status\":\"SUCCESS\",\"message\":\"已关闭考试提醒。再次发送'设置考试提醒'重新开启。\"}";
    }
}
