package com.youkeda.exercise.claw.agent.runtime;

import com.youkeda.exercise.claw.agent.model.ResultStatus;
import com.youkeda.exercise.claw.agent.runtime.SkillReplyGuard.GuardContext;
import com.youkeda.exercise.claw.agent.runtime.SkillReplyGuard.GuardResult;
import com.youkeda.exercise.claw.agent.skill.SkillSession;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SkillReplyGuardRegistryTest {

    @Test
    void dispatchesToSkillSpecificGuard() {
        SkillReplyGuard guard = new SkillReplyGuard() {
            @Override public String getSkillName() { return "travel"; }
            @Override public GuardResult validate(GuardContext ctx) {
                return GuardResult.reject("travel 专属：先调 travel_collect");
            }
        };
        SkillReplyGuardRegistry registry = new SkillReplyGuardRegistry(java.util.List.of(guard));

        GuardResult result = registry.validate("travel", "我要去三亚",
                "已规划好", SkillSession.create("u"), Set.of(), Map.of());
        assertFalse(result.allowed());
        assertEquals("travel 专属：先调 travel_collect", result.correction());
    }

    @Test
    void allowsWhenNoGuardForSkill() {
        SkillReplyGuardRegistry registry = new SkillReplyGuardRegistry(java.util.List.of());
        GuardResult result = registry.validate("weather", "今天天气",
                "晴", SkillSession.create("u"), Set.of(), Map.of());
        assertTrue(result.allowed());
    }

    @Test
    void rejectsDuplicateSkillGuard() {
        SkillReplyGuard guard = new SkillReplyGuard() {
            @Override public String getSkillName() { return "travel"; }
            @Override public GuardResult validate(GuardContext ctx) { return GuardResult.allow(); }
        };
        assertThrows(IllegalStateException.class,
                () -> new SkillReplyGuardRegistry(java.util.List.of(guard, guard)));
    }

    @Test
    void guardsWithNullSkillRunForEverySkill() {
        SkillReplyGuard guard = new SkillReplyGuard() {
            @Override public String getSkillName() { return null; } // 全局
            @Override public GuardResult validate(GuardContext ctx) {
                return GuardResult.reject("全局 guard");
            }
        };
        SkillReplyGuardRegistry registry = new SkillReplyGuardRegistry(java.util.List.of(guard));
        GuardResult result = registry.validate("anySkill", "x", "y",
                SkillSession.create("u"), Set.of(), Map.of());
        assertFalse(result.allowed());
    }

    @Test
    void passesExactToolStatusesToGuard() {
        var captured = new java.util.concurrent.atomic.AtomicReference<Map<String, ResultStatus>>();
        SkillReplyGuard guard = new SkillReplyGuard() {
            @Override public String getSkillName() { return "test"; }
            @Override public GuardResult validate(GuardContext ctx) {
                captured.set(ctx.toolStatuses());
                return GuardResult.allow();
            }
        };
        SkillReplyGuardRegistry registry = new SkillReplyGuardRegistry(java.util.List.of(guard));
        Map<String, ResultStatus> input = Map.of("some_tool", ResultStatus.SUCCESS);
        registry.validate("test", "msg", "reply", SkillSession.create("u"),
                Set.of(), input);
        assertEquals(input, captured.get(),
                "toolStatuses 必须无修改地透传——禁止全映射为 SUCCESS");
    }

    @Test
    void allowsWhenToolStatusesNull() {
        SkillReplyGuardRegistry registry = new SkillReplyGuardRegistry(java.util.List.of());
        GuardResult result = registry.validate("weather", "今天天气", "晴",
                SkillSession.create("u"), Set.of(), null);
        assertTrue(result.allowed());
    }

    @Test
    void allowsWhenExecutedCallsNull() {
        SkillReplyGuardRegistry registry = new SkillReplyGuardRegistry(java.util.List.of());
        GuardResult result = registry.validate("weather", "今天天气", "晴",
                SkillSession.create("u"), null, Map.of());
        assertTrue(result.allowed());
    }
}
