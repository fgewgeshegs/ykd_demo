package com.youkeda.exercise.claw.ai.retrieval;

public record SkillKnowledgeChunk(
        String chunkId,
        String skillName,
        String documentId,
        int chunkIndex,
        String content,
        String source,
        Integer pageNumber,
        String version
) {
}
