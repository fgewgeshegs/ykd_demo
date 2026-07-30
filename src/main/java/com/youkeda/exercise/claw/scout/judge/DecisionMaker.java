package com.youkeda.exercise.claw.scout.judge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.scout.ScoutProperties;
import com.youkeda.exercise.claw.scout.context.UserProfile;
import com.youkeda.exercise.claw.scout.matcher.MatchedCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 推荐决策器
 *
 * 用 LLM 判断候选信息是否值得推荐给用户
 */
@Service
public class DecisionMaker {

    private static final Logger log = LoggerFactory.getLogger(DecisionMaker.class);
    private static final int FIRST_ATTEMPT_TOKENS = 1200;
    private static final int RETRY_TOKENS = 1800;

    private static final String SYSTEM_PROMPT = """
            你是信息推荐决策专家。判断以下候选信息是否值得推荐给用户。

            要求：
            1. 候选已经通过新鲜度和相关度初筛，不要过度保守
            2. 推荐原因和建议行动各不超过20个字
            3. 列表中出现的信息即表示推荐，不要输出未推荐信息
            4. relevanceScore 0.0-1.0，越高越值得推荐
            5. 优先覆盖不同来源和不同角度，避免内容同质化

            返回严格的 JSON 数组格式：
            [
              {
                "index": 0,
                "reason": "推荐原因",
                "suggestion": "建议行动",
                "relevanceScore": 0.85
              }
            ]

            不要重复标题、摘要和来源。只有候选明显无关或内容无效时才不推荐。
            """;

    private final LLMClient llmClient;
    private final ScoutProperties props;
    private final ObjectMapper objectMapper;

    public DecisionMaker(LLMClient llmClient, ScoutProperties props, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    /**
     * LLM 判断是否推荐
     */
    public List<Recommendation> judge(UserProfile profile,
                                       List<MatchedCandidate> candidates) {
        if (candidates.isEmpty()) return List.of();

        try {
            String json = llmClient.chatWithSystemPrompt(
                    SYSTEM_PROMPT, buildPrompt(profile, candidates), FIRST_ATTEMPT_TOKENS);
            List<Recommendation> recommendations = parseValidRecommendations(json, candidates);

            if (recommendations == null) {
                log.warn("价值判断首次响应为空或格式无效，使用紧凑 Prompt 重试");
                json = llmClient.chatWithSystemPrompt(
                        SYSTEM_PROMPT, buildCompactPrompt(profile, candidates), RETRY_TOKENS);
                recommendations = parseValidRecommendations(json, candidates);
            }

            if (recommendations == null) {
                log.warn("价值判断重试仍无有效响应，使用已排序候选补足推荐");
                return ensureMinimumRecommendations(List.of(), candidates);
            }

            // 先限制模型输出，再用已经通过初筛的高分候选补足基础信息量。
            if (recommendations.size() > props.getMaxRecommendations()) {
                recommendations = recommendations.subList(0, props.getMaxRecommendations());
            }
            recommendations = ensureMinimumRecommendations(recommendations, candidates);
            recommendations.sort(
                    Comparator.comparingDouble(Recommendation::relevanceScore).reversed());

            log.info("推荐决策完成 | candidates={} | recommended={}",
                    candidates.size(), recommendations.size());

            return recommendations;
        } catch (Exception e) {
            log.error("推荐决策失败，使用已排序候选补足推荐", e);
            return ensureMinimumRecommendations(List.of(), candidates);
        }
    }

    private String buildPrompt(UserProfile profile, List<MatchedCandidate> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户画像：\n");
        sb.append(profile.toText()).append("\n");
        sb.append("候选信息（共").append(candidates.size()).append("条）：\n\n");

        for (int i = 0; i < candidates.size(); i++) {
            MatchedCandidate c = candidates.get(i);
            sb.append("[").append(i).append("] ");
            sb.append("标题：").append(c.item().getTitle()).append("\n");
            sb.append("    内容：").append(truncate(c.item().getContent(), 200)).append("\n");
            sb.append("    来源：").append(c.item().getSource()).append("\n");
            sb.append("    类型：").append(c.item().getCategory()).append("\n");
            sb.append("    匹配度：").append(String.format("%.2f", c.semanticScore())).append("\n\n");
        }

        int maxCount = Math.min(props.getMaxRecommendations(), candidates.size());
        int targetMin = Math.min(props.getMinRecommendations(), maxCount);
        sb.append("请推荐 ").append(targetMin).append("-").append(maxCount)
                .append(" 条；仅排除明显无关、无效或重复的信息。");
        return sb.toString();
    }

    private String buildCompactPrompt(UserProfile profile,
                                      List<MatchedCandidate> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户画像：\n").append(profile.toText()).append("\n");
        sb.append("候选信息：\n");
        for (int i = 0; i < candidates.size(); i++) {
            MatchedCandidate candidate = candidates.get(i);
            sb.append("[").append(i).append("] ")
                    .append(candidate.item().getTitle()).append("\n")
                    .append("摘要：").append(truncate(candidateSummary(candidate), 100)).append("\n")
                    .append("匹配度：")
                    .append(String.format("%.2f", candidate.semanticScore())).append("\n")
                    .append("匹配原因：").append(candidate.matchReason()).append("\n\n");
        }
        int maxCount = Math.min(props.getMaxRecommendations(), candidates.size());
        int targetMin = Math.min(props.getMinRecommendations(), maxCount);
        sb.append("请直接返回严格 JSON 数组，推荐 ")
                .append(targetMin).append("-").append(maxCount)
                .append(" 条。不要输出分析过程、Markdown 或额外文字。");
        return sb.toString();
    }

    /**
     * 返回 null 表示响应为空、没有可提取的 JSON，或非空结果中没有可解析推荐；
     * 合法的空数组 [] 返回空列表，代表模型明确没有选中强推荐。
     */
    private List<Recommendation> parseValidRecommendations(
            String json, List<MatchedCandidate> candidates) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonNode array = extractRecommendationArray(json);
            if (array == null) {
                logInvalidResponse(json);
                return null;
            }
            List<Recommendation> parsed = parseRecommendations(array, candidates);
            if (!array.isEmpty() && parsed.isEmpty()) return null;
            return parsed;
        } catch (Exception e) {
            logInvalidResponse(json);
            return null;
        }
    }

    private List<Recommendation> ensureMinimumRecommendations(
            List<Recommendation> modelRecommendations,
            List<MatchedCandidate> candidates) {
        int maxCount = Math.min(props.getMaxRecommendations(), candidates.size());
        int targetMin = Math.min(props.getMinRecommendations(), maxCount);
        List<Recommendation> completed = new ArrayList<>(modelRecommendations);
        Set<String> selected = new HashSet<>();
        completed.forEach(rec -> selected.add(recommendationKey(rec.source(), rec.title())));

        long now = System.currentTimeMillis();
        for (MatchedCandidate candidate : candidates) {
            if (completed.size() >= targetMin || completed.size() >= maxCount) break;
            String key = recommendationKey(
                    candidate.item().getSource(), candidate.item().getTitle());
            if (!selected.add(key)) continue;
            completed.add(new Recommendation(
                    UUID.randomUUID().toString(),
                    candidate.item().getTitle(),
                    candidateSummary(candidate),
                    candidate.matchReason(),
                    "快速浏览原文",
                    candidate.item().getSource(),
                    candidate.semanticScore(),
                    Recommendation.Tier.DISCOVERY,
                    now));
        }
        return completed;
    }

    private String recommendationKey(String source, String title) {
        return (source == null ? "" : source) + "\u0000"
                + (title == null ? "" : title);
    }

    private List<Recommendation> parseRecommendations(
            JsonNode arr, List<MatchedCandidate> candidates) {
        List<Recommendation> result = new ArrayList<>();
        try {
            long now = System.currentTimeMillis();
            for (JsonNode node : arr) {
                if (node.has("recommended")
                        && !node.path("recommended").asBoolean(false)) continue;

                int index = node.path("index").asInt(-1);
                if (index < 0 || index >= candidates.size()) continue;

                MatchedCandidate candidate = candidates.get(index);
                Recommendation rec = new Recommendation(
                        UUID.randomUUID().toString(),
                        node.path("title").asText(candidate.item().getTitle()),
                        node.path("summary").asText(candidateSummary(candidate)),
                        node.path("reason").asText(""),
                        node.path("suggestion").asText(""),
                        candidate.item().getSource(),
                        (float) node.path("relevanceScore").asDouble(candidate.semanticScore()),
                        Recommendation.Tier.STRONG,
                        now
                );
                result.add(rec);
            }
        } catch (Exception e) {
            log.error("推荐结果 JSON 解析失败", e);
        }
        return result;
    }

    private JsonNode extractRecommendationArray(String responseText) throws Exception {
        String jsonText = extractJson(responseText);
        JsonNode root = objectMapper.readTree(jsonText);
        if (root == null) return null;
        if (root.isArray()) return root;
        if (!root.isObject()) return null;

        JsonNode wrapped = root.get("recommendations");
        if (wrapped != null && wrapped.isArray()) {
            return wrapped;
        }
        if (root.has("index")) {
            return objectMapper.createArrayNode().add(root);
        }
        return null;
    }

    private void logInvalidResponse(String response) {
        String compact = response == null
                ? ""
                : response.replaceAll("\\s+", " ").trim();
        log.warn("价值判断响应不是可识别的 JSON 推荐结果 | response={}",
                truncate(compact, 300));
    }

    private String candidateSummary(MatchedCandidate candidate) {
        String summary = candidate.item().getSummary();
        if (summary != null && !summary.isBlank()) {
            return summary;
        }
        return truncate(candidate.item().getContent(), 100);
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    private String extractJson(String text) {
        String trimmed = text.replaceAll("(?s)<think>.*?</think>", "").trim();

        int arrayStart = trimmed.indexOf('[');
        int arrayEnd = trimmed.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            return trimmed.substring(arrayStart, arrayEnd + 1);
        }

        int objectStart = trimmed.indexOf('{');
        int objectEnd = trimmed.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return trimmed.substring(objectStart, objectEnd + 1);
        }
        return trimmed;
    }
}
