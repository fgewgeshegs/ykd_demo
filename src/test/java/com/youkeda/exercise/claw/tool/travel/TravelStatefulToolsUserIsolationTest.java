package com.youkeda.exercise.claw.tool.travel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.runtime.ToolExecutionContext;
import com.youkeda.exercise.claw.agent.runtime.ToolRegistry;
import com.youkeda.exercise.claw.feature.travel.TravelPlanService;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TravelStatefulToolsUserIsolationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ToolExecutionContext context =
            new ToolExecutionContext("旅游方案操作", null, "user-a");

    @Test
    void saveOptionsUsesExecutionContextUserId() {
        TravelPlanService service = serviceReturningSuccess();
        TravelSaveOptionsTool tool = new TravelSaveOptionsTool(
                service, objectMapper, mock(ToolRegistry.class));

        tool.execute("{\"options\":[]}", context);

        verify(service).handle(any(JsonNode.class), eq("user-a"));
    }

    @Test
    void selectOptionUsesExecutionContextUserId() {
        TravelPlanService service = serviceReturningSuccess();
        TravelSelectOptionTool tool = new TravelSelectOptionTool(
                service, objectMapper, mock(ToolRegistry.class));

        tool.execute("{\"selected_option_id\":\"plan_a\"}", context);

        verify(service).handle(any(JsonNode.class), eq("user-a"));
    }

    @Test
    void reviseUsesExecutionContextUserId() {
        TravelPlanService service = serviceReturningSuccess();
        TravelReviseTool tool = new TravelReviseTool(
                service, objectMapper, mock(ToolRegistry.class));

        tool.execute("{\"feedback\":\"减少购物安排\"}", context);

        verify(service).handle(any(JsonNode.class), eq("user-a"));
    }

    private TravelPlanService serviceReturningSuccess() {
        TravelPlanService service = mock(TravelPlanService.class);
        when(service.handle(any(JsonNode.class), eq("user-a")))
                .thenReturn(objectMapper.createObjectNode().put("status", "SUCCESS"));
        return service;
    }
}
