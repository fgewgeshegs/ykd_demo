package com.youkeda.exercise.claw.agent.skill;

import java.time.Instant;

public record WorkflowRequest(
        String workflowName,
        String userId,
        String payload,
        Instant createdAt
) {
}
