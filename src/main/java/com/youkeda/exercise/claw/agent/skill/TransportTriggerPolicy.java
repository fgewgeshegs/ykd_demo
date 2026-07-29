package com.youkeda.exercise.claw.agent.skill;

import org.springframework.stereotype.Component;
import java.util.Optional;

@Component("transportTriggerPolicy")
public class TransportTriggerPolicy implements SkillTriggerPolicy {

    private static final java.util.Set<String> TRANSPORT_VERBS = java.util.Set.of(
            "打车", "叫车", "代驾", "坐车", "怎么去");

    private static final java.util.Set<String> SEARCH_VERBS = java.util.Set.of(
            "搜索", "查一下", "有什么", "推荐");

    @Override
    public SkillTriggerMatch match(String message, Optional<SkillSession> session) {
        if (message == null || message.isBlank()) return SkillTriggerMatch.noMatch();

        boolean hasTransportVerb = TRANSPORT_VERBS.stream().anyMatch(message::contains);
        if (!hasTransportVerb) return SkillTriggerMatch.noMatch();

        // Has location word + transport verb -> high confidence transport
        boolean hasPlace = message.contains("西湖") || message.contains("上海")
                || message.contains("北京") || message.contains("杭州")
                || message.contains("广州") || message.contains("深圳")
                || message.contains("地铁") || message.contains("公交");

        // Has search verb -> lower confidence (might be scout)
        boolean hasSearchVerb = SEARCH_VERBS.stream().anyMatch(message::contains);

        if (hasTransportVerb && hasPlace && !hasSearchVerb) {
            return new SkillTriggerMatch(true, 0.9, "transport: place + verb", false);
        } else if (hasTransportVerb) {
            return new SkillTriggerMatch(true, 0.75, "transport: verb only", false);
        }

        return SkillTriggerMatch.noMatch();
    }
}
