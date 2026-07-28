package com.youkeda.exercise.claw.scout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.tool.FunctionExecutionContext;
import com.youkeda.exercise.claw.agent.tool.LLMFunction;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import com.youkeda.exercise.claw.wechat.user.WechatUserManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 信息猎手 LLM 工具入口
 *
 * 允许用户通过对话手动触发信息猎手
 */
@Component
@ConditionalOnProperty(name = "scout.enabled", havingValue = "true")
public class ScoutFunction implements LLMFunction {

    private static final Logger log = LoggerFactory.getLogger(ScoutFunction.class);

    private final ScoutOrchestrator orchestrator;
    private final LLMFunctionRegistry functionRegistry;
    private final ObjectMapper objectMapper;
    private final WechatUserManager userManager;

    public ScoutFunction(ScoutOrchestrator orchestrator,
                          LLMFunctionRegistry functionRegistry,
                          ObjectMapper objectMapper,
                          WechatUserManager userManager) {
        this.orchestrator = orchestrator;
        this.functionRegistry = functionRegistry;
        this.objectMapper = objectMapper;
        this.userManager = userManager;
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
        return "触发信息猎手 Agent，根据你的兴趣、项目和目标，主动发现高价值信息并推荐。"
                + "当用户说「帮我找找」「有什么新消息」「搜搜看」「今天有什么值得关注的」「信息猎手」时调用。";
    }

    @Override
    public JsonNode getParameters() {
        return objectMapper.createObjectNode()
                .put("type", "object")
                .set("properties", objectMapper.createObjectNode());
    }

    @Override
    public String execute(String argumentsJson) {
        // 无上下文版本，不应被直接调用
        return "{\"status\":\"ERROR\",\"message\":\"需要用户上下文\"}";
    }

    @Override
    public String execute(String argumentsJson, FunctionExecutionContext context) {
        try {
            log.info("手动触发信息猎手 | userId={}", userManager.getOwnerUserId());
            ScoutReport report = orchestrator.runForUser(userManager.getOwnerUserId());

            String result = objectMapper.writeValueAsString(Map.of(
                    "status", "SUCCESS",
                    "tasksGenerated", report.tasksGenerated(),
                    "itemsCollected", report.itemsCollected(),
                    "recommendations", report.recommendations(),
                    "message", report.recommendations() > 0
                            ? "信息猎手已发现 " + report.recommendations() + " 条与你相关的信息，请查看推送。"
                            : "信息猎手本轮未发现足够有价值的信息，下次再试试。"
            ));

            return result;
        } catch (Exception e) {
            log.error("信息猎手执行失败 | userId={}", userManager.getOwnerUserId(), e);
            return "{\"status\":\"ERROR\",\"message\":\"信息猎手执行失败: " + e.getMessage() + "\"}";
        }
    }
}
