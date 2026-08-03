package com.youkeda.exercise.claw.agent.skill;

import com.youkeda.exercise.claw.skill.SkillDefinition;
import com.youkeda.exercise.claw.skill.SkillRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TravelSkillRoutingTest {

    @Test
    void routesMultiDayTripRequestToTravelSkill() {
        SkillDefinition travel = travelSkill();
        SkillRegistry registry = mock(SkillRegistry.class);
        when(registry.getAll()).thenReturn(List.of(travel));
        SkillSessionStore store = mock(SkillSessionStore.class);
        when(store.find("owner")).thenReturn(Optional.empty());
        TriggerPolicyFactory policyFactory = mock(TriggerPolicyFactory.class);
        when(policyFactory.getPolicy("travelTriggerPolicy"))
                .thenReturn(new TravelTriggerPolicy());
        SkillLlmRouter llmRouter = mock(SkillLlmRouter.class);
        SkillRouter router = new SkillRouter(
                registry, store, policyFactory, llmRouter, emptyTriggers());

        SkillRoutingResult result = router.route(
                "帮我规划一个新疆三日游，8月5号从杭州出发", "owner");

        assertEquals("travel", result.primarySkill());
        assertEquals(SkillRoutingResult.SkillRoutingAction.ACTIVATE, result.action());
        verifyNoInteractions(llmRouter);
    }

    @Test
    void pendingTravelSlotAnswerContinuesTravelWithoutTriggerWords() {
        SkillSession session = SkillSession.create("owner")
                .withActiveSkill("travel")
                .withPendingAction(
                        SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS,
                        "participant_count");
        SkillSessionStore store = mock(SkillSessionStore.class);
        when(store.find("owner")).thenReturn(Optional.of(session));
        SkillLlmRouter llmRouter = mock(SkillLlmRouter.class);
        SkillRouter router = new SkillRouter(
                mock(SkillRegistry.class), store, mock(TriggerPolicyFactory.class),
                llmRouter, emptyTriggers());

        SkillRoutingResult result = router.route("一个人", "owner");

        assertEquals("travel", result.primarySkill());
        assertEquals(SkillRoutingResult.SkillRoutingAction.CONTINUE, result.action());
        verifyNoInteractions(llmRouter);
    }

    private SkillDefinition travelSkill() {
        SkillDefinition travel = mock(SkillDefinition.class);
        when(travel.name()).thenReturn("travel");
        when(travel.triggerPolicyName()).thenReturn("travelTriggerPolicy");
        when(travel.priority()).thenReturn(5);
        return travel;
    }

    private TriggerProperties emptyTriggers() {
        TriggerProperties properties = mock(TriggerProperties.class);
        when(properties.getTriggers()).thenReturn(Map.of());
        return properties;
    }
}
