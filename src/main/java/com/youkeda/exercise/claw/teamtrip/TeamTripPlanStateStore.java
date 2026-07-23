package com.youkeda.exercise.claw.teamtrip;

/** 团建方案状态存储。 */
public interface TeamTripPlanStateStore {

    TeamTripPlanDraft get(String userId);

    void save(String userId, TeamTripPlanDraft draft);

    void clear(String userId);
}
