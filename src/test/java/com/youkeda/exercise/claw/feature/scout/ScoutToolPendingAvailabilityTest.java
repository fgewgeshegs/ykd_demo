package com.youkeda.exercise.claw.feature.scout;
import com.youkeda.exercise.claw.tool.scout.ScoutTool;

import com.youkeda.exercise.claw.agent.skill.SkillSession;
import com.youkeda.exercise.claw.agent.runtime.ToolExecutionContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoutToolPendingAvailabilityTest {

    private final ScoutTool function = new ScoutTool(
            null, null, null, null);

    @Test
    void shortSlotAnswerIsAvailableOnlyForPendingScoutAction() {
        SkillSession pending = SkillSession.create("owner")
                .withActiveSkill("information-scout")
                .withPendingAction("START_INFORMATION_SCOUT", "query");

        assertTrue(function.isAvailable(new ToolExecutionContext("AI / 深度学习", pending)));
        assertFalse(function.isAvailable(new ToolExecutionContext(
                "AI / 深度学习", SkillSession.create("owner"))));
    }

    @Test
    void cancellationDoesNotUsePendingAuthorization() {
        SkillSession pending = SkillSession.create("owner")
                .withActiveSkill("information-scout")
                .withPendingAction("START_INFORMATION_SCOUT", "query");

        assertFalse(function.isAvailable(new ToolExecutionContext("不要查了", pending)));
    }
}
