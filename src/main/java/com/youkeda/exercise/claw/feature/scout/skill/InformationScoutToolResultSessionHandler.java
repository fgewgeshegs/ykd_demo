package com.youkeda.exercise.claw.feature.scout.skill;

import com.youkeda.exercise.claw.agent.skill.SkillSession;
import com.youkeda.exercise.claw.agent.skill.ToolResultSessionHandler;
import org.springframework.stereotype.Component;

@Component
public class InformationScoutToolResultSessionHandler
        implements ToolResultSessionHandler {

    @Override
    public String skillName() {
        return "information-scout";
    }

    @Override
    public String toolName() {
        return "information_scout";
    }

    @Override
    public SkillSession afterExecution(
            SkillSession session,
            String toolName,
            String toolResult) {
        return session.clearPendingAction();
    }
}
