package com.youkeda.exercise.claw.agent.runtime;

import com.youkeda.exercise.claw.agent.skill.SkillSession;
import com.youkeda.exercise.claw.agent.skill.TravelTriggerPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillReplyGuardRegistryTest {

    @Test
    void delegatesValidationToActiveSkillGuard() {
        SkillReplyGuardRegistry registry = new SkillReplyGuardRegistry(
                List.of(new TravelReplyGuard(new TravelTriggerPolicy())));

        SkillReplyGuard.GuardResult result = registry.validate(
                "travel",
                "规划新疆三日游",
                "已经规划好了。",
                SkillSession.create("owner").withActiveSkill("travel"),
                Set.of());

        assertFalse(result.allowed());
    }

    @Test
    void allowsSkillsWithoutRegisteredGuard() {
        SkillReplyGuardRegistry registry = new SkillReplyGuardRegistry(List.of());

        SkillReplyGuard.GuardResult result = registry.validate(
                "common", "你好", "你好", SkillSession.create("owner"), Set.of());

        assertTrue(result.allowed());
    }
}
