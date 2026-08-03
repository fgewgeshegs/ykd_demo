package com.youkeda.exercise.claw.agent.skill;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SkillPendingCoordinator {

    public static final String START_INFORMATION_SCOUT = "START_INFORMATION_SCOUT";
    public static final String COLLECT_TRAVEL_REQUIREMENTS = "COLLECT_TRAVEL_REQUIREMENTS";
    public static final String NEW_TRAVEL_PLAN = "travelNewPlan";

    private final List<ToolResultSessionHandler> handlers;

    public SkillPendingCoordinator(List<ToolResultSessionHandler> handlers) {
        this.handlers = handlers == null ? List.of() : List.copyOf(handlers);
    }

    public SkillSession afterToolExecution(SkillSession session, String toolName) {
        return afterToolExecution(session, toolName, null);
    }

    public SkillSession afterToolExecution(
            SkillSession session, String toolName, String toolResult) {
        if (session == null) return null;
        SkillSession updated = session;
        for (ToolResultSessionHandler handler : handlers) {
            if (handler.supports(updated.activeSkill(), toolName)) {
                updated = handler.afterExecution(updated, toolName, toolResult);
            }
        }
        return updated;
    }
}
