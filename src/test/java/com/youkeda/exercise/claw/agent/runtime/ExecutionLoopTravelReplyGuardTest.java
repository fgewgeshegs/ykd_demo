package com.youkeda.exercise.claw.agent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.SafetyPolicy;
import com.youkeda.exercise.claw.agent.ToolResultStatusParser;
import com.youkeda.exercise.claw.agent.activity.AgentActivityRecorder;
import com.youkeda.exercise.claw.agent.memory.Message;
import com.youkeda.exercise.claw.agent.model.ResultStatus;
import com.youkeda.exercise.claw.agent.plan.PlanStore;
import com.youkeda.exercise.claw.agent.plan.PlanValidator;
import com.youkeda.exercise.claw.agent.skill.SkillPendingCoordinator;
import com.youkeda.exercise.claw.agent.skill.SkillSession;
import com.youkeda.exercise.claw.agent.skill.TravelTriggerPolicy;
import com.youkeda.exercise.claw.feature.travel.TravelToolResultSessionHandler;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.ai.llm.LLMResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
class ExecutionLoopTravelReplyGuardTest {

    @Test
    void failsSafeAfterTwoRejectedTravelReplies() {
        LLMClient llmClient = mock(LLMClient.class);
        when(llmClient.chatWithTools(anyString(), anyList(), anyList()))
                .thenReturn(new LLMResponse(
                        "已经规划好了：Day 1 去乌鲁木齐。", List.of(), "stop"));
        PlanStore planStore = mock(PlanStore.class);
        ExecutionLoop loop = new ExecutionLoop(
                llmClient,
                mock(ToolExecutor.class),
                planStore,
                mock(PlanValidator.class),
                new ObjectMapper(),
                new SkillReplyGuardRegistry(List.of(new TravelReplyGuard(new TravelTriggerPolicy()))));
        SkillSession session = SkillSession.create("owner").withActiveSkill("travel");
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", "帮我规划一个新疆三日游"));

        ExecutionLoop.Result result = loop.run(
                "system", messages, List.of(), null,
                new ToolExecutionContext(messages.get(0).content(), session, "owner"),
                session, "request-guard-max", "travel", messages.get(0).content());

        assertEquals(ExecutionLoop.LoopStatus.TEXT_REPLY, result.status());
        assertFalse(result.reply().contains("已经规划好了"));
        verify(llmClient, times(2)).chatWithTools(anyString(), anyList(), anyList());
    }

    @Test
    void retriesWhenTravelPlanIsReturnedBeforeCollectToolRuns() {
        ObjectMapper objectMapper = new ObjectMapper();
        LLMClient llmClient = mock(LLMClient.class);
        when(llmClient.chatWithTools(anyString(), anyList(), anyList()))
                .thenReturn(
                        new LLMResponse("已经规划好了：第一天去乌鲁木齐。", List.of(), "stop"),
                        new LLMResponse(null, List.of(new LLMResponse.ToolCall(
                                "tc1", "travel_collect",
                                "{\"departure_city\":\"杭州\","
                                        + "\"destination\":\"新疆\","
                                        + "\"travel_date\":\"8月5日\","
                                        + "\"duration\":\"3天2晚\"}")), "tool_calls"),
                        new LLMResponse("还需要确认出行人数和预算。", List.of(), "stop"));

        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.register(new Tool() {
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
        ToolResultStatusParser statusParser = mock(ToolResultStatusParser.class);
        when(statusParser.parse(anyString())).thenReturn(ResultStatus.PARTIAL);
        PlanStore planStore = mock(PlanStore.class);
        ToolExecutor toolExecutor = new ToolExecutor(
                toolRegistry, safetyPolicy,
                new SkillPendingCoordinator(List.of(
                        new TravelToolResultSessionHandler(objectMapper))),
                mock(AgentActivityRecorder.class), statusParser,
                planStore, objectMapper);
        ExecutionLoop loop = new ExecutionLoop(
                llmClient,
                toolExecutor,
                planStore,
                mock(PlanValidator.class),
                objectMapper,
                new SkillReplyGuardRegistry(List.of(new TravelReplyGuard(new TravelTriggerPolicy()))));
        SkillSession session = SkillSession.create("owner").withActiveSkill("travel");
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", "帮我规划一个新疆三日游，8月5号从杭州出发"));

        ExecutionLoop.Result result = loop.run(
                "system", messages, toolRegistry.getAllDefinitions(),
                null,
                new ToolExecutionContext(messages.get(0).content(), session, "owner"),
                session,
                "request-1",
                "travel",
                messages.get(0).content());

        assertEquals(ExecutionLoop.LoopStatus.TEXT_REPLY, result.status());
        assertEquals("还需要确认出行人数和预算。", result.reply());
        assertTrue(result.session().hasPendingAction(
                SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS));
        verify(llmClient, times(3)).chatWithTools(anyString(), anyList(), anyList());
    }

    @Test
    void rejectsReplyAfterTravelCollectReturnsError() {
        ObjectMapper objectMapper = new ObjectMapper();
        LLMClient llmClient = mock(LLMClient.class);
        String invalidReply = "请问出行人数是多少？";
        when(llmClient.chatWithTools(anyString(), anyList(), anyList()))
                .thenReturn(
                        new LLMResponse(null, List.of(new LLMResponse.ToolCall(
                                "tc1", "travel_collect", "{}")), "tool_calls"),
                        new LLMResponse(invalidReply, List.of(), "stop"),
                        new LLMResponse(invalidReply, List.of(), "stop"));

        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.register(new Tool() {
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
                return "{\"status\":\"ERROR\",\"error\":\"storage failed\"}";
            }
        });
        SafetyPolicy safetyPolicy = mock(SafetyPolicy.class);
        when(safetyPolicy.canExecute(anyString(), anyString())).thenReturn(null);
        PlanStore planStore = mock(PlanStore.class);
        ToolExecutor toolExecutor = new ToolExecutor(
                toolRegistry, safetyPolicy,
                new SkillPendingCoordinator(List.of(
                        new TravelToolResultSessionHandler(objectMapper))),
                mock(AgentActivityRecorder.class),
                new ToolResultStatusParser(objectMapper), planStore, objectMapper);
        ExecutionLoop loop = new ExecutionLoop(
                llmClient, toolExecutor, planStore, mock(PlanValidator.class),
                objectMapper,
                new SkillReplyGuardRegistry(List.of(
                        new TravelReplyGuard(new TravelTriggerPolicy()))));
        SkillSession session = SkillSession.create("owner").withActiveSkill("travel");
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", "帮我规划一个新疆三日游"));

        ExecutionLoop.Result result = loop.run(
                "system", messages, toolRegistry.getAllDefinitions(), null,
                new ToolExecutionContext(messages.get(0).content(), session, "owner"),
                session, "request-error", "travel", messages.get(0).content());

        assertEquals(ExecutionLoop.LoopStatus.TEXT_REPLY, result.status());
        assertFalse(invalidReply.equals(result.reply()));
        verify(llmClient, times(3)).chatWithTools(anyString(), anyList(), anyList());
    }

    @Test
    void guardsSynthesizedReplyAfterToolLoopReachesRoundLimit() {
        ObjectMapper objectMapper = new ObjectMapper();
        AtomicInteger calls = new AtomicInteger();
        LLMClient llmClient = mock(LLMClient.class);
        when(llmClient.chatWithTools(anyString(), anyList(), anyList()))
                .thenAnswer(invocation -> {
                    int call = calls.incrementAndGet();
                    if (call <= 15) {
                        return new LLMResponse(null, List.of(new LLMResponse.ToolCall(
                                "tc" + call, "web_search",
                                "{\"round\":" + call + "}")), "tool_calls");
                    }
                    return new LLMResponse(
                            "已经规划好了：第一天去乌鲁木齐。",
                            List.of(), "stop");
                });
        ToolRegistry toolRegistry = new ToolRegistry();
        toolRegistry.register(new Tool() {
            @Override
            public String getName() {
                return "web_search";
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
                return "{\"status\":\"SUCCESS\"}";
            }
        });
        SafetyPolicy safetyPolicy = mock(SafetyPolicy.class);
        when(safetyPolicy.canExecute(anyString(), anyString())).thenReturn(null);
        PlanStore planStore = mock(PlanStore.class);
        ToolExecutor toolExecutor = new ToolExecutor(
                toolRegistry, safetyPolicy,
                new SkillPendingCoordinator(List.of()),
                mock(AgentActivityRecorder.class),
                new ToolResultStatusParser(objectMapper), planStore, objectMapper);
        ExecutionLoop loop = new ExecutionLoop(
                llmClient, toolExecutor, planStore, mock(PlanValidator.class),
                objectMapper,
                new SkillReplyGuardRegistry(List.of(
                        new TravelReplyGuard(new TravelTriggerPolicy()))));
        SkillSession session = SkillSession.create("owner").withActiveSkill("travel");
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", "帮我规划一个新疆三日游"));

        ExecutionLoop.Result result = loop.run(
                "system", messages, toolRegistry.getAllDefinitions(), null,
                new ToolExecutionContext(messages.get(0).content(), session, "owner"),
                session, "request-max-rounds", "travel", messages.get(0).content());

        assertEquals(ExecutionLoop.LoopStatus.TEXT_REPLY, result.status());
        assertFalse(result.reply().contains("已经规划好了"));
        assertEquals(16, calls.get());
    }
}
