package com.youkeda.exercise.claw.agent.skill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.ai.llm.LLMResponse;
import com.youkeda.exercise.claw.agent.tool.FunctionExecutionContext;
import com.youkeda.exercise.claw.scout.ScoutTriggerPolicy;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class SkillToolFallbackPolicy {

    private static final ObjectMapper JSON = new ObjectMapper();

    public Optional<LLMResponse.ToolCall> createFallback(
            SkillRoutingResult routing,
            String currentMessage,
            FunctionExecutionContext executionContext,
            int toolCallCount) {
        if (routing == null
                || toolCallCount > 0
                || !"information-scout".equals(routing.primarySkill())
                || !ScoutTriggerPolicy.hasExplicitRequest(currentMessage)) {
            return Optional.empty();
        }

        String arguments = isProfileDiscovery(currentMessage)
                ? "{}"
                : topicArguments(currentMessage);
        return Optional.of(new LLMResponse.ToolCall(
                "runtime-scout-" + UUID.randomUUID(),
                "information_scout",
                arguments));
    }

    private boolean isProfileDiscovery(String message) {
        String normalized = message == null ? "" : message.replaceAll("\\s+", "");
        return normalized.matches(".*(?:有什么值得关注|有什么新动态|有什么新消息|有什么新资讯).*")
                || normalized.matches("(?:启动|运行|开启|调用)?信息猎手");
    }

    private String topicArguments(String message) {
        ObjectNode args = JSON.createObjectNode();
        args.put("query", message == null ? "" : message.trim());
        return args.toString();
    }
}
