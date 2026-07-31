package com.youkeda.exercise.claw.ai.retrieval;

import java.util.List;
import java.util.Set;

public interface SkillKnowledgeStore {

    void upsert(SkillKnowledgeChunk chunk, float[] vector);

    List<SkillKnowledgeSearchResult> search(
            float[] queryVector,
            Set<String> skillNames,
            int topK,
            float minScore
    );

    void deleteByDocument(String documentId);
}
