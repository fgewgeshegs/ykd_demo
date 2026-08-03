package com.youkeda.exercise.claw.agent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.SafetyPolicy;
import com.youkeda.exercise.claw.agent.ToolResultStatusParser;
import com.youkeda.exercise.claw.agent.activity.AgentActivityRecorder;

import com.youkeda.exercise.claw.agent.plan.PlanStore;
import com.youkeda.exercise.claw.agent.skill.SkillPendingCoordinator;
import com.youkeda.exercise.claw.agent.skill.SkillSession;
import com.youkeda.exercise.claw.feature.travel.TravelToolResultSessionHandler;
import com.youkeda.exercise.claw.ai.llm.LLMResponse;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ToolExecutorTravelPendingTest {

    @Test
    void propagatesTravelCollectResultIntoPendingSession() {
        ObjectMapper objectMapper = new ObjectMapper();
        ToolRegistry registry = new ToolRegistry();
        registry.register(new Tool() {
            @Override
            public String getName() {
                return "travel_collect";
            }

            @Override
            public String getDescription() {
                return "test";
            }

            @Override
            public JsonNode getParameters() {
                return objectMapper.createObjectNode().put("type", "object");
            }

            @Override
            public String execute(String argumentsJson) {
                return "{\"status\":\"NEED_MORE_INFORMATION\","
                        + "\"missing_fields\":[\"participant_count\",\"budget\"]}";
            }
        });

        SafetyPolicy safetyPolicy = mock(SafetyPolicy.class);
        when(safetyPolicy.canExecute(anyString(), anyString())).thenReturn(null);
        ToolResultStatusParser statusParser = new ToolResultStatusParser(objectMapper);
        ToolExecutor executor = new ToolExecutor(
                registry,
                safetyPolicy,
                new SkillPendingCoordinator(List.of(
                        new TravelToolResultSessionHandler(objectMapper))),
                mock(AgentActivityRecorder.class),
                statusParser,
                mock(PlanStore.class),
                objectMapper);
        SkillSession session = SkillSession.create("owner").withActiveSkill("travel");

        ToolExecutor.ToolExecutionBatch batch = executor.executeToolCalls(
                List.of(new LLMResponse.ToolCall("tc1", "travel_collect", "{}")),
                new ToolExecutionContext("帮我规划新疆三日游", session, "owner"),
                session,
                null,
                "request-1",
                "travel",
                "帮我规划新疆三日游",
                new HashSet<>());

        assertTrue(batch.session().hasPendingAction(
                SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS));
        assertEquals("participant_count", batch.session().pendingSlot());
    }
}
