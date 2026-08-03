package com.youkeda.exercise.claw.agent.skill;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.feature.scout.skill.InformationScoutToolResultSessionHandler;
import com.youkeda.exercise.claw.feature.travel.TravelToolResultSessionHandler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkillPendingCoordinatorTest {

    @Test
    void delegatesToolResultsToRegisteredSessionHandler() {
        ToolResultSessionHandler handler = mock(ToolResultSessionHandler.class);
        SkillSession session = SkillSession.create("owner").withActiveSkill("custom");
        SkillSession updated = session.withPendingAction("CUSTOM_PENDING", "slot");
        when(handler.supports("custom", "custom_tool")).thenReturn(true);
        when(handler.afterExecution(session, "custom_tool", "result"))
                .thenReturn(updated);
        SkillPendingCoordinator coordinator =
                new SkillPendingCoordinator(List.of(handler));

        SkillSession result = coordinator.afterToolExecution(
                session, "custom_tool", "result");

        assertSame(updated, result);
    }


    @Test
    void clearsPendingAfterScoutToolExecutes() {
        SkillPendingCoordinator coordinator = coordinator();
        SkillSession session = SkillSession.create("owner")
                .withActiveSkill("information-scout")
                .withPendingAction("START_INFORMATION_SCOUT", "query");

        SkillSession updated = coordinator.afterToolExecution(session, "information_scout");

        assertFalse(updated.hasPendingAction("START_INFORMATION_SCOUT"));
    }

    @Test
    void recordsFirstMissingTravelSlotAfterCollect() {
        SkillPendingCoordinator coordinator = coordinator();
        SkillSession session = SkillSession.create("owner").withActiveSkill("travel");

        SkillSession updated = coordinator.afterToolExecution(
                session,
                "travel_collect",
                "{\"status\":\"NEED_MORE_INFORMATION\","
                        + "\"missing_fields\":[\"participant_count\",\"budget\"]}");

        assertTrue(updated.hasPendingAction(
                SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS));
        assertEquals("participant_count", updated.pendingSlot());
    }

    @Test
    void clearsTravelPendingActionWhenCollectionCompletes() {
        SkillPendingCoordinator coordinator = coordinator();
        SkillSession session = SkillSession.create("owner")
                .withActiveSkill("travel")
                .withPendingAction(
                        SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS,
                        "budget")
                .withContextValue(SkillPendingCoordinator.NEW_TRAVEL_PLAN, "true");

        SkillSession updated = coordinator.afterToolExecution(
                session,
                "travel_collect",
                "{\"status\":\"ALL_COLLECTED\"}");

        assertFalse(updated.hasPendingAction(
                SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS));
        assertFalse(updated.context().containsKey(
                SkillPendingCoordinator.NEW_TRAVEL_PLAN));
    }

    @Test
    void keepsNewPlanMarkerWhenTravelArgumentsAreInvalid() {
        SkillPendingCoordinator coordinator = coordinator();
        SkillSession session = SkillSession.create("owner")
                .withActiveSkill("travel")
                .withContextValue(SkillPendingCoordinator.NEW_TRAVEL_PLAN, "true")
                .withPendingAction(
                        SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS,
                        "initial_request");

        SkillSession updated = coordinator.afterToolExecution(
                session, "travel_collect",
                "{\"status\":\"INVALID_ARGUMENT\",\"error\":\"参数错误\"}");

        assertTrue("true".equals(updated.context().get(
                SkillPendingCoordinator.NEW_TRAVEL_PLAN)));
        assertTrue(updated.hasPendingAction(
                SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS));
    }

    @Test
    void keepsNewPlanMarkerWhenTravelCollectionFails() {
        SkillPendingCoordinator coordinator = coordinator();
        SkillSession session = SkillSession.create("owner")
                .withActiveSkill("travel")
                .withContextValue(SkillPendingCoordinator.NEW_TRAVEL_PLAN, "true")
                .withPendingAction(
                        SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS,
                        "initial_request");

        SkillSession updated = coordinator.afterToolExecution(
                session, "travel_collect",
                "{\"status\":\"ERROR\",\"error\":\"暂时失败\"}");

        assertTrue("true".equals(updated.context().get(
                SkillPendingCoordinator.NEW_TRAVEL_PLAN)));
        assertTrue(updated.hasPendingAction(
                SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS));
    }

    private SkillPendingCoordinator coordinator() {
        return new SkillPendingCoordinator(List.of(
                new InformationScoutToolResultSessionHandler(),
                new TravelToolResultSessionHandler(new ObjectMapper())));
    }
}
