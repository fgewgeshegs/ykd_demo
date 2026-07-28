package com.youkeda.exercise.claw.teamtrip;

/** 团建方案状态存储。 */
public interface TeamTripPlanStateStore {

    TeamTripPlanDraft get();

    void save(TeamTripPlanDraft draft);

    void clear();
}
