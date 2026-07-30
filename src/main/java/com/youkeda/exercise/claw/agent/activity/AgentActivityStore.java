package com.youkeda.exercise.claw.agent.activity;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

@Component
public class AgentActivityStore {

    private static final int MAX_RECENT_LIMIT = 200;
    private static final int MAX_SUMMARY_LENGTH = 300;

    private final JdbcTemplate jdbcTemplate;

    public AgentActivityStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS agent_activity (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                request_id  TEXT NOT NULL,
                event_type  TEXT NOT NULL,
                skill_name  TEXT,
                tool_name   TEXT,
                status      TEXT NOT NULL,
                summary     TEXT NOT NULL DEFAULT '',
                duration_ms INTEGER,
                created_at  INTEGER NOT NULL
            )
            """);
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_agent_activity_created
            ON agent_activity(created_at DESC)
            """);
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_agent_activity_request
            ON agent_activity(request_id, created_at)
            """);
    }

    public void record(AgentActivityEvent event) {
        jdbcTemplate.update("""
                INSERT INTO agent_activity
                    (request_id, event_type, skill_name, tool_name, status, summary, duration_ms, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                event.requestId(), event.eventType().name(), event.skillName(), event.toolName(),
                event.status(), truncate(event.summary(), MAX_SUMMARY_LENGTH), event.durationMs(),
                System.currentTimeMillis());
    }

    public List<AgentActivity> findRecent(int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, MAX_RECENT_LIMIT));
        return jdbcTemplate.query("""
                SELECT id, request_id, event_type, skill_name, tool_name, status,
                       summary, duration_ms, created_at
                FROM agent_activity
                ORDER BY id DESC
                LIMIT ?
                """, (rs, rowNum) -> mapActivity(rs), limit);
    }

    public DashboardSummary getSummary() {
        return jdbcTemplate.queryForObject("""
                SELECT
                    SUM(CASE WHEN event_type = 'REQUEST_RECEIVED' THEN 1 ELSE 0 END) AS request_count,
                    SUM(CASE WHEN event_type IN ('TOOL_SUCCEEDED', 'TOOL_FAILED') THEN 1 ELSE 0 END) AS tool_count,
                    SUM(CASE WHEN event_type IN ('TOOL_FAILED', 'REQUEST_FAILED') THEN 1 ELSE 0 END) AS failure_count,
                    MAX(created_at) AS last_activity_at
                FROM agent_activity
                """, (rs, rowNum) -> new DashboardSummary(
                rs.getLong("request_count"),
                rs.getLong("tool_count"),
                rs.getLong("failure_count"),
                nullableInstant(rs, "last_activity_at")));
    }

    private static AgentActivity mapActivity(ResultSet rs) throws SQLException {
        long duration = rs.getLong("duration_ms");
        Long durationMs = rs.wasNull() ? null : duration;
        return new AgentActivity(
                rs.getLong("id"),
                rs.getString("request_id"),
                ActivityEventType.valueOf(rs.getString("event_type")),
                rs.getString("skill_name"),
                rs.getString("tool_name"),
                rs.getString("status"),
                rs.getString("summary"),
                durationMs,
                Instant.ofEpochMilli(rs.getLong("created_at")));
    }

    private static Instant nullableInstant(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : Instant.ofEpochMilli(value);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }
}
