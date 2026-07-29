package com.youkeda.exercise.claw.agent.skill;

public record SkillKnowledgeSearchResult(
        String chunkId,
        String skillName,
        String documentId,
        String content,
        String source,
        Integer pageNumber,
        double score
) {
}
