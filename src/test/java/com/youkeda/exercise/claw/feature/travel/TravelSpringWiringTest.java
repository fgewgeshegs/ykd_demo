package com.youkeda.exercise.claw.feature.travel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.runtime.SkillReplyGuardRegistry;
import com.youkeda.exercise.claw.agent.runtime.TravelReplyGuard;
import com.youkeda.exercise.claw.agent.skill.SkillLifecycleCoordinator;
import com.youkeda.exercise.claw.agent.skill.SkillPendingCoordinator;
import com.youkeda.exercise.claw.agent.skill.TravelTriggerPolicy;
import com.youkeda.exercise.claw.feature.scout.skill.InformationScoutToolResultSessionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TravelSpringWiringTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ObjectMapper.class)
            .withBean(TravelTriggerPolicy.class)
            .withBean(TravelReplyGuard.class)
            .withBean(SkillReplyGuardRegistry.class)
            .withBean(TravelSkillLifecycleHandler.class)
            .withBean(SkillLifecycleCoordinator.class)
            .withBean(TravelToolResultSessionHandler.class)
            .withBean(InformationScoutToolResultSessionHandler.class)
            .withBean(SkillPendingCoordinator.class);

    @Test
    void wiresTravelPoliciesAndRegisteredHandlers() {
        contextRunner.run(context -> {
            assertNull(context.getStartupFailure());
            assertNotNull(context.getBean(TravelReplyGuard.class));
            assertNotNull(context.getBean(SkillReplyGuardRegistry.class));
            assertNotNull(context.getBean(SkillLifecycleCoordinator.class));
            SkillPendingCoordinator coordinator = context.getBean(
                    SkillPendingCoordinator.class);
            assertDoesNotThrow(() -> coordinator.afterToolExecution(
                    com.youkeda.exercise.claw.agent.skill.SkillSession
                            .create("owner").withActiveSkill("travel"),
                    "travel_collect",
                    "{\"status\":\"NEED_MORE_INFORMATION\","
                            + "\"missing_fields\":[\"budget\"]}"));
        });
    }
}
