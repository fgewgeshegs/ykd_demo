package com.youkeda.exercise.claw.agent.skill;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkillLifecycleCoordinatorTest {

    @Test
    void delegatesRoutingToPrimarySkillHandlerAndReturnsUpdatedSession() {
        SkillLifecycleHandler handler = mock(SkillLifecycleHandler.class);
        when(handler.getSkillName()).thenReturn("travel");
        SkillSession session = SkillSession.create("owner").withActiveSkill("travel");
        SkillSession updated = session.withContextValue("marker", "true");
        SkillRoutingResult routing = new SkillRoutingResult(
                "travel", Set.of(), SkillRoutingResult.SkillRoutingAction.ACTIVATE,
                0.9, "test");
        when(handler.onRouting("规划新疆三日游", routing, session)).thenReturn(updated);
        SkillLifecycleCoordinator coordinator = new SkillLifecycleCoordinator(List.of(handler));

        SkillSession result = coordinator.onRouting(
                "规划新疆三日游", routing, session);

        assertSame(updated, result);
        verify(handler).onRouting("规划新疆三日游", routing, session);
    }

    @Test
    void leavesSessionUnchangedWhenNoHandlerExists() {
        SkillSession session = SkillSession.create("owner");
        SkillRoutingResult routing = new SkillRoutingResult(
                "common", Set.of(), SkillRoutingResult.SkillRoutingAction.NONE,
                0.0, "test");
        SkillLifecycleCoordinator coordinator = new SkillLifecycleCoordinator(List.of());

        assertSame(session, coordinator.onRouting("你好", routing, session));
    }
}
