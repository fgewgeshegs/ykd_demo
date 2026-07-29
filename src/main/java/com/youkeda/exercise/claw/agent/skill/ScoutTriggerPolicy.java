package com.youkeda.exercise.claw.agent.skill;

import org.springframework.stereotype.Component;
import java.util.Optional;

@Component("scoutTriggerPolicy")
public class ScoutTriggerPolicy implements SkillTriggerPolicy {

    private static final java.util.Set<String> TRIGGERS = java.util.Set.of(
            "帮我找", "有什么新消息", "搜搜看", "关注", "搜索", "查一下", "帮我查查");

    private static final java.util.Set<String> EXCLUDE = java.util.Set.of(
            "天气", "旅游", "路线", "打车", "怎么去", "多少钱", "价格");

    @Override
    public SkillTriggerMatch match(String message, Optional<SkillSession> session) {
        if (message == null || message.isBlank()) return SkillTriggerMatch.noMatch();

        // Check exclusion first — "帮我查一下天气" should not trigger scout
        for (String ex : EXCLUDE) {
            if (message.contains(ex)) return SkillTriggerMatch.noMatch();
        }

        for (String trigger : TRIGGERS) {
            if (message.contains(trigger)) {
                return new SkillTriggerMatch(true, 0.8, "scout trigger: " + trigger, false);
            }
        }

        return SkillTriggerMatch.noMatch();
    }
}
