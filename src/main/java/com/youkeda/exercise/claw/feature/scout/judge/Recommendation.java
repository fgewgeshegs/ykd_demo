package com.youkeda.exercise.claw.feature.scout.judge;

/**
 * 推荐结果
 */
public record Recommendation(
        String id,
        String title,
        String summary,
        String reason,
        String suggestion,
        String source,
        float relevanceScore,
        Tier tier,
        long createdAt
) {
    public enum Tier {
        STRONG,
        DISCOVERY
    }

    /**
     * 兼容校园提醒等非 Scout 调用方：它们属于业务明确选中的强推荐。
     */
    public Recommendation(
            String id,
            String title,
            String summary,
            String reason,
            String suggestion,
            String source,
            float relevanceScore,
            long createdAt) {
        this(id, title, summary, reason, suggestion, source,
                relevanceScore, Tier.STRONG, createdAt);
    }
}
