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
import java.util.List;
import java.util.UUID;

/**
 * 推荐决策器
 *
 * 用 LLM 判断候选信息是否值得推荐给用户
 */
@Service
public class DecisionMaker {

    private static final Logger log = LoggerFactory.getLogger(DecisionMaker.class);

    private static final String SYSTEM_PROMPT = """
            你是信息推荐决策专家。判断以下候选信息是否值得推荐给用户。

            要求：
            1. 只推荐真正有价值的信息，宁缺毋滥
            2. 推荐原因要结合用户的具体情况
            3. 建议行动要具体可执行
            4. relevanceScore 0.0-1.0，越高越值得推荐

            返回严格的 JSON 数组格式：
            [
              {
                "index": 0,
                "recommended": true,
                "title": "信息标题",
                "summary": "一句话摘要",
                "reason": "推荐原因",
                "suggestion": "建议行动",
                "relevanceScore": 0.85
              }
            ]

            只返回推荐的（recommended=true）信息。如果都不值得推荐，返回空数组 []。
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

        String prompt = buildPrompt(profile, candidates);

        try {
            String json = llmClient.chatWithSystemPrompt(SYSTEM_PROMPT, prompt);
            if (json == null || json.isBlank()) {
                log.warn("LLM 返回空，跳过推荐");
                return List.of();
            }

            List<Recommendation> recommendations = parseRecommendations(json, candidates);

            // 截断到最大推荐数
            if (recommendations.size() > props.getMaxRecommendations()) {
                recommendations = recommendations.subList(0, props.getMaxRecommendations());
            }

            log.info("推荐决策完成 | candidates={} | recommended={}",
                    candidates.size(), recommendations.size());

            return recommendations;
        } catch (Exception e) {
            log.error("推荐决策失败", e);
            return List.of();
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

        sb.append("请判断哪些信息值得推荐（最多").append(props.getMaxRecommendations()).append("条）。");
        return sb.toString();
    }

    private List<Recommendation> parseRecommendations(String json,
                                                       List<MatchedCandidate> candidates) {
        List<Recommendation> result = new ArrayList<>();
        try {
            String jsonStr = extractJson(json);
            JsonNode arr = objectMapper.readTree(jsonStr);
            if (!arr.isArray()) return result;

            long now = System.currentTimeMillis();
            for (JsonNode node : arr) {
                if (!node.path("recommended").asBoolean(false)) continue;

                int index = node.path("index").asInt(-1);
                if (index < 0 || index >= candidates.size()) continue;

                MatchedCandidate candidate = candidates.get(index);
                Recommendation rec = new Recommendation(
                        UUID.randomUUID().toString(),
                        node.path("title").asText(candidate.item().getTitle()),
                        node.path("summary").asText(truncate(candidate.item().getContent(), 100)),
                        node.path("reason").asText(""),
                        node.path("suggestion").asText(""),
                        candidate.item().getSource(),
                        (float) node.path("relevanceScore").asDouble(candidate.semanticScore()),
                        now
                );
                result.add(rec);
            }
        } catch (Exception e) {
            log.error("推荐结果 JSON 解析失败", e);
        }
        return result;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    private String extractJson(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('[');
            int end = trimmed.lastIndexOf(']');
            if (start >= 0 && end > start) {
                return trimmed.substring(start, end + 1);
            }
        }
        return trimmed;
    }
}
