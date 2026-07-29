package com.youkeda.exercise.claw.agent.scout;

import java.time.Instant;

public record ScoutTask(
        String taskId,
        String userId,
        String query,
        ScoutTaskStatus status,
        Instant createdAt,
        Instant completedAt,
        String summary
) {
}
