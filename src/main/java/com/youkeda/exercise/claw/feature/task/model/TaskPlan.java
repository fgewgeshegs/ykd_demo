package com.youkeda.exercise.claw.feature.task.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 任务计划实体
 *
 * <p>表示用户高层目标的拆解计划。
 * 由 {@code plan_tasks} 创建，{@code execute_plan_tasks} 执行。
 *
 * <p>状态流转：
 * PREVIEW → CONFIRMED → （执行后变为 DONE）
 * PREVIEW → CANCELLED
 */
public class TaskPlan {

    public static final String STATUS_PREVIEW = "PREVIEW";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_DONE = "DONE";

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Long id;
    private String userId;
    private String goal;
    /** JSON 数组：每个元素含 order, content, delay_minutes/execute_time, repeat_type */
    private String tasksJson;
    private String status;
    private LocalDateTime createdTime;

    public TaskPlan() {
    }

    public TaskPlan(String userId, String goal, String tasksJson) {
        this.userId = userId;
        this.goal = goal;
        this.tasksJson = tasksJson;
        this.status = STATUS_PREVIEW;
        this.createdTime = LocalDateTime.now();
    }

    // ==================== Getters & Setters ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public String getTasksJson() { return tasksJson; }
    public void setTasksJson(String tasksJson) { this.tasksJson = tasksJson; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    // ==================== 序列化辅助 ====================

    public String getCreatedTimeAsString() {
        return createdTime != null ? createdTime.format(DTF) : null;
    }

    public void setCreatedTimeFromString(String str) {
        this.createdTime = (str != null && !str.isEmpty())
                ? LocalDateTime.parse(str, DTF)
                : null;
    }

    public boolean isPreview() {
        return STATUS_PREVIEW.equals(status);
    }

    public boolean isConfirmed() {
        return STATUS_CONFIRMED.equals(status);
    }

    public String getStatusDisplay() {
        return switch (status) {
            case STATUS_PREVIEW -> "待确认";
            case STATUS_CONFIRMED -> "已确认";
            case STATUS_CANCELLED -> "已取消";
            case STATUS_DONE -> "已完成";
            default -> status;
        };
    }
}