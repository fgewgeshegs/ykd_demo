package com.youkeda.exercise.claw.scout.matcher;

import com.youkeda.exercise.claw.agent.memory.longterm.EmbeddingClient;
import com.youkeda.exercise.claw.scout.ScoutProperties;
import com.youkeda.exercise.claw.scout.context.UserProfile;
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
    public List<MatchedCandidate> match(String userId, UserProfile profile,
                                         List<InformationItem> items) {
        if (items.isEmpty()) return List.of();

        try {
            // 1. 用户兴趣 → embedding
            String interestText = profile.toText();
            if (interestText.isBlank()) {
                // 无画像时返回全部（按采集时间）
                log.info("用户画像为空，返回全部候选 | userId={}", userId);
                return items.stream()
                        .filter(item -> item.getVector() != null)
                        .limit(props.getMaxCandidates())
                        .map(item -> new MatchedCandidate(item, 0.5f, "通用推荐"))
                        .toList();
            }

            float[] userVector = embeddingClient.embed(interestText);

            // 2. 计算相似度
            List<MatchedCandidate> candidates = new ArrayList<>();
            for (InformationItem item : items) {
                if (item.getVector() == null) continue;

                float score = cosineSimilarity(userVector, item.getVector());
                if (score >= props.getMinMatchScore()) {
                    candidates.add(new MatchedCandidate(item, score, ""));
                }
            }

            // 3. 排序 + 截断
            candidates.sort(Comparator.comparingDouble(MatchedCandidate::semanticScore).reversed());
            List<MatchedCandidate> topK = candidates.stream()
                    .limit(props.getMaxCandidates())
                    .toList();

            log.info("语义匹配完成 | userId={} | total={} | matched={}",
                    userId, items.size(), topK.size());

            return topK;
        } catch (Exception e) {
            log.error("语义匹配失败 | userId={}", userId, e);
            return List.of();
        }
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
