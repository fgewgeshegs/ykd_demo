package com.youkeda.exercise.claw.agent.skill;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record SkillDefinition(
        String name,
        String description,
        int priority,
        Set<String> tags,
        Set<String> requiredTools,
        Set<String> optionalTools,
        String systemPromptResource,
        String triggerPolicyName,
        SkillKnowledgeConfig knowledge,
        boolean enabled
) {
    public Set<String> allowedTools() {
        if (requiredTools == null && optionalTools == null) return Set.of();
        return Stream.concat(
                (requiredTools == null ? Stream.<String>empty() : requiredTools.stream()),
                (optionalTools == null ? Stream.<String>empty() : optionalTools.stream())
        ).collect(Collectors.toUnmodifiableSet());
    }
}
