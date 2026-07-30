package com.youkeda.exercise.claw.ai.retrieval;

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
