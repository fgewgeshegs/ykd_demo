package com.youkeda.exercise.claw.agent.skill;

public interface SkillExecutor {

    String getName();

    SkillExecutionResult execute(SkillExecutionRequest request);
}
