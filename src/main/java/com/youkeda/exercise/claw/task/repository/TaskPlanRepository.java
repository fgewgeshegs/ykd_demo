package com.youkeda.exercise.claw.task.repository;

import com.youkeda.exercise.claw.task.model.TaskPlan;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务计划 SQLite 持久化仓库
 *
 * <p>表 {@code task_plan} 存储用户的高层目标拆解计划。
 * 复用同数据库文件 {@code ./data/claw-tasks.db}。
 */
@Repository
public class TaskPlanRepository {

    private static final Logger log = LoggerFactory.getLogger(TaskPlanRepository.class);

    private static final String TABLE_DDL = """
            CREATE TABLE IF NOT EXISTS task_plan (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id      TEXT NOT NULL,
                goal         TEXT NOT NULL,
                tasks_json   TEXT NOT NULL,
                status       TEXT NOT NULL DEFAULT 'PREVIEW',
                created_time TEXT NOT NULL DEFAULT (datetime('now','localtime'))
            )
            """;

    private static final String INSERT_SQL = """
            INSERT INTO task_plan (user_id, goal, tasks_json, status, created_time)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String SELECT_BY_ID = """
            SELECT id, user_id, goal, tasks_json, status, created_time
            FROM task_plan WHERE id = ?
            """;

    private static final String SELECT_BY_USER = """
            SELECT id, user_id, goal, tasks_json, status, created_time
            FROM task_plan WHERE user_id = ? ORDER BY created_time DESC
            """;

    private static final String SELECT_BY_USER_AND_STATUS = """
            SELECT id, user_id, goal, tasks_json, status, created_time
            FROM task_plan WHERE user_id = ? AND status = ? ORDER BY created_time DESC
            """;

    private static final String UPDATE_STATUS = """
            UPDATE task_plan SET status = ? WHERE id = ?
            """;

    private static final String UPDATE_STATUS_BY_USER = """
            UPDATE task_plan SET status = ? WHERE id = ? AND user_id = ?
            """;

    @Value("${task.db-path:./data/claw-tasks.db}")
    private String dbPath;

    public TaskPlanRepository() {
    }

    @PostConstruct
    public void init() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(TABLE_DDL);
            log.info("任务计划表初始化完成 | path={}", dbPath);
        } catch (SQLException e) {
            log.error("任务计划表初始化失败 | path={}", dbPath, e);
            throw new RuntimeException("任务计划表初始化失败", e);
        }
    }

    public TaskPlan save(TaskPlan plan) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, plan.getUserId());
            ps.setString(2, plan.getGoal());
            ps.setString(3, plan.getTasksJson());
            ps.setString(4, plan.getStatus());
            ps.setString(5, plan.getCreatedTimeAsString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) plan.setId(rs.getLong(1));
            }
            log.info("任务计划已保存 | id={} | userId={} | goal={} | tasks={}",
                    plan.getId(), plan.getUserId(), plan.getGoal(),
                    plan.getTasksJson() != null ? plan.getTasksJson().length() : 0);
            return plan;
        } catch (SQLException e) {
            log.error("保存任务计划失败 | userId={} | goal={}", plan.getUserId(), plan.getGoal(), e);
            throw new RuntimeException("保存任务计划失败", e);
        }
    }

    public TaskPlan findById(Long id) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapPlan(rs);
            }
        } catch (SQLException e) {
            log.error("查询任务计划失败 | id={}", id, e);
        }
        return null;
    }

    public List<TaskPlan> findByUserId(String userId) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_USER)) {
            ps.setString(1, userId);
            List<TaskPlan> results = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapPlan(rs));
            }
            return results;
        } catch (SQLException e) {
            log.error("查询用户计划失败 | userId={}", userId, e);
            return List.of();
        }
    }

    public List<TaskPlan> findByUserIdAndStatus(String userId, String status) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_USER_AND_STATUS)) {
            ps.setString(1, userId);
            ps.setString(2, status);
            List<TaskPlan> results = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapPlan(rs));
            }
            return results;
        } catch (SQLException e) {
            log.error("查询用户计划失败 | userId={} | status={}", userId, status, e);
            return List.of();
        }
    }

    public boolean updateStatus(Long id, String status) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS)) {
            ps.setString(1, status);
            ps.setLong(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("更新计划状态失败 | id={} | status={}", id, status, e);
            return false;
        }
    }

    public boolean updateStatus(Long id, String status, String userId) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS_BY_USER)) {
            ps.setString(1, status);
            ps.setLong(2, id);
            ps.setString(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("更新计划状态失败 | id={} | status={} | userId={}", id, status, userId, e);
            return false;
        }
    }

    public boolean markConfirmed(Long id, String userId) {
        return updateStatus(id, TaskPlan.STATUS_CONFIRMED, userId);
    }

    public boolean markCancelled(Long id, String userId) {
        return updateStatus(id, TaskPlan.STATUS_CANCELLED, userId);
    }

    public boolean markDone(Long id) {
        return updateStatus(id, TaskPlan.STATUS_DONE);
    }

    private TaskPlan mapPlan(ResultSet rs) throws SQLException {
        TaskPlan plan = new TaskPlan();
        plan.setId(rs.getLong("id"));
        plan.setUserId(rs.getString("user_id"));
        plan.setGoal(rs.getString("goal"));
        plan.setTasksJson(rs.getString("tasks_json"));
        plan.setStatus(rs.getString("status"));
        plan.setCreatedTimeFromString(rs.getString("created_time"));
        return plan;
    }

    private Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
        }
        return conn;
    }
}