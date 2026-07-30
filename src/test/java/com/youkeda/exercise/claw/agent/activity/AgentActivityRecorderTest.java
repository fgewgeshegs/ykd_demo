package com.youkeda.exercise.claw.agent.activity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AgentActivityRecorderTest {

    private AgentActivityStore store;
    private AgentActivityRecorder recorder;

    @BeforeEach
    void setUp() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource(
                "jdbc:sqlite::memory:", true);
        store = new AgentActivityStore(new JdbcTemplate(dataSource));
        store.init();
        recorder = new AgentActivityRecorder(store);
    }

    @Test
    void recordsSkillAndToolLifecycleWithoutSensitivePayloads() {
        String requestId = recorder.beginRequest();
        recorder.skillSelected(requestId, "weather");
        recorder.toolStarted(requestId, "weather", "weather_query");
        recorder.toolFinished(requestId, "weather", "weather_query", true, 42L);
        recorder.requestCompleted(requestId, 71L);

        List<AgentActivity> activities = store.findRecent(20);

        assertFalse(requestId.isBlank());
        assertEquals(List.of(
                        ActivityEventType.RESPONSE_COMPLETED,
                        ActivityEventType.TOOL_SUCCEEDED,
                        ActivityEventType.TOOL_STARTED,
                        ActivityEventType.SKILL_SELECTED,
                        ActivityEventType.REQUEST_RECEIVED),
                activities.stream().map(AgentActivity::eventType).toList());
        assertEquals("weather_query", activities.get(1).toolName());
        assertEquals(42L, activities.get(1).durationMs());
        assertFalse(activities.stream().anyMatch(activity ->
                activity.summary().contains("arguments") || activity.summary().contains("result")));
    }

    @Test
    void recordsBlockedAndFailedToolCallsSeparately() {
        String requestId = recorder.beginRequest();
        recorder.toolBlocked(requestId, "travel", "route_query", "安全策略阻止");
        recorder.toolFinished(requestId, "travel", "route_query", false, 18L);

        List<AgentActivity> activities = store.findRecent(10);

        assertEquals(ActivityEventType.TOOL_FAILED, activities.get(0).eventType());
        assertEquals(ActivityEventType.TOOL_BLOCKED, activities.get(1).eventType());
    }
}
