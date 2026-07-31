package com.youkeda.exercise.claw.agent.skill;

import org.springframework.stereotype.Component;

@Component
public class SkillPendingCoordinator {

    public static final String START_INFORMATION_SCOUT = "START_INFORMATION_SCOUT";

    public SkillSession afterToolExecution(SkillSession session, String toolName) {
        if (session == null) return null;
        if ("information_scout".equals(toolName)) {
            return session.clearPendingAction();
        }
        return session;
    }
}
