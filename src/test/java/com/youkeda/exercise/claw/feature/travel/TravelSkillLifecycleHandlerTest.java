package com.youkeda.exercise.claw.feature.travel;

import com.youkeda.exercise.claw.agent.skill.SkillPendingCoordinator;
import com.youkeda.exercise.claw.agent.skill.SkillRoutingResult;
import com.youkeda.exercise.claw.agent.skill.SkillSession;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelSkillLifecycleHandlerTest {

    private final TravelSkillLifecycleHandler handler = new TravelSkillLifecycleHandler();

    @Test
    void marksNewPlanWithoutDeletingDraftBeforeToolSucceeds() {
        SkillSession session = SkillSession.create("owner").withActiveSkill("travel");

        SkillSession updated = handler.onRouting(
                "帮我规划新疆三日游",
                routing(SkillRoutingResult.SkillRoutingAction.ACTIVATE),
                session);

        assertFalse(updated.context().get(
                SkillPendingCoordinator.NEW_TRAVEL_PLAN).isBlank());
        assertTrue(updated.hasPendingAction(
                SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS));
    }

    @Test
    void marksAnotherFullTripRequestWhileTravelIsAlreadyActive() {
        SkillSession session = SkillSession.create("owner").withActiveSkill("travel");

        SkillSession updated = handler.onRouting(
                "帮我规划北京三日游",
                routing(SkillRoutingResult.SkillRoutingAction.CONTINUE),
                session);

        assertTrue(updated.context().containsKey(
                SkillPendingCoordinator.NEW_TRAVEL_PLAN));
    }

    @Test
    void repeatedDeliveryUsesStableMessageIdForNewPlanMarker() {
        SkillSession session = SkillSession.create("owner").withActiveSkill("travel");

        SkillSession first = handler.onRouting(
                "帮我规划北京三日游",
                routing(SkillRoutingResult.SkillRoutingAction.CONTINUE),
                session,
                "message-1");
        SkillSession duplicate = handler.onRouting(
                "帮我规划北京三日游",
                routing(SkillRoutingResult.SkillRoutingAction.CONTINUE),
                session,
                "message-1");

        assertEquals(
                first.context().get(SkillPendingCoordinator.NEW_TRAVEL_PLAN),
                duplicate.context().get(SkillPendingCoordinator.NEW_TRAVEL_PLAN));
    }

    @Test
    void marksDestinationVisitRequestAsNewPlan() {
        SkillSession session = SkillSession.create("owner").withActiveSkill("common");

        SkillSession updated = handler.onRouting(
                "8月去青岛玩三天",
                routing(SkillRoutingResult.SkillRoutingAction.ACTIVATE),
                session.withActiveSkill("travel"));

        assertTrue(updated.context().containsKey(
                SkillPendingCoordinator.NEW_TRAVEL_PLAN));
    }

    @Test
    void doesNotMarkResumeOrRevisionAsNewPlan() {
        SkillSession session = SkillSession.create("owner").withActiveSkill("travel");

        SkillSession resumed = handler.onRouting(
                "继续刚才的旅行",
                routing(SkillRoutingResult.SkillRoutingAction.SWITCH),
                session);
        SkillSession revised = handler.onRouting(
                "把行程改成三天",
                routing(SkillRoutingResult.SkillRoutingAction.CONTINUE),
                session);

        assertFalse(resumed.context().containsKey(
                SkillPendingCoordinator.NEW_TRAVEL_PLAN));
        assertFalse(revised.context().containsKey(
                SkillPendingCoordinator.NEW_TRAVEL_PLAN));
    }

    @Test
    void resumeRequestCancelsUncommittedNewPlanMarker() {
        SkillSession session = SkillSession.create("owner")
                .withActiveSkill("travel")
                .withContextValue(SkillPendingCoordinator.NEW_TRAVEL_PLAN, "true")
                .withPendingAction(
                        SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS,
                        "initial_request");

        SkillSession updated = handler.onRouting(
                "继续刚才的旅行",
                routing(SkillRoutingResult.SkillRoutingAction.SWITCH),
                session);

        assertFalse(updated.context().containsKey(
                SkillPendingCoordinator.NEW_TRAVEL_PLAN));
        assertFalse(updated.hasPendingAction(
                SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS));
    }

    @Test
    void continueCurrentPlanningKeepsUncommittedNewPlanMarker() {
        SkillSession session = SkillSession.create("owner")
                .withActiveSkill("travel")
                .withContextValue(SkillPendingCoordinator.NEW_TRAVEL_PLAN, "true")
                .withPendingAction(
                        SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS,
                        "initial_request");

        SkillSession updated = handler.onRouting(
                "继续规划新疆三日游",
                routing(SkillRoutingResult.SkillRoutingAction.CONTINUE),
                session);

        assertTrue(updated.context().containsKey(
                SkillPendingCoordinator.NEW_TRAVEL_PLAN));
    }

    private SkillRoutingResult routing(SkillRoutingResult.SkillRoutingAction action) {
        return new SkillRoutingResult("travel", Set.of(), action, 0.9, "test");
    }
}
