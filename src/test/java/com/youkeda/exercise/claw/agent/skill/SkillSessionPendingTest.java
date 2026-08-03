package com.youkeda.exercise.claw.agent.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkillSessionPendingTest {

    @Test
    void pendingActionSurvivesNormalSessionUpdatesUntilCleared() {
        SkillSession pending = SkillSession.create("owner")
                .withActiveSkill("information-scout")
                .withPendingAction("START_INFORMATION_SCOUT", "query");

        SkillSession updated = pending.withResetInactivity();

        assertTrue(updated.hasPendingAction("START_INFORMATION_SCOUT"));
        assertEquals("query", updated.pendingSlot());
        assertFalse(updated.clearPendingAction().hasPendingAction("START_INFORMATION_SCOUT"));
    }

    @Test
    void pendingActionIsSuspendedAndRestoredAcrossSkillSwitch() {
        SkillSession travel = SkillSession.create("owner")
                .withActiveSkill("travel")
                .withPendingAction("COLLECT_TRAVEL_REQUIREMENTS", "budget");

        SkillSession weather = travel.withActiveSkill("weather");
        SkillSession resumed = weather.withActiveSkill("travel");

        assertFalse(weather.hasPendingAction("COLLECT_TRAVEL_REQUIREMENTS"));
        assertTrue(weather.hasSuspendedPendingAction(
                "travel", "COLLECT_TRAVEL_REQUIREMENTS"));
        assertTrue(resumed.hasPendingAction("COLLECT_TRAVEL_REQUIREMENTS"));
        assertEquals("budget", resumed.pendingSlot());
    }
}
