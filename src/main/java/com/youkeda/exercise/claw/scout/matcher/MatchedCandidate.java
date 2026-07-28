package com.youkeda.exercise.claw.scout.matcher;

import com.youkeda.exercise.claw.scout.processor.InformationItem;

/**
 * 匹配候选结果
 */
public record MatchedCandidate(
        InformationItem item,
        float semanticScore,
        String matchReason
) {}
