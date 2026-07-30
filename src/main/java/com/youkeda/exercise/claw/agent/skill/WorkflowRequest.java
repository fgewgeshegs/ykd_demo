package com.youkeda.exercise.claw.agent.skill;

import java.time.Instant;

public record WorkflowRequest(
        String taskId,
        String workflowName,
        String payload,
        Instant createdAt
) {
}
