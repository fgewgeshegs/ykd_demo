package com.youkeda.exercise.claw.teamtrip;

import org.springframework.stereotype.Component;

/** 默认团建方案状态存储（基于内存）。SQLite 版本将在后续阶段实现。 */
@Component
public class DefaultTeamTripPlanStateStore implements TeamTripPlanStateStore {

    private volatile TeamTripPlanDraft store;

    @Override
    public TeamTripPlanDraft get() {
        return store;
    }

    @Override
    public void save(TeamTripPlanDraft draft) {
        this.store = draft;
    }

    @Override
    public void clear() {
        this.store = null;
    }
}
