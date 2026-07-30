package com.youkeda.exercise.claw.campus.function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.runtime.ToolExecutionContext;
import com.youkeda.exercise.claw.agent.runtime.Tool;
import com.youkeda.exercise.claw.agent.runtime.ToolRegistry;
import com.youkeda.exercise.claw.campus.model.CampusConfig;
import com.youkeda.exercise.claw.campus.store.CampusConfigStore;
import com.youkeda.exercise.claw.campus.store.PendingAskStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "campus.enabled", havingValue = "true")
public class ExamReminderFunction implements Tool {

    private static final Logger log = LoggerFactory.getLogger(ExamReminderFunction.class);

    private final CampusConfigStore configStore;
    private final ToolRegistry functionRegistry;
    private final ObjectMapper objectMapper;
    private final PendingAskStore pendingAskStore;

    public ExamReminderFunction(CampusConfigStore configStore,
                                 ToolRegistry functionRegistry,
                                 ObjectMapper objectMapper,
                                 PendingAskStore pendingAskStore) {
        this.configStore = configStore;
        this.functionRegistry = functionRegistry;
        this.objectMapper = objectMapper;
        this.pendingAskStore = pendingAskStore;
    }

    @PostConstruct
    public void init() {
        functionRegistry.register(this);
        log.info("ExamReminderFunction 已注册到 ToolRegistry");
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
        actionProp.put("description", "操作类型：setup=首次设置, modify=修改, query=查询, disable=关闭, answer=回答是否需要提醒");
        actionProp.set("enum", objectMapper.createArrayNode()
            .add("setup").add("modify").add("query").add("disable").add("answer"));

        var noticeTypeProp = properties.putObject("noticeType");
        noticeTypeProp.put("type", "string");
        noticeTypeProp.put("description", "考试通知类型，如 FINAL_EXAM、CET、RETAKE 等（action=answer 时需要）");

        var answerProp = properties.putObject("answer");
        answerProp.put("type", "string");
        answerProp.put("description", "用户的回答：yes=需要提醒, no=不需要（action=answer 时需要）");

        root.set("required", objectMapper.createArrayNode().add("action"));

        return root;
    }

    @Override
    public String execute(String argumentsJson) {
        return execute(argumentsJson, null);
    }

    @Override
    public String execute(String argumentsJson, ToolExecutionContext context) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            String action = args.path("action").asText("query");

            return switch (action) {
                case "setup", "modify" -> handleSetup(args);
                case "query" -> handleQuery();
                case "disable" -> handleDisable();
                case "answer" -> handleAnswer(args);
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

    private String handleAnswer(JsonNode args) throws Exception {
        String noticeType = args.path("noticeType").asText("");
        String answer = args.path("answer").asText("");

        if (noticeType.isBlank() || answer.isBlank()) {
            return "{\"status\":\"ERROR\",\"message\":\"请提供 noticeType 和 answer 参数\"}";
        }

        pendingAskStore.updateAnswer(noticeType, answer);
        log.info("用户回答了考试提醒询问 | noticeType={} | answer={}", noticeType, answer);

        if ("yes".equals(answer)) {
            CampusConfig config = configStore.get();
            if (config != null) {
                List<String> currentTypes = new ArrayList<>(config.getPreferences().getAutoPushTypes());
                if (!currentTypes.contains(noticeType)) {
                    currentTypes.add(noticeType);
                    config.getPreferences().setAutoPushTypes(currentTypes);
                    configStore.save(config);
                }
            }
        }

        return objectMapper.writeValueAsString(Map.of(
            "status", "SUCCESS",
            "message", "已记录你的回答"
        ));
    }
}
