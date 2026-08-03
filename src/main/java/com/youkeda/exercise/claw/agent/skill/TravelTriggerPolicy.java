package com.youkeda.exercise.claw.agent.skill;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

/** 旅游规划专用触发策略，避免依赖宽泛的“方案/规划”单关键词。 */
@Component("travelTriggerPolicy")
public class TravelTriggerPolicy implements SkillTriggerPolicy {

    private static final String NUMBER = "[零一二三四五六七八九十百千万两\\d]+";

    private static final Pattern TRAVEL_REQUEST = Pattern.compile(
            "(?:" + NUMBER + ")[日天](?:游|行程)"
                    + "|(?:去|到).{1,16}(?:玩|逛|旅游|旅行)(?:" + NUMBER + ")[日天]"
                    + "|(?:规划|安排|制定|设计|生成|策划|准备|做)"
                    + ".{0,20}(?:旅游|旅行|出游|行程|攻略)");
    private static final Pattern ACTIVE_TRAVEL_CONTINUATION = Pattern.compile(
            "重新规划|再做(?:一个|一份)?(?:行程|方案)?|换(?:个|一个)?(?:地方|目的地)"
                    + "|(?:修改|调整|改成|改为|优化|细化).{0,12}"
                    + "(?:行程|预算|人数|出发|目的地|日期|天数|住宿|酒店|交通|景点|"
                    + NUMBER + "天)"
                    + "|(?:行程|预算|人数|出发|目的地|日期|天数|住宿|酒店|交通|景点)"
                    + ".{0,12}(?:修改|调整|改)");
    private static final Pattern TRAVEL_KNOWLEDGE_QUESTION = Pattern.compile(
            "是什么意思|什么含义|有哪些经典路线|有哪(?:些|几条).{0,8}路线");
    private static final Pattern MONEY_RESPONSE = Pattern.compile(
            "(?:预算|人均)?\\s*\\d+(?:\\.\\d+)?\\s*(?:元|块|万)?");
    private static final Pattern PEOPLE_RESPONSE = Pattern.compile(
            "(?:[一二三四五六七八九十两\\d]+个?人|自己|独自)");

    @Override
    public SkillTriggerMatch match(String message, Optional<SkillSession> currentSession) {
        if (message == null || message.isBlank()) {
            return SkillTriggerMatch.noMatch();
        }
        boolean activeTravel = currentSession
                .map(session -> "travel".equals(session.activeSkill()))
                .orElse(false);
        if (currentSession
                .filter(session -> session.hasSuspendedPendingAction(
                        "travel", SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS))
                .filter(session -> matchesSuspendedSlot(
                        message, session.suspendedPendingSlot("travel")))
                .isPresent()) {
            return new SkillTriggerMatch(
                    true, 0.95, "suspended travel pending response", true);
        }
        if (activeTravel && ACTIVE_TRAVEL_CONTINUATION.matcher(message).find()) {
            return new SkillTriggerMatch(
                    true, 0.92, "active travel continuation", true);
        }
        if (TRAVEL_KNOWLEDGE_QUESTION.matcher(message).find()) {
            return SkillTriggerMatch.noMatch();
        }
        if (!TRAVEL_REQUEST.matcher(message).find()) {
            return SkillTriggerMatch.noMatch();
        }
        return new SkillTriggerMatch(true, 0.9, "travel planning intent", false);
    }

    private boolean matchesSuspendedSlot(String message, String slot) {
        String normalized = message == null ? "" : message.trim();
        if (normalized.isEmpty() || slot == null) return false;
        return switch (slot) {
            case "budget", "budget_total", "budget_per_person" ->
                    MONEY_RESPONSE.matcher(normalized).matches();
            case "participant_count" -> PEOPLE_RESPONSE.matcher(normalized).matches();
            case "departure_city" -> normalized.matches("(?:从)?[\\p{IsHan}]{2,10}(?:出发)?");
            case "destination_or_scope", "destination", "travel_scope" ->
                    normalized.matches("(?:去|到)?[\\p{IsHan}]{2,16}");
            case "travel_date" -> normalized.matches(".*(?:月|日|号|今天|明天|后天|周|星期).*?");
            case "duration" -> normalized.matches(".*[天日晚].*");
            default -> false;
        };
    }
}
