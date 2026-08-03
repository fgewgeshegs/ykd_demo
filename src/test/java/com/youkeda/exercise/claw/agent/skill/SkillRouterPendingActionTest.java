package com.youkeda.exercise.claw.agent.skill;
import com.youkeda.exercise.claw.skill.SkillRegistry;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
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

    @Test
    void bareTravelCancellationDeactivatesPendingTravelAction() {
        SkillSessionStore store = mock(SkillSessionStore.class);
        SkillSession session = SkillSession.create("owner")
                .withActiveSkill("travel")
                .withPendingAction(
                        SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS,
                        "budget");
        when(store.find("owner")).thenReturn(Optional.of(session));
        SkillRouter router = new SkillRouter(
                mock(SkillRegistry.class), store, mock(TriggerPolicyFactory.class),
                mock(SkillLlmRouter.class), mock(TriggerProperties.class));

        SkillRoutingResult result = router.route("不规划了", "owner");

        assertEquals("common", result.primarySkill());
        assertEquals(SkillRoutingResult.SkillRoutingAction.DEACTIVATE, result.action());
    }

    @Test
    void explicitWeatherTriggerPreemptsPendingTravelCollection() {
        SkillRegistry registry = mock(SkillRegistry.class);
        SkillSessionStore store = mock(SkillSessionStore.class);
        TriggerPolicyFactory policyFactory = mock(TriggerPolicyFactory.class);
        TriggerProperties triggerProperties = mock(TriggerProperties.class);
        SkillSession session = SkillSession.create("owner")
                .withActiveSkill("travel")
                .withPendingAction(
                        SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS,
                        "budget");
        when(store.find("owner")).thenReturn(Optional.of(session));
        com.youkeda.exercise.claw.skill.SkillDefinition weather =
                mock(com.youkeda.exercise.claw.skill.SkillDefinition.class);
        when(weather.name()).thenReturn("weather");
        when(weather.priority()).thenReturn(2);
        when(weather.triggerPolicyName()).thenReturn("keywordTriggerPolicy");
        when(registry.getAll()).thenReturn(List.of(weather));
        when(triggerProperties.getTriggers())
                .thenReturn(Map.of("weather", List.of("天气")));
        SkillRouter router = new SkillRouter(
                registry, store, policyFactory,
                mock(SkillLlmRouter.class), triggerProperties);

        SkillRoutingResult result = router.route("杭州今天天气怎么样", "owner");

        assertEquals("weather", result.primarySkill());
        assertEquals(SkillRoutingResult.SkillRoutingAction.ACTIVATE, result.action());
    }

    @Test
    void shortBudgetResumesSuspendedTravelAfterWeather() {
        SkillRegistry registry = mock(SkillRegistry.class);
        SkillSessionStore store = mock(SkillSessionStore.class);
        TriggerPolicyFactory policyFactory = mock(TriggerPolicyFactory.class);
        TriggerProperties triggerProperties = mock(TriggerProperties.class);
        SkillSession weather = SkillSession.create("owner")
                .withActiveSkill("travel")
                .withPendingAction(
                        SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS,
                        "budget")
                .withActiveSkill("weather");
        when(store.find("owner")).thenReturn(Optional.of(weather));
        com.youkeda.exercise.claw.skill.SkillDefinition travel =
                mock(com.youkeda.exercise.claw.skill.SkillDefinition.class);
        when(travel.name()).thenReturn("travel");
        when(travel.priority()).thenReturn(10);
        when(travel.triggerPolicyName()).thenReturn("travelTriggerPolicy");
        when(registry.getAll()).thenReturn(List.of(travel));
        when(triggerProperties.getTriggers()).thenReturn(Map.of());
        when(policyFactory.getPolicy("travelTriggerPolicy"))
                .thenReturn(new TravelTriggerPolicy());
        SkillRouter router = new SkillRouter(
                registry, store, policyFactory,
                mock(SkillLlmRouter.class), triggerProperties);

        SkillRoutingResult result = router.route("2000元", "owner");

        assertEquals("travel", result.primarySkill());
        assertEquals(SkillRoutingResult.SkillRoutingAction.ACTIVATE, result.action());
    }
}
