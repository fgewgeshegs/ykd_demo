package com.youkeda.exercise.claw.agent.skill;

public interface WorkflowWorker {

    String getName();

    WorkflowResult execute(WorkflowRequest request);
}
