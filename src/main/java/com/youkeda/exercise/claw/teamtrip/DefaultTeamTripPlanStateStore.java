package com.youkeda.exercise.claw.teamtrip;

import org.springframework.stereotype.Component;

/** 默认团建方案状态存储（基于内存）。单用户场景忽略 userId。 */
@Component
public class DefaultTeamTripPlanStateStore implements TeamTripPlanStateStore {

    private volatile TeamTripPlanDraft store;

    @Override
    public TeamTripPlanDraft get(String userId) {
        return store;
    }

    @Override
    public void save(String userId, TeamTripPlanDraft draft) {
        this.store = draft;
    }

    @Override
    public void clear(String userId) {
        this.store = null;
    }
}
