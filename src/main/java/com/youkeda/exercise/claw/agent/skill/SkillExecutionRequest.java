package com.youkeda.exercise.claw.agent.skill;

public record SkillExecutionRequest(
        SkillDefinition skill,
        String currentMessage,
        SkillSession session,
        String workflowName
) {
}
