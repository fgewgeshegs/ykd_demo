package com.youkeda.exercise.claw.agent.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class SkillPendingCoordinatorTest {


    @Test
    void clearsPendingAfterScoutToolExecutes() {
        SkillPendingCoordinator coordinator = new SkillPendingCoordinator();
        SkillSession session = SkillSession.create("owner")
                .withActiveSkill("information-scout")
                .withPendingAction("START_INFORMATION_SCOUT", "query");

        SkillSession updated = coordinator.afterToolExecution(session, "information_scout");

        assertFalse(updated.hasPendingAction("START_INFORMATION_SCOUT"));
    }
}
