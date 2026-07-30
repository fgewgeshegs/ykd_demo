package com.youkeda.exercise.claw.scout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.scout.ScoutTaskManager;

import com.youkeda.exercise.claw.agent.skill.WorkflowRegistry;
import com.youkeda.exercise.claw.agent.skill.WorkflowWorker;
import com.youkeda.exercise.claw.agent.tool.FunctionExecutionContext;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

class ScoutFunctionProfileModeTest {

    @Test
    void startsTaskWithoutUserIdentity() {
        ScoutTaskManager taskManager = mock(ScoutTaskManager.class);
        WorkflowRegistry workflowRegistry = mock(WorkflowRegistry.class);
        when(workflowRegistry.getWorker("scoutWorkflow"))
                .thenReturn(Optional.of(mock(WorkflowWorker.class)));
        ScoutFunction function = new ScoutFunction(
                mock(ScoutOrchestrator.class), mock(LLMFunctionRegistry.class),
                new ObjectMapper(), taskManager, workflowRegistry);

        function.execute("{}", new FunctionExecutionContext("最近有什么值得关注的事情吗"));

        verify(taskManager).createTask(anyString(), eq(""));
        verify(taskManager).isDuplicate();
    }

    @Test
    void omittedQueryStartsProfileDiscoveryInsteadOfUsingBroadRequestAsTopic() {
        ScoutTaskManager taskManager = mock(ScoutTaskManager.class);
        WorkflowRegistry workflowRegistry = mock(WorkflowRegistry.class);
        when(workflowRegistry.getWorker("scoutWorkflow"))
                .thenReturn(Optional.of(mock(WorkflowWorker.class)));
        ScoutFunction function = new ScoutFunction(
                mock(ScoutOrchestrator.class), mock(LLMFunctionRegistry.class),
                new ObjectMapper(), taskManager, workflowRegistry);

        function.execute("{}", new FunctionExecutionContext("最近有什么值得关注的事情吗"));

        verify(taskManager).createTask(anyString(), eq(""));
    }
}
