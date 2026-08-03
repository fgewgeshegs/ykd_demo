package com.youkeda.exercise.claw.tool.travel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.runtime.ToolRegistry;
import com.youkeda.exercise.claw.feature.travel.TravelPlanService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class TravelToolDescriptionContractTest {

    @Test
    void statefulTravelToolsReferenceRegisteredCostToolName() {
        TravelPlanService service = mock(TravelPlanService.class);
        ObjectMapper objectMapper = new ObjectMapper();
        ToolRegistry registry = mock(ToolRegistry.class);
        String descriptions = new TravelSaveOptionsTool(service, objectMapper, registry)
                .getDescription()
                + new TravelReviseTool(service, objectMapper, registry).getDescription();

        assertTrue(descriptions.contains("travel_calculate_cost"));
        assertFalse(descriptions.contains("budget_calculator"));
    }
}
