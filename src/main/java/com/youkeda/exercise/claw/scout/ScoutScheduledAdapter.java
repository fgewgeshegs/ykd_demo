package com.youkeda.exercise.claw.scout;

import com.youkeda.exercise.claw.agent.skill.WorkflowRegistry;
import com.youkeda.exercise.claw.agent.skill.WorkflowRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class ScoutScheduledAdapter {

    private static final Logger log = LoggerFactory.getLogger(ScoutScheduledAdapter.class);

    private final WorkflowRegistry workflowRegistry;

    public ScoutScheduledAdapter(WorkflowRegistry workflowRegistry) {
        this.workflowRegistry = workflowRegistry;
    }

    @Scheduled(cron = "${workflow.scout.cron:0 0 8 * * *}")
    public void scheduledScout() {
        log.info("Scheduled scout workflow triggered");
        workflowRegistry.getWorker("scoutWorkflow").ifPresent(worker -> {
            WorkflowRequest request = new WorkflowRequest(
                    "scoutWorkflow", "default", null, Instant.now());
            worker.execute(request);
        });
    }
}
