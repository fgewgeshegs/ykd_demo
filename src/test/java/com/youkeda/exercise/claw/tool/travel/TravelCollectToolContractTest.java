package com.youkeda.exercise.claw.tool.travel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.runtime.ToolExecutionContext;
import com.youkeda.exercise.claw.agent.runtime.ToolRegistry;
import com.youkeda.exercise.claw.agent.skill.SkillPendingCoordinator;
import com.youkeda.exercise.claw.agent.skill.SkillSession;
import com.youkeda.exercise.claw.feature.travel.TravelPlanService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TravelCollectToolContractTest {

    @Test
    void descriptionAlwaysRequiresCollectForNewTravelRequest() {
        TravelCollectTool tool = new TravelCollectTool(
                mock(TravelPlanService.class), new ObjectMapper(), mock(ToolRegistry.class));

        String description = tool.getDescription();

        assertTrue(description.contains("新的旅游规划请求必须先调用本工具"));
        assertFalse(description.contains("先用文字一次性追问，不调用此工具"));
    }

    @Test
    void startsNewPlanAtomicallyWhenLifecycleMarkedRequest() {
        ObjectMapper objectMapper = new ObjectMapper();
        TravelPlanService service = mock(TravelPlanService.class);
        ObjectNode result = objectMapper.createObjectNode().put("status", "NEED_MORE_INFORMATION");
        when(service.startNewPlan(any(), eq("user-a"), eq("request-1"))).thenReturn(result);
        TravelCollectTool tool = new TravelCollectTool(
                service, objectMapper, mock(ToolRegistry.class));
        SkillSession session = SkillSession.create("user-a")
                .withActiveSkill("travel")
                .withContextValue(SkillPendingCoordinator.NEW_TRAVEL_PLAN, "request-1");

        tool.execute("{\"destination\":\"新疆\"}",
                new ToolExecutionContext("规划新疆三日游", session, "user-a"));

        verify(service).startNewPlan(any(), eq("user-a"), eq("request-1"));
        verify(service, never()).handle(any(), any(String.class));
    }

    @Test
    void updatesExistingPlanWithExecutionContextUser() {
        ObjectMapper objectMapper = new ObjectMapper();
        TravelPlanService service = mock(TravelPlanService.class);
        ObjectNode result = objectMapper.createObjectNode().put("status", "NEED_MORE_INFORMATION");
        when(service.handle(any(), eq("user-a"))).thenReturn(result);
        TravelCollectTool tool = new TravelCollectTool(
                service, objectMapper, mock(ToolRegistry.class));

        tool.execute("{\"participant_count\":1}",
                new ToolExecutionContext(
                        "一个人",
                        SkillSession.create("user-a").withActiveSkill("travel"),
                        "user-a"));

        verify(service).handle(any(), eq("user-a"));
    }
}
