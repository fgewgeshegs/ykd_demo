package com.youkeda.exercise.claw.ai.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.memory.Message;
import com.youkeda.exercise.claw.common.PromptLoader;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class LLMClientReasoningContentTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LLMClient client = new LLMClient(
            new LLMProperties(), objectMapper, mock(PromptLoader.class));

    @Test
    void shouldPassReasoningContentBackWithAssistantToolCall() throws Exception {
        Message message = new Message(
                "assistant", "{\"city\":\"无锡\"}",
                null, null, null,
                "call_1", "weather_query", "先确认天气日期");

        Method method = LLMClient.class.getDeclaredMethod("serializeMessage", Message.class);
        method.setAccessible(true);
        ObjectNode serialized = (ObjectNode) method.invoke(client, message);

        assertEquals("先确认天气日期", serialized.path("reasoning_content").asText());
        assertEquals("weather_query",
                serialized.path("tool_calls").get(0).path("function").path("name").asText());
    }

    @Test
    void shouldReadReasoningContentFromStructuredResponse() throws Exception {
        String body = """
                {"choices":[{"finish_reason":"tool_calls","message":{
                  "content":null,
                  "reasoning_content":"需要先查询天气",
                  "tool_calls":[{"id":"call_1","type":"function","function":{
                    "name":"weather_query","arguments":"{\\"city\\":\\"无锡\\"}"
                  }}]
                }}]}
                """;

        Method method = LLMClient.class.getDeclaredMethod("parseStructuredResponse", String.class);
        method.setAccessible(true);
        LLMResponse response = (LLMResponse) method.invoke(client, body);

        assertEquals("需要先查询天气", response.getReasoningContent());
        assertEquals("weather_query", response.getToolCalls().get(0).name());
    }
}
