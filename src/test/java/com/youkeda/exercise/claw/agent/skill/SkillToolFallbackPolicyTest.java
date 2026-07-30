package com.youkeda.exercise.claw.agent.skill;

import com.youkeda.exercise.claw.agent.tool.FunctionExecutionContext;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SkillToolFallbackPolicyTest {

    private final SkillToolFallbackPolicy policy = new SkillToolFallbackPolicy();

    @Test
    void createsProfileScoutCallWhenLlmOnlyRepliesWithText() {
        SkillRoutingResult routing = new SkillRoutingResult(
                "information-scout", Set.of(),
                SkillRoutingResult.SkillRoutingAction.CONTINUE, 0.95,
                "explicit scout request");

        var call = policy.createFallback(
                routing,
                "最近有什么值得关注的事情吗",
                new FunctionExecutionContext("最近有什么值得关注的事情吗"),
                0).orElseThrow();

        assertEquals("information_scout", call.name());
        assertEquals("{}", call.arguments());
    }

    @Test
    void doesNotFallbackForNonRequestOrAfterAnotherToolCall() {
        SkillRoutingResult routing = new SkillRoutingResult(
                "information-scout", Set.of(),
                SkillRoutingResult.SkillRoutingAction.CONTINUE, 0.95,
                "active skill");

        assertTrue(policy.createFallback(
                routing, "好的", new FunctionExecutionContext("好的"), 0).isEmpty());
        assertTrue(policy.createFallback(
                routing, "最近有什么值得关注的事情吗",
                new FunctionExecutionContext("最近有什么值得关注的事情吗"), 1).isEmpty());
    }
}
