package com.youkeda.exercise.claw.agent.scout;

import com.youkeda.exercise.claw.agent.skill.WorkflowRequest;
import com.youkeda.exercise.claw.agent.skill.WorkflowResult;
import com.youkeda.exercise.claw.agent.skill.WorkflowWorker;
import com.youkeda.exercise.claw.scout.ScoutOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;

@Component("scoutWorkflowWorker")
public class ScoutWorkflowWorker implements WorkflowWorker {

    private static final Logger log = LoggerFactory.getLogger(ScoutWorkflowWorker.class);

    private final ScoutOrchestrator orchestrator;
    private final ScoutTaskManager taskManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "scout-worker");
        t.setDaemon(true);
        return t;
    });

    @Value("${workflow.timeout-minutes:30}")
    private int timeoutMinutes;

    @Value("${workflow.retry-max:3}")
    private int retryMax;

    public ScoutWorkflowWorker(ScoutOrchestrator orchestrator,
                               ScoutTaskManager taskManager) {
        this.orchestrator = orchestrator;
        this.taskManager = taskManager;
    }

    @Override
    public String getName() {
        return "scoutWorkflowWorker";
    }

    @Override
    public WorkflowResult execute(WorkflowRequest request) {
        String taskId = UUID.randomUUID().toString();
        taskManager.createTask(taskId, request.userId(), request.payload());

        Duration timeout = Duration.ofMinutes(timeoutMinutes);
        Exception lastError = null;

        for (int attempt = 0; attempt <= retryMax; attempt++) {
            if (attempt > 0) {
                log.info("Retry {}/{} for scout workflow, task={}", attempt, retryMax, taskId);
                try { Thread.sleep(1000L * attempt); } catch (InterruptedException ignored) { break; }
            }

            taskManager.updateStatus(taskId, ScoutTaskStatus.RUNNING);

            try {
                Future<WorkflowResult> future = executor.submit(() -> {
                    com.youkeda.exercise.claw.scout.ScoutReport report = orchestrator.runForUser(request.userId());
                    String summary = report.toString();
                    return new WorkflowResult(taskId, WorkflowResult.WorkflowStatus.COMPLETED,
                            Instant.now(), summary, null);
                });

                WorkflowResult result = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
                taskManager.updateStatus(taskId, ScoutTaskStatus.COMPLETED);
                taskManager.updateSummary(taskId, result.summary());
                log.info("Scout workflow completed: taskId={}", taskId);
                return result;

            } catch (TimeoutException e) {
                lastError = e;
                log.warn("Scout workflow timeout attempt {}/{}", attempt + 1, retryMax);
                taskManager.updateStatus(taskId, ScoutTaskStatus.PENDING);
            } catch (Exception e) {
                lastError = e;
                log.error("Scout workflow failed attempt {}/{}", attempt + 1, retryMax, e);
                taskManager.updateStatus(taskId, ScoutTaskStatus.PENDING);
            }
        }

        taskManager.updateStatus(taskId, ScoutTaskStatus.FAILED);
        return new WorkflowResult(taskId, WorkflowResult.WorkflowStatus.FAILED,
                Instant.now(), "All retries exhausted: " + lastError.getMessage(), null);
    }
}
