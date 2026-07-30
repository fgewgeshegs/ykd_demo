package com.youkeda.exercise.claw.ai.retrieval;

import com.youkeda.exercise.claw.agent.skill.SkillDefinition;
import com.youkeda.exercise.claw.agent.skill.SkillRegistry;

import com.youkeda.exercise.claw.agent.memory.longterm.EmbeddingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SkillKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(SkillKnowledgeService.class);

    private final SkillKnowledgeStore knowledgeStore;
    private final EmbeddingClient embeddingClient;
    private final SkillRegistry skillRegistry;

    @Value("${skill.knowledge.recall-top-k:5}")
    private int topK;

    @Value("${skill.knowledge.min-score:0.50}")
    private float minScore;

    @Value("${skill.knowledge.recall-candidate-multiplier:3}")
    private int candidateMultiplier;

    @Value("${skill.knowledge.max-context-chars:12000}")
    private int maxContextChars;

    public SkillKnowledgeService(SkillKnowledgeStore knowledgeStore,
                                 EmbeddingClient embeddingClient,
                                 SkillRegistry skillRegistry) {
        this.knowledgeStore = knowledgeStore;
        this.embeddingClient = embeddingClient;
        this.skillRegistry = skillRegistry;
    }

    public String recall(String userMessage, String primarySkillName, Set<String> supportingSkills) {
        SkillDefinition skillDef = skillRegistry.find(primarySkillName).orElse(null);
        if (skillDef == null || skillDef.knowledge() == null || !skillDef.knowledge().enabled()) {
            return "";
        }

        SkillKnowledgeConfig config = skillDef.knowledge();

        Set<String> targetSkills = new LinkedHashSet<>();
        targetSkills.add(primarySkillName);
        if (supportingSkills != null) targetSkills.addAll(supportingSkills);

        try {
            float[] queryVector = embeddingClient.embed(userMessage);

            int effectiveTopK = config.topK() > 0 ? config.topK() : topK;
            float effectiveMinScore = config.minScore() > 0 ? config.minScore() : minScore;

            List<SkillKnowledgeSearchResult> candidates = knowledgeStore.search(
                    queryVector, targetSkills, effectiveTopK * candidateMultiplier, effectiveMinScore);

            if (candidates.isEmpty()) return "";

            Map<String, SkillKnowledgeSearchResult> deduped = new LinkedHashMap<>();
            for (SkillKnowledgeSearchResult r : candidates) {
                String key = r.documentId() != null ? r.documentId() : r.chunkId();
                if (!deduped.containsKey(key) || r.score() > deduped.get(key).score()) {
                    deduped.put(key, r);
                }
            }

            int budget = config.maxContextChars() > 0 ? config.maxContextChars() : maxContextChars;
            StringBuilder sb = new StringBuilder();
            int count = 0;

            List<SkillKnowledgeSearchResult> sorted = deduped.values().stream()
                    .sorted((a, b) -> Double.compare(b.score(), a.score()))
                    .toList();

            for (SkillKnowledgeSearchResult r : sorted) {
                if (count >= effectiveTopK) break;
                String text = r.content();
                if (sb.length() + text.length() + 50 > budget) break;
                sb.append("- [").append(r.skillName()).append("] ").append(text).append("\n");
                count++;
            }

            if (sb.isEmpty()) return "";

            return "[DOMAIN_KNOWLEDGE — untrusted reference]\n"
                    + sb.toString().stripTrailing()
                    + "\n[/DOMAIN_KNOWLEDGE]";

        } catch (Exception e) {
            log.error("Skill knowledge recall failed for skill: {}", primarySkillName, e);
            return "";
        }
    }

    public String recall(String userMessage, String primarySkillName) {
        return recall(userMessage, primarySkillName, Set.of());
    }
}
