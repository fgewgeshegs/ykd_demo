package com.youkeda.exercise.claw.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.memory.ContextStore;
import com.youkeda.exercise.claw.agent.tool.LLMFunction;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.ai.llm.LLMResponse;
import com.youkeda.exercise.claw.ai.llm.ToolDefinition;
import com.youkeda.exercise.claw.teamtrip.InMemoryTeamTripPlanStateStore;
import com.youkeda.exercise.claw.teamtrip.TeamTripPlanDraft;
import com.youkeda.exercise.claw.teamtrip.TeamTripPlanOption;
import com.youkeda.exercise.claw.teamtrip.TeamTripPlanService;
import com.youkeda.exercise.claw.teamtrip.TeamTripToolCallPolicy;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReActAgentExecutorTest {

    @Test
    void shouldStopOfferingToolsWhenWholeBatchWasBlocked() {
        Fixture fixture = fixture();
        when(fixture.llmClient.chatWithTools(anyList(), anyList()))
                .thenReturn(
                        new LLMResponse(null,
                                List.of(new LLMResponse.ToolCall("tc1", "unknown_tool", "{}")),
                                "tool_calls"),
                        new LLMResponse("请补充必要信息。", List.of(), "stop"));

        String reply = fixture.executor.execute(new AgentContext()
                .setUserId("u1").setMessage("帮我做方案"));

        assertEquals("请补充必要信息。", reply);
        ArgumentCaptor<List<ToolDefinition>> tools = ArgumentCaptor.forClass(List.class);
        verify(fixture.llmClient, times(2)).chatWithTools(anyList(), tools.capture());
        assertFalse(tools.getAllValues().get(0).isEmpty());
        assertTrue(tools.getAllValues().get(1).isEmpty());
    }

    @Test
    void shouldSynthesizeExistingResultsInsteadOfReturningTimeoutAtRoundLimit() {
        Fixture fixture = fixture();
        AtomicInteger calls = new AtomicInteger();
        when(fixture.llmClient.chatWithTools(anyList(), anyList())).thenAnswer(invocation -> {
            int call = calls.incrementAndGet();
            if (call <= 12) {
                return new LLMResponse(null,
                        List.of(new LLMResponse.ToolCall(
                                "tc" + call, "dummy_tool", "{\"round\":" + call + "}")),
                        "tool_calls");
            }
            return new LLMResponse("已根据现有结果整理回复。", List.of(), "stop");
        });

        String reply = fixture.executor.execute(new AgentContext()
                .setUserId("u2").setMessage("生成完整方案"));

        assertEquals("已根据现有结果整理回复。", reply);
        assertEquals(13, calls.get());
    }

    @Test
    void shouldNotReportTimeoutWhenFinalSynthesisStillRequestsTool() {
        Fixture fixture = fixture();
        AtomicInteger calls = new AtomicInteger();
        when(fixture.llmClient.chatWithTools(anyList(), anyList())).thenAnswer(invocation -> {
            int call = calls.incrementAndGet();
            return new LLMResponse(null,
                    List.of(new LLMResponse.ToolCall(
                            "tc" + call,
                            call <= 12 ? "dummy_tool" : "budget_calculator",
                            "{\"round\":" + call + "}")),
                    "tool_calls");
        });

        String reply = fixture.executor.execute(new AgentContext()
                .setUserId("u-limit").setMessage("生成完整方案"));

        assertTrue(reply.contains("当前可用信息"));
        assertFalse(reply.contains("处理请求超时"));
        assertFalse(reply.contains("继续生成"));
        assertEquals(13, calls.get());
    }

    @Test
    void shouldRemoveLegacyLimitReplyWhenUserContinuesGeneration() {
        Fixture fixture = fixture();
        when(fixture.contextStore.getHistory(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of(
                new com.youkeda.exercise.claw.agent.memory.Message("user", "生成团建方案"),
                new com.youkeda.exercise.claw.agent.memory.Message("assistant",
                        "本轮处理步骤已达到上限，请回复“继续生成”。"),
                new com.youkeda.exercise.claw.agent.memory.Message("user", "继续生成")));
        when(fixture.llmClient.chatWithTools(anyList(), anyList()))
                .thenReturn(new LLMResponse("继续完成方案。", List.of(), "stop"));

        fixture.executor.execute(new AgentContext()
                .setUserId("u-continue").setMessage("继续生成"));

        ArgumentCaptor<List<com.youkeda.exercise.claw.agent.memory.Message>> messages =
                ArgumentCaptor.forClass(List.class);
        verify(fixture.llmClient).chatWithTools(messages.capture(), anyList());
        assertFalse(messages.getValue().stream()
                .anyMatch(message -> message.content() != null
                        && message.content().contains("本轮处理步骤已达到上限")));
        assertTrue(messages.getValue().stream()
                .anyMatch(message -> "system".equals(message.role())
                        && message.content().contains("不得复述")));
    }

    @Test
    void shouldReturnOptionComparisonImmediatelyAfterBudgetCalculation() {
        ObjectMapper objectMapper = new ObjectMapper();
        LLMClient llmClient = mock(LLMClient.class);
        ContextStore contextStore = mock(ContextStore.class);
        when(contextStore.getHistory(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of());

        InMemoryTeamTripPlanStateStore store = new InMemoryTeamTripPlanStateStore();
        TeamTripPlanDraft draft = new TeamTripPlanDraft();
        draft.setStage("OPTIONS_READY_FOR_COSTING");
        draft.setOptions(List.of(
                option("plan_a", "太湖风光线"),
                option("plan_b", "惠山古镇线"),
                option("plan_c", "蠡园休闲线")));
        store.save("u3", draft);

        LLMFunctionRegistry registry = new LLMFunctionRegistry();
        registry.register(new LLMFunction() {
            @Override
            public String getName() {
                return "budget_calculator";
            }

            @Override
            public String getDescription() {
                return "测试预算工具";
            }

            @Override
            public com.fasterxml.jackson.databind.JsonNode getParameters() {
                return objectMapper.createObjectNode().put("type", "object");
            }

            @Override
            public String execute(String argumentsJson) {
                return """
                        {"status":"SUCCESS","plans":[
                          {"planId":"plan_a","costStatus":"SUCCESS","estimatedTotalMin":9800,
                           "estimatedTotalMax":9800,"perPersonMin":326.67,"perPersonMax":326.67,
                           "targetBudget":10000,"budgetStatus":"WITHIN_BUDGET"},
                          {"planId":"plan_b","costStatus":"SUCCESS","estimatedTotalMin":10800,
                           "estimatedTotalMax":10800,"perPersonMin":360,"perPersonMax":360,
                           "targetBudget":10000,"budgetStatus":"OVER_BUDGET"},
                          {"planId":"plan_c","costStatus":"SUCCESS","estimatedTotalMin":10000,
                           "estimatedTotalMax":10000,"perPersonMin":333.33,"perPersonMax":333.33,
                           "targetBudget":10000,"budgetStatus":"WITHIN_BUDGET"}
                        ]}
                        """;
            }
        });

        TeamTripPlanService service = new TeamTripPlanService(store, objectMapper);
        ReActAgentExecutor executor = new ReActAgentExecutor(
                llmClient, registry, contextStore, objectMapper,
                new TeamTripToolCallPolicy(store), service);
        when(llmClient.chatWithTools(anyList(), anyList())).thenReturn(
                new LLMResponse(null,
                        List.of(new LLMResponse.ToolCall("tc-budget", "budget_calculator", "{}")),
                        "tool_calls"),
                new LLMResponse("""
                        三个候选方案已经核算完成：

                        方案A：太湖风光线，预计总费用 ¥9800。

                        请回复你想选择的方案。
                        """, List.of(), "stop"));

        String reply = executor.execute(new AgentContext()
                .setUserId("u3").setMessage("生成三个方案"));

        assertTrue(reply.contains("三个候选方案已经核算完成"));
        assertTrue(reply.contains("太湖风光线"));
        assertTrue(reply.contains("¥9800"));
        assertTrue(reply.contains("请回复你想选择的方案"));
        verify(llmClient, times(2)).chatWithTools(anyList(), anyList());
    }

    private static TeamTripPlanOption option(String id, String name) {
        TeamTripPlanOption option = new TeamTripPlanOption();
        option.setOptionId(id);
        option.setDisplayName(name);
        option.setPositioning("测试定位");
        option.setHighlights("测试亮点");
        option.setItinerarySummary("测试行程");
        return option;
    }

    private Fixture fixture() {
        ObjectMapper objectMapper = new ObjectMapper();
        LLMClient llmClient = mock(LLMClient.class);
        ContextStore contextStore = mock(ContextStore.class);
        when(contextStore.getHistory(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of());

        LLMFunctionRegistry registry = new LLMFunctionRegistry();
        registry.register(new LLMFunction() {
            @Override
            public String getName() {
                return "dummy_tool";
            }

            @Override
            public String getDescription() {
                return "测试工具";
            }

            @Override
            public com.fasterxml.jackson.databind.JsonNode getParameters() {
                return objectMapper.createObjectNode().put("type", "object");
            }

            @Override
            public String execute(String argumentsJson) {
                return "{\"status\":\"SUCCESS\"}";
            }
        });

        InMemoryTeamTripPlanStateStore store = new InMemoryTeamTripPlanStateStore();
        TeamTripPlanService service = new TeamTripPlanService(store, objectMapper);
        TeamTripToolCallPolicy policy = new TeamTripToolCallPolicy(store);
        ReActAgentExecutor executor = new ReActAgentExecutor(
                llmClient, registry, contextStore, objectMapper, policy, service);
        return new Fixture(llmClient, executor, contextStore);
    }

    private record Fixture(LLMClient llmClient, ReActAgentExecutor executor,
                           ContextStore contextStore) {
    }
}
