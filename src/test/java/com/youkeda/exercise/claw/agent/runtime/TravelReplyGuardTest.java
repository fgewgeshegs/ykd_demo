package com.youkeda.exercise.claw.agent.runtime;

import com.youkeda.exercise.claw.agent.skill.SkillPendingCoordinator;
import com.youkeda.exercise.claw.agent.skill.SkillSession;
import com.youkeda.exercise.claw.agent.skill.TravelTriggerPolicy;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelReplyGuardTest {

    private final TravelReplyGuard guard = new TravelReplyGuard(new TravelTriggerPolicy());

    @Test
    void rejectsCompletedPlanBeforeTravelCollectRuns() {
        SkillReplyGuard.GuardResult result = guard.validate(new SkillReplyGuard.GuardContext(
                "帮我规划一个新疆三日游，8月5号从杭州出发",
                "已经规划好了，第一天去乌鲁木齐。",
                SkillSession.create("owner").withActiveSkill("travel"),
                Set.of()));

        assertFalse(result.allowed());
    }

    @Test
    void allowsReplyAfterTravelCollectRuns() {
        SkillReplyGuard.GuardResult result = guard.validate(new SkillReplyGuard.GuardContext(
                "帮我规划一个新疆三日游，8月5号从杭州出发",
                "还需要确认出行人数。",
                SkillSession.create("owner").withActiveSkill("travel"),
                Set.of("travel_collect|{}")));

        assertTrue(result.allowed());
    }

    @Test
    void rejectsPendingSlotReplyWhenTravelCollectWasSkipped() {
        SkillSession session = SkillSession.create("owner")
                .withActiveSkill("travel")
                .withPendingAction(
                        SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS,
                        "participant_count");

        SkillReplyGuard.GuardResult result = guard.validate(new SkillReplyGuard.GuardContext(
                "一个人",
                "好的，我继续规划。",
                session,
                Set.of()));

        assertFalse(result.allowed());
    }

    @Test
    void rejectsCompletedPlanWhileTravelRequirementsAreStillMissing() {
        SkillSession session = SkillSession.create("owner")
                .withActiveSkill("travel")
                .withPendingAction(
                        SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS,
                        "participant_count");

        SkillReplyGuard.GuardResult result = guard.validate(new SkillReplyGuard.GuardContext(
                "帮我规划新疆三日游",
                "已经规划好了。Day 1 去乌鲁木齐，Day 2 去天池。",
                session,
                Set.of("travel_collect|{}")));

        assertFalse(result.allowed());
    }

    @Test
    void rejectsItineraryTextWhileTravelRequirementsAreStillMissing() {
        SkillSession session = SkillSession.create("owner")
                .withActiveSkill("travel")
                .withPendingAction(
                        SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS,
                        "participant_count");

        SkillReplyGuard.GuardResult result = guard.validate(new SkillReplyGuard.GuardContext(
                "帮我规划北京两天游",
                "上午游览故宫，下午前往颐和园，晚上逛王府井。",
                session,
                Set.of("travel_collect|{}")));

        assertFalse(result.allowed());
    }

    @Test
    void rejectsDayFourInThreeDayTrip() {
        SkillReplyGuard.GuardResult result = guard.validate(new SkillReplyGuard.GuardContext(
                "帮我规划新疆三日游",
                "Day 1 乌鲁木齐，Day 2 天池，Day 3 吐鲁番，Day 4 返回杭州。",
                SkillSession.create("owner").withActiveSkill("travel"),
                Set.of("travel_collect|{}")));

        assertFalse(result.allowed());
    }

    @Test
    void doesNotMistakeCalendarDayForTripDuration() {
        SkillReplyGuard.GuardResult result = guard.validate(new SkillReplyGuard.GuardContext(
                "帮我规划8月5日出发的新疆三日游",
                "Day 1 乌鲁木齐，Day 2 天池，Day 3 吐鲁番，Day 4 返回杭州。",
                SkillSession.create("owner").withActiveSkill("travel"),
                Set.of("travel_collect|{}")));

        assertFalse(result.allowed());
    }

    @Test
    void rejectsBudgetSummaryBeforeCostCalculatorRuns() {
        SkillReplyGuard.GuardResult result = guard.validate(new SkillReplyGuard.GuardContext(
                "帮我规划新疆三日游",
                "预计总费用5000元，人均费用2500元，在预算内。",
                SkillSession.create("owner").withActiveSkill("travel"),
                Set.of("travel_collect|{}")));

        assertFalse(result.allowed());
    }

    @Test
    void rejectsThirdDayInTwoDayTripWrittenWithLiang() {
        SkillReplyGuard.GuardResult result = guard.validate(new SkillReplyGuard.GuardContext(
                "帮我规划北京两天游",
                "第一天故宫，第二天长城，第三天返程。",
                SkillSession.create("owner").withActiveSkill("travel"),
                Set.of("travel_collect|{}")));

        assertFalse(result.allowed());
    }

    @Test
    void allowsEleventhDayInElevenDayTrip() {
        SkillReplyGuard.GuardResult result = guard.validate(new SkillReplyGuard.GuardContext(
                "帮我规划新疆十一天游",
                "第一天抵达，第十天游览，第十一天返程。",
                SkillSession.create("owner").withActiveSkill("travel"),
                Set.of("travel_collect|{}")));

        assertTrue(result.allowed());
    }

    @Test
    void rejectsOversizedDayNumberWithoutThrowing() {
        SkillReplyGuard.GuardResult result = assertDoesNotThrow(() -> guard.validate(
                new SkillReplyGuard.GuardContext(
                        "规划新疆3天游",
                        "第999999999999999999999999天返程。",
                        SkillSession.create("owner").withActiveSkill("travel"),
                        Set.of("travel_collect|{}"))));

        assertFalse(result.allowed());
    }

    @Test
    void rejectsHundredAndFirstDayForHundredDayRequest() {
        SkillReplyGuard.GuardResult result = guard.validate(
                new SkillReplyGuard.GuardContext(
                        "帮我规划一百天的旅行",
                        "第一百零一天返程。",
                        SkillSession.create("owner").withActiveSkill("travel"),
                        Set.of("travel_collect|{}")));

        assertFalse(result.allowed());
    }

    @Test
    void comparesOversizedRequestedAndReplyDaysExactly() {
        SkillReplyGuard.GuardResult result = guard.validate(
                new SkillReplyGuard.GuardContext(
                        "规划999999999999999999999999天游",
                        "第1000000000000000000000000天返程。",
                        SkillSession.create("owner").withActiveSkill("travel"),
                        Set.of("travel_collect|{}")));

        assertFalse(result.allowed());
    }
}
