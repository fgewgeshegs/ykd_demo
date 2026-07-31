package com.youkeda.exercise.claw.agent.skill;
import com.youkeda.exercise.claw.skill.SkillRegistry;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SkillRouterPendingActionTest {

    @Test
    void routesShortAnswerToPendingScoutActionBeforeNormalTriggerChecks() {
        SkillRegistry registry = mock(SkillRegistry.class);
        SkillSessionStore store = mock(SkillSessionStore.class);
        TriggerPolicyFactory policyFactory = mock(TriggerPolicyFactory.class);
        SkillLlmRouter llmRouter = mock(SkillLlmRouter.class);
        TriggerProperties triggerProperties = mock(TriggerProperties.class);
        SkillSession session = SkillSession.create("owner")
                .withActiveSkill("information-scout")
                .withPendingAction("START_INFORMATION_SCOUT", "query");
        when(store.find("owner")).thenReturn(Optional.of(session));
        SkillRouter router = new SkillRouter(
                registry, store, policyFactory, llmRouter, triggerProperties);

        SkillRoutingResult result = router.route("AI / 深度学习", "owner");

        assertEquals("information-scout", result.primarySkill());
        assertEquals(SkillRoutingResult.SkillRoutingAction.CONTINUE, result.action());
        assertEquals(0.95, result.confidence());
        verifyNoInteractions(llmRouter);
    }

    @Test
    void cancellationDeactivatesPendingScoutAction() {
        SkillSessionStore store = mock(SkillSessionStore.class);
        SkillSession session = SkillSession.create("owner")
                .withActiveSkill("information-scout")
                .withPendingAction("START_INFORMATION_SCOUT", "query");
        when(store.find("owner")).thenReturn(Optional.of(session));
        SkillRouter router = new SkillRouter(
                mock(SkillRegistry.class), store, mock(TriggerPolicyFactory.class),
                mock(SkillLlmRouter.class), mock(TriggerProperties.class));

        SkillRoutingResult result = router.route("算了，不查了", "owner");

        assertEquals("common", result.primarySkill());
        assertEquals(SkillRoutingResult.SkillRoutingAction.DEACTIVATE, result.action());
    }
}
