package com.youkeda.exercise.claw.agent.runtime;

import com.youkeda.exercise.claw.agent.SafetyPolicy;
import com.youkeda.exercise.claw.agent.ToolResultStatusParser;
import com.youkeda.exercise.claw.agent.activity.AgentActivityRecorder;
import com.youkeda.exercise.claw.agent.model.ResultStatus;
import com.youkeda.exercise.claw.agent.plan.PlanStore;
import com.youkeda.exercise.claw.agent.skill.SkillPendingCoordinator;
import com.youkeda.exercise.claw.agent.skill.SkillSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.ai.llm.LLMResponse;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ToolExecutorToolStatusTest {

    /** 最小测试桩：返回固定 JSON 结果的工具 */
    static class StubTool extends AbstractTool {
        private final String name;
        private final String resultJson;
        private boolean available = true;

        StubTool(String name, String resultJson, ToolRegistry registry, ObjectMapper om) {
            super(registry, om);
            this.name = name;
            this.resultJson = resultJson;
        }

        @Override public String getName() { return name; }
        @Override public String getDescription() { return "stub"; }
        @Override public JsonNode getParameters() { return schema().build(); }
        @Override public String execute(String argumentsJson, ToolExecutionContext context) {
            return resultJson;
        }
        @Override public boolean isAvailable(ToolExecutionContext context) { return available; }
        void setAvailable(boolean available) { this.available = available; }
    }

    @Test
    void recordsSuccessAndBlockedStatusPerTool() {
        ObjectMapper om = new ObjectMapper();
        ToolRegistry registry = new ToolRegistry();
        StubTool ok = new StubTool("weather_query", "{\"status\":\"SUCCESS\"}", registry, om);
        StubTool blocked = new StubTool("didi_ride", "{\"status\":\"BLOCKED\"}", registry, om);
        blocked.setAvailable(false);
        registry.register(ok);
        registry.register(blocked);

        SafetyPolicy allowAll = mock(SafetyPolicy.class);
        when(allowAll.canExecute(any(), any())).thenReturn(null);

        ToolExecutor executor = new ToolExecutor(
                registry, allowAll, mock(SkillPendingCoordinator.class),
                mock(AgentActivityRecorder.class), new ToolResultStatusParser(om),
                mock(PlanStore.class), om);

        LLMResponse.ToolCall callOk = new LLMResponse.ToolCall("c1", "weather_query", "{}");
        LLMResponse.ToolCall callBlocked = new LLMResponse.ToolCall("c2", "didi_ride", "{}");

        ToolExecutor.ToolExecutionBatch batch = executor.executeToolCalls(
                List.of(callOk, callBlocked),
                mock(ToolExecutionContext.class),
                SkillSession.create("u"),
                null, "req", "common", "帮我查天气",
                new HashSet<>());

        Map<String, ResultStatus> statuses = batch.toolStatuses();
        assertEquals(ResultStatus.SUCCESS, statuses.get("weather_query"));
        assertEquals(ResultStatus.BLOCKED, statuses.get("didi_ride"));
        assertTrue(batch.executedInBatch());
    }

    @Test
    void recordsFailedForUnknownTool() {
        ObjectMapper om = new ObjectMapper();
        ToolRegistry registry = new ToolRegistry();
        SafetyPolicy allowAll = mock(SafetyPolicy.class);
        when(allowAll.canExecute(any(), any())).thenReturn(null);
        ToolExecutor executor = new ToolExecutor(
                registry, allowAll, mock(SkillPendingCoordinator.class),
                mock(AgentActivityRecorder.class), new ToolResultStatusParser(om),
                mock(PlanStore.class), om);

        LLMResponse.ToolCall call = new LLMResponse.ToolCall("c1", "no_such_tool", "{}");
        ToolExecutor.ToolExecutionBatch batch = executor.executeToolCalls(
                List.of(call), mock(ToolExecutionContext.class),
                SkillSession.create("u"), null, "req", "common", "x", new HashSet<>());

        assertEquals(ResultStatus.FAILED, batch.toolStatuses().get("no_such_tool"));
    }
}
