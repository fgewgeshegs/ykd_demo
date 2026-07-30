package com.youkeda.exercise.claw.scout.matcher;

import com.youkeda.exercise.claw.agent.memory.longterm.EmbeddingClient;
import com.youkeda.exercise.claw.scout.ScoutProperties;
import com.youkeda.exercise.claw.scout.context.UserProfile;
import com.youkeda.exercise.claw.scout.processor.InformationFreshness;
import com.youkeda.exercise.claw.scout.processor.InformationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 候选信息匹配器
 *
 * 将用户画像向量化，与新采集的信息做语义相似度匹配
 */
@Service
public class CandidateMatcher {

    private static final Logger log = LoggerFactory.getLogger(CandidateMatcher.class);

    private final EmbeddingClient embeddingClient;
    private final ScoutProperties props;

    public CandidateMatcher(EmbeddingClient embeddingClient, ScoutProperties props) {
        this.embeddingClient = embeddingClient;
        this.props = props;
    }

    /**
     * 匹配用户兴趣与信息
     */
    public List<MatchedCandidate> match(UserProfile profile,
                                         List<InformationItem> items) {
        return match(profile, null, items);
    }

    /**
     * 匹配本次明确主题、用户画像与信息。
     */
    public List<MatchedCandidate> match(UserProfile profile,
                                         String explicitQuery,
                                         List<InformationItem> items) {
        if (items.isEmpty()) return List.of();

        try {
            List<InformationItem> freshItems = items.stream()
                    .filter(item -> InformationFreshness.isFresh(item, props.getFreshnessDays()))
                    .toList();
            if (freshItems.isEmpty()) {
                log.info("语义匹配完成 | total={} | fresh=0 | matched=0", items.size());
                return List.of();
            }

            List<String> facets = profileFacets(profile, explicitQuery);
            if (facets.isEmpty()) {
                // 无画像时返回全部（按采集时间）
                log.info("用户画像为空，返回全部候选");
                return freshItems.stream()
                        .filter(item -> item.getVector() != null)
                        .limit(props.getMaxCandidates())
                        .map(item -> new MatchedCandidate(item, 0.5f, "通用推荐"))
                        .toList();
            }

            List<float[]> facetVectors = embeddingClient.embedBatch(facets);
            boolean hasExplicitQuery = explicitQuery != null && !explicitQuery.isBlank();

            // 每条信息取与单个画像维度的最佳相似度，避免整份画像相互稀释。
            List<MatchedCandidate> ranked = new ArrayList<>();
            for (InformationItem item : freshItems) {
                if (item.getVector() == null) continue;

                float topicScore = hasExplicitQuery
                        ? cosineSimilarity(facetVectors.get(0), item.getVector())
                        : -1f;
                if (hasExplicitQuery && topicScore < props.getFallbackMatchScore()) {
                    continue;
                }

                float bestScore = -1f;
                int bestFacet = -1;
                int firstProfileFacet = hasExplicitQuery ? 1 : 0;
                for (int i = firstProfileFacet; i < facetVectors.size(); i++) {
                    float score = cosineSimilarity(facetVectors.get(i), item.getVector());
                    if (score > bestScore) {
                        bestScore = score;
                        bestFacet = i;
                    }
                }
                if (hasExplicitQuery) {
                    float rankingScore = bestFacet >= 0
                            ? 0.85f * topicScore + 0.15f * bestScore
                            : topicScore;
                    String reason = "匹配本次主题：" + explicitQuery.trim();
                    if (bestFacet >= 0) {
                        reason += "；画像排序：" + facets.get(bestFacet);
                    }
                    ranked.add(new MatchedCandidate(item, rankingScore, reason));
                } else if (bestFacet >= 0) {
                    ranked.add(new MatchedCandidate(
                            item, bestScore, "匹配画像维度：" + facets.get(bestFacet)));
                }
            }

            ranked.sort(Comparator.comparingDouble(MatchedCandidate::semanticScore).reversed());
            List<MatchedCandidate> strictMatches = ranked.stream()
                    .filter(candidate -> candidate.semanticScore() >= props.getMinMatchScore())
                    .limit(props.getMaxCandidates())
                    .toList();

            List<MatchedCandidate> topK = new ArrayList<>(strictMatches);
            int fallbackSlots = Math.min(
                    props.getFallbackCandidateCount(),
                    Math.max(0, props.getMaxCandidates() - topK.size()));
            if (fallbackSlots > 0) {
                List<MatchedCandidate> supplements = ranked.stream()
                        .filter(candidate -> candidate.semanticScore() < props.getMinMatchScore())
                        .filter(candidate -> candidate.semanticScore() >= props.getFallbackMatchScore())
                        .limit(fallbackSlots)
                        .toList();
                topK.addAll(supplements);
            }

            float maxScore = ranked.isEmpty() ? 0f : ranked.get(0).semanticScore();
            int supplemented = topK.size() - strictMatches.size();
            log.info("语义匹配完成 | total={} | fresh={} | strict={} | supplemented={} | matched={} | maxScore={} | threshold={}",
                    items.size(), freshItems.size(), strictMatches.size(), supplemented,
                    topK.size(), maxScore, props.getMinMatchScore());

            return topK;
        } catch (Exception e) {
            log.error("语义匹配失败", e);
            return List.of();
        }
    }

    private List<String> profileFacets(UserProfile profile, String explicitQuery) {
        List<String> facets = new ArrayList<>();
        if (explicitQuery != null && !explicitQuery.isBlank()) {
            facets.add("本次关注主题：" + explicitQuery.trim());
        }
        addFacets(facets, "兴趣：", profile.interests());
        addFacets(facets, "当前项目：", profile.currentProjects());
        addFacets(facets, "技术栈：", profile.techStack());
        addFacets(facets, "目标：", profile.goals());
        if (profile.contextSummary() != null && !profile.contextSummary().isBlank()) {
            facets.add("上下文：" + profile.contextSummary().trim());
        }
        return facets;
    }

    private void addFacets(List<String> facets, String prefix, List<String> values) {
        if (values == null) return;
        values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .map(value -> prefix + value)
                .forEach(facets::add);
    }

    /**
     * 余弦相似度
     */
    private float cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return 0f;

        double dotProduct = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator == 0) return 0f;

        return (float) (dotProduct / denominator);
    }
}
