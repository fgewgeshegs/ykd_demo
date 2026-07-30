package com.youkeda.exercise.claw.agent.skill;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillPendingCoordinatorTest {

    @Test
    void marksPendingWhenLlmAsksInsteadOfCallingScout() {
        SkillPendingCoordinator coordinator = new SkillPendingCoordinator();
        SkillSession session = SkillSession.create("owner")
                .withActiveSkill("information-scout");
        SkillRoutingResult routing = new SkillRoutingResult(
                "information-scout", Set.of(),
                SkillRoutingResult.SkillRoutingAction.ACTIVATE, 0.9,
                "scout explicit request");

        SkillSession updated = coordinator.afterDirectReply(session, routing);

        assertTrue(updated.hasPendingAction("START_INFORMATION_SCOUT"));
    }

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
