package com.youkeda.exercise.claw.ai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DsmlToolCallParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldConvertDsmlContentToStructuredBudgetToolCall() throws Exception {
        String content = """
                <｜｜DSML｜｜tool_calls>
                <｜｜DSML｜｜invoke name="budget_calculator">
                <｜｜DSML｜｜parameter name="headcount" string="false">30</｜｜DSML｜｜parameter>
                <｜｜DSML｜｜parameter name="city" string="true">无锡</｜｜DSML｜｜parameter>
                <｜｜DSML｜｜parameter name="plans" string="false">[{"plan_id":"plan_a","items":[]}]</｜｜DSML｜｜parameter>
                </｜｜DSML｜｜invoke>
                </｜｜DSML｜｜tool_calls>
                """;

        List<LLMResponse.ToolCall> calls = DsmlToolCallParser.parse(content, objectMapper);

        assertEquals(1, calls.size());
        assertEquals("budget_calculator", calls.get(0).name());
        JsonNode arguments = objectMapper.readTree(calls.get(0).arguments());
        assertEquals(30, arguments.get("headcount").asInt());
        assertEquals("无锡", arguments.get("city").asText());
        assertTrue(arguments.get("plans").isArray());
        assertFalse(arguments.get("headcount").isTextual());
    }

    @Test
    void shouldSupportMultipleInvocationsAndXmlEntities() throws Exception {
        String content = """
                <｜｜DSML｜｜tool_calls>
                <｜｜DSML｜｜invoke name="first_tool">
                <｜｜DSML｜｜parameter name="query" string="true">酒店 &amp; 餐饮</｜｜DSML｜｜parameter>
                </｜｜DSML｜｜invoke>
                <｜｜DSML｜｜invoke name="second_tool">
                <｜｜DSML｜｜parameter name="enabled" string="false">true</｜｜DSML｜｜parameter>
                </｜｜DSML｜｜invoke>
                </｜｜DSML｜｜tool_calls>
                """;

        List<LLMResponse.ToolCall> calls = DsmlToolCallParser.parse(content, objectMapper);

        assertEquals(2, calls.size());
        assertEquals("酒店 & 餐饮",
                objectMapper.readTree(calls.get(0).arguments()).get("query").asText());
        assertTrue(objectMapper.readTree(calls.get(1).arguments()).get("enabled").asBoolean());
    }
}
