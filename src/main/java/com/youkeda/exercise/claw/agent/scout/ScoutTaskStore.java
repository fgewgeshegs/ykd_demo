package com.youkeda.exercise.claw.agent.scout;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class ScoutTaskStore {

    private final JdbcTemplate jdbcTemplate;

    public ScoutTaskStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        ensureTable();
    }

    private void ensureTable() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS scout_tasks (
                task_id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL,
                query TEXT,
                status TEXT NOT NULL,
                skill_name TEXT,
                workflow_name TEXT,
                created_at INTEGER NOT NULL,
                completed_at INTEGER,
                summary TEXT,
                result_json TEXT
            )
        """);
    }

    public void save(ScoutTask task) {
        jdbcTemplate.update(
            "INSERT OR REPLACE INTO scout_tasks " +
            "(task_id, user_id, query, status, created_at, completed_at, summary) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)",
            task.taskId(), task.userId(), task.query(),
            task.status().name(),
            task.createdAt().getEpochSecond(),
            task.completedAt() != null ? task.completedAt().getEpochSecond() : null,
            task.summary()
        );
    }

    public Optional<ScoutTask> find(String taskId) {
        List<ScoutTask> results = jdbcTemplate.query(
            "SELECT * FROM scout_tasks WHERE task_id = ?", this::mapRow, taskId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<ScoutTask> findByUser(String userId) {
        return jdbcTemplate.query(
            "SELECT * FROM scout_tasks WHERE user_id = ? ORDER BY created_at DESC LIMIT 20",
            this::mapRow, userId);
    }

    public void updateStatus(String taskId, ScoutTaskStatus status) {
        jdbcTemplate.update(
            "UPDATE scout_tasks SET status = ?, completed_at = ? WHERE task_id = ?",
            status.name(),
            status == ScoutTaskStatus.COMPLETED || status == ScoutTaskStatus.FAILED
                    ? Instant.now().getEpochSecond() : null,
            taskId);
    }

    public void updateSummary(String taskId, String summary) {
        jdbcTemplate.update("UPDATE scout_tasks SET summary = ? WHERE task_id = ?", summary, taskId);
    }

    private ScoutTask mapRow(ResultSet rs, int rowNum) throws SQLException {
        long completedAtEpoch = rs.getLong("completed_at");
        boolean wasNull = rs.wasNull();
        return new ScoutTask(
            rs.getString("task_id"),
            rs.getString("user_id"),
            rs.getString("query"),
            ScoutTaskStatus.valueOf(rs.getString("status")),
            Instant.ofEpochSecond(rs.getLong("created_at")),
            wasNull ? null : Instant.ofEpochSecond(completedAtEpoch),
            rs.getString("summary")
        );
    }
}
