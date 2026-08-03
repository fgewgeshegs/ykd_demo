package com.youkeda.exercise.claw.agent.skill;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelTriggerPolicyTest {

    private final TravelTriggerPolicy policy = new TravelTriggerPolicy();

    @Test
    void matchesDestinationAndMultiDayTripPlanningRequest() {
        SkillTriggerMatch match = policy.match(
                "帮我规划一个新疆三日游，8月5号从杭州出发",
                Optional.empty());

        assertTrue(match.matched());
    }

    @Test
    void matchesHundredDayTripRequest() {
        assertTrue(policy.match("新疆一百天游", Optional.empty()).matched());
    }

    @Test
    void doesNotMatchGenericStudyPlan() {
        SkillTriggerMatch result = policy.match(
                "帮我规划一下408复习方案", Optional.empty());

        assertFalse(result.matched());
    }

    @Test
    void keepsExplicitReplanCommandInActiveTravelSession() {
        SkillSession session = SkillSession.create("owner").withActiveSkill("travel");

        SkillTriggerMatch result = policy.match("重新规划，换个目的地", Optional.of(session));

        assertTrue(result.matched());
    }

    @Test
    void keepsDurationRevisionInActiveTravelSession() {
        SkillSession session = SkillSession.create("owner").withActiveSkill("travel");

        SkillTriggerMatch result = policy.match("改成5天", Optional.of(session));

        assertTrue(result.matched());
    }

    @Test
    void resumesSuspendedTravelBudgetAfterAnotherSkill() {
        SkillSession weather = SkillSession.create("owner")
                .withActiveSkill("travel")
                .withPendingAction(
                        SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS,
                        "budget")
                .withActiveSkill("weather");

        SkillTriggerMatch result = policy.match("2000元", Optional.of(weather));

        assertTrue(result.matched());
    }

    @Test
    void doesNotMatchTravelKnowledgeQuestion() {
        assertFalse(policy.match("旅游签证怎么办", Optional.empty()).matched());
        assertFalse(policy.match("去美国旅游需要什么签证", Optional.empty()).matched());
        assertFalse(policy.match("推荐几个北京旅游景点", Optional.empty()).matched());
        assertFalse(policy.match("解释一下行程字段", Optional.empty()).matched());
        assertFalse(policy.match("三日游是什么意思", Optional.empty()).matched());
        assertFalse(policy.match("三日游有哪些经典路线", Optional.empty()).matched());
    }

    @Test
    void travelSkillUsesDedicatedTriggerPolicy() throws Exception {
        String config = new String(
                new ClassPathResource("config/skills.yml").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);

        assertTrue(config.matches("(?s).*travel:.*?triggerPolicyName:\\s*travelTriggerPolicy.*"));
    }
}
