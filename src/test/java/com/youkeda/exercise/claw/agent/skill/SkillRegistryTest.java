package com.youkeda.exercise.claw.agent.skill;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SkillRegistryTest {

    @Test
    void testSkillDefinition_allowedTools() {
        SkillDefinition def = new SkillDefinition(
            "test", "test skill", 5, Set.of("tag1"),
            Set.of("tool_a", "tool_b"),
            Set.of("tool_c"),
            "prompts/test.txt", null, SkillKnowledgeConfig.disabled(), true
        );
        assertEquals(Set.of("tool_a", "tool_b", "tool_c"), def.allowedTools());
    }

    @Test
    void testSkillDefinition_allowedToolsWithNulls() {
        SkillDefinition def = new SkillDefinition(
            "test", "test skill", 5, Set.of("tag1"),
            null, null,
            "prompts/test.txt", null, SkillKnowledgeConfig.disabled(), true
        );
        assertEquals(Set.of(), def.allowedTools());
    }

    @Test
    void testSkillDefinition_enabled() {
        SkillDefinition enabled = new SkillDefinition(
            "test", "", 0, Set.of(), Set.of(), Set.of(),
            null, null, SkillKnowledgeConfig.disabled(), true);
        assertTrue(enabled.enabled());
    }

    @Test
    void testSkillHealth_status() {
        assertEquals(SkillHealth.SkillStatus.HEALTHY,
            new SkillHealth("test", SkillHealth.SkillStatus.HEALTHY, Set.of(), Set.of()).status());
        assertEquals(SkillHealth.SkillStatus.DEGRADED,
            new SkillHealth("test", SkillHealth.SkillStatus.DEGRADED, Set.of("tool_c"), Set.of()).status());
        assertEquals(SkillHealth.SkillStatus.UNAVAILABLE,
            new SkillHealth("test", SkillHealth.SkillStatus.UNAVAILABLE, Set.of(), Set.of("tool_a")).status());
    }

    @Test
    void testSkillKnowledgeConfig_disabled() {
        SkillKnowledgeConfig config = SkillKnowledgeConfig.disabled();
        assertFalse(config.enabled());
        assertEquals(0, config.topK());
    }
}
