package com.youkeda.exercise.claw.agent.skill;

import java.time.Instant;

public record WorkflowResult(
        String taskId,
        WorkflowStatus status,
        Instant completedAt,
        String summary,
        Object details
) {
    public enum WorkflowStatus {
        COMPLETED, FAILED, CANCELLED, TIMEOUT
    }
}
