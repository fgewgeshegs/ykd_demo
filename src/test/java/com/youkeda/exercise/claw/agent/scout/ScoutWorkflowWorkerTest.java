package com.youkeda.exercise.claw.agent.scout;

import com.youkeda.exercise.claw.agent.skill.WorkflowRequest;
import com.youkeda.exercise.claw.agent.skill.WorkflowResult;
import com.youkeda.exercise.claw.scout.ScoutOrchestrator;
import com.youkeda.exercise.claw.scout.ScoutReport;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ScoutWorkflowWorkerTest {

    @Test
    void updatesTheTaskIdAllocatedByTheEntrypoint() {
        ScoutOrchestrator orchestrator = mock(ScoutOrchestrator.class);
        ScoutTaskManager taskManager = mock(ScoutTaskManager.class);
        when(orchestrator.run("AI agents"))
                .thenReturn(new ScoutReport(1, 2, 1));

        ScoutWorkflowWorker worker = new ScoutWorkflowWorker(orchestrator, taskManager);
        ReflectionTestUtils.setField(worker, "timeoutMinutes", 1);
        ReflectionTestUtils.setField(worker, "retryMax", 0);

        WorkflowRequest request = new WorkflowRequest(
                "task-123", "scoutWorkflow", "AI agents", Instant.now());

        WorkflowResult result = worker.execute(request);

        assertEquals("task-123", result.taskId());
        verify(taskManager, never()).createTask(anyString(), anyString());
        verify(taskManager).updateStatus("task-123", ScoutTaskStatus.RUNNING);
        verify(taskManager).updateStatus("task-123", ScoutTaskStatus.COMPLETED);
        verify(orchestrator).run("AI agents");
    }
}
