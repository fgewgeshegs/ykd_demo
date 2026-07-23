package com.youkeda.exercise.claw.budget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BudgetCalculatorFunctionTest {

    @Test
    void shouldAcceptSnakeCaseToolArguments() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        BudgetCalculatorFunction function = new BudgetCalculatorFunction(
                new BudgetCalculatorService(), objectMapper, new LLMFunctionRegistry());

        String response = function.execute("""
                {
                  "headcount":30,
                  "days":2,
                  "nights":1,
                  "city":"无锡",
                  "target_total_budget":35000,
                  "contingency_rate":0,
                  "plans":[{
                    "plan_id":"A",
                    "plan_name":"方案A",
                    "plan_version":1,
                    "items":[{
                      "category":"TRANSPORT",
                      "item_name":"往返大巴",
                      "pricing_mode":"FIXED",
                      "unit_price":8000,
                      "price_status":"CONFIRMED",
                      "price_source":"商家报价"
                    }]
                  }]
                }
                """);

        JsonNode result = objectMapper.readTree(response);
        assertEquals("SUCCESS", result.path("status").asText());
        assertEquals("A", result.path("plans").get(0).path("planId").asText());
        assertEquals(8000, result.path("plans").get(0).path("estimatedTotalMax").asInt());
    }
}
