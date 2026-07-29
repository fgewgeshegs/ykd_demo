package com.youkeda.exercise.claw.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.memory.ContextStore;
import com.youkeda.exercise.claw.agent.memory.longterm.LongTermMemoryService;
import com.youkeda.exercise.claw.agent.plan.DefaultPlanStore;
import com.youkeda.exercise.claw.agent.plan.PlanValidator;
import com.youkeda.exercise.claw.agent.tool.FunctionExecutionContext;
import com.youkeda.exercise.claw.agent.tool.LLMFunction;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.ai.llm.LLMResponse;
import com.youkeda.exercise.claw.ai.llm.ToolDefinition;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReActAgentToolAvailabilityTest {

    @Test
    void shouldNotExposeOrExecuteToolWhenCurrentMessageDoesNotAllowIt() {
        ObjectMapper objectMapper = new ObjectMapper();
        LLMClient llmClient = mock(LLMClient.class);
        ContextStore contextStore = mock(ContextStore.class);
        when(contextStore.getHistory(anyInt())).thenReturn(List.of());

        AtomicInteger executions = new AtomicInteger();
        LLMFunctionRegistry registry = new LLMFunctionRegistry();
        registry.register(new LLMFunction() {
            @Override
            public String getName() {
                return "information_scout";
            }

            @Override
            public String getDescription() {
                return "测试用信息猎手";
            }

            @Override
            public JsonNode getParameters() {
                return objectMapper.createObjectNode().put("type", "object");
            }

            @Override
            public boolean isAvailable(FunctionExecutionContext context) {
                return context.currentMessage().contains("帮我查");
            }

            @Override
            public String execute(String argumentsJson) {
                executions.incrementAndGet();
                return "{\"status\":\"SUCCESS\"}";
            }
        });

        LongTermMemoryService longTermMemoryService = mock(LongTermMemoryService.class);
        when(longTermMemoryService.recall(anyString())).thenReturn(List.of());
        ReActAgentExecutor executor = new ReActAgentExecutor(
                llmClient, registry, contextStore, objectMapper,
                new DefaultPlanStore(), new PlanValidator(), new SafetyPolicy(),
                longTermMemoryService);

        when(llmClient.chatWithSystemPrompt(anyString(), anyString()))
                .thenReturn("NEED_TOOLS");
        when(llmClient.chatWithTools(anyList(), anyList()))
                .thenReturn(
                        new LLMResponse(null,
                                List.of(new LLMResponse.ToolCall(
                                        "tc-scout", "information_scout", "{}")),
                                "tool_calls"),
                        new LLMResponse("了解，你两类比赛都有参与。", List.of(), "stop"));

        String reply = executor.execute(new AgentContext().setMessage("都涉及一点"));

        assertEquals("了解，你两类比赛都有参与。", reply);
        assertEquals(0, executions.get());
        ArgumentCaptor<List<ToolDefinition>> tools = ArgumentCaptor.forClass(List.class);
        verify(llmClient, times(2)).chatWithTools(anyList(), tools.capture());
        assertTrue(tools.getAllValues().get(0).stream()
                .noneMatch(tool -> "information_scout".equals(tool.name())));
        assertTrue(tools.getAllValues().get(1).isEmpty());
    }
}
