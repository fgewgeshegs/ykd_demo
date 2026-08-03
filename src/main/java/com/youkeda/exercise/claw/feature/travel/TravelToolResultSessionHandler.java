package com.youkeda.exercise.claw.feature.travel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.skill.SkillPendingCoordinator;
import com.youkeda.exercise.claw.agent.skill.SkillSession;
import com.youkeda.exercise.claw.agent.skill.ToolResultSessionHandler;
import org.springframework.stereotype.Component;

@Component
public class TravelToolResultSessionHandler implements ToolResultSessionHandler {

    private final ObjectMapper objectMapper;

    public TravelToolResultSessionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String skillName() {
        return "travel";
    }

    @Override
    public String toolName() {
        return "travel_collect";
    }

    @Override
    public SkillSession afterExecution(
            SkillSession session,
            String toolName,
            String toolResult) {
        if (toolResult == null || toolResult.isBlank()) return session;
        try {
            JsonNode result = objectMapper.readTree(toolResult);
            String status = result.path("status").asText("");
            if ("NEED_MORE_INFORMATION".equals(status)
                    || "ALL_COLLECTED".equals(status)) {
                session = session.withoutContextValue(
                        SkillPendingCoordinator.NEW_TRAVEL_PLAN);
            }
            if ("ALL_COLLECTED".equals(status)) {
                return session.hasPendingAction(
                        SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS)
                        ? session.clearPendingAction()
                        : session;
            }
            if ("NEED_MORE_INFORMATION".equals(status)) {
                JsonNode missingFields = result.path("missing_fields");
                String slot = missingFields.isArray() && !missingFields.isEmpty()
                        ? missingFields.get(0).asText()
                        : null;
                return session.withPendingAction(
                        SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS,
                        slot);
            }
        } catch (Exception ignored) {
            // Invalid tool output must not corrupt the existing session.
        }
        return session;
    }
}
