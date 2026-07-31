package com.youkeda.exercise.claw.agent.skill;

import org.springframework.stereotype.Component;
import java.util.Optional;

@Component("scoutTriggerPolicy")
public class ScoutTriggerPolicy implements SkillTriggerPolicy {

    @Override
    public SkillTriggerMatch match(String message, Optional<SkillSession> session) {
        if (message == null || message.isBlank()) return SkillTriggerMatch.noMatch();

        // 委托给旧的高精度策略（正则 + 否定检测 + 非请求过滤）
        boolean triggered = com.youkeda.exercise.claw.feature.scout.ScoutTriggerPolicy.hasExplicitRequest(message);
        if (triggered) {
            return new SkillTriggerMatch(true, 0.9, "scout explicit request", false);
        }
        return SkillTriggerMatch.noMatch();
    }
}
