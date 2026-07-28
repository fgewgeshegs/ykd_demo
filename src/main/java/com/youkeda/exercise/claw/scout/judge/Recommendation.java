package com.youkeda.exercise.claw.scout.judge;

/**
 * 推荐结果
 */
public record Recommendation(
        String id,
        String userId,
        String title,
        String summary,
        String reason,
        String suggestion,
        String source,
        float relevanceScore,
        long createdAt
) {}
