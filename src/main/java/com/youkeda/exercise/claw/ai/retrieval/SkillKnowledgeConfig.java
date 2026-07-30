package com.youkeda.exercise.claw.ai.retrieval;

import java.util.Set;

public record SkillKnowledgeConfig(
        boolean enabled,
        Set<String> namespaces,
        int topK,
        float minScore,
        int maxContextChars
) {
    public static SkillKnowledgeConfig disabled() {
        return new SkillKnowledgeConfig(false, Set.of(), 0, 0f, 0);
    }
}
