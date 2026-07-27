package com.youkeda.exercise.claw.teamtrip;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/** 默认团建方案状态存储（基于内存）。SQLite 版本将在后续阶段实现。 */
@Component
public class DefaultTeamTripPlanStateStore implements TeamTripPlanStateStore {

    private final ConcurrentHashMap<String, TeamTripPlanDraft> store = new ConcurrentHashMap<>();

    @Override
    public TeamTripPlanDraft get(String userId) {
        return store.get(userId);
    }

    @Override
    public void save(String userId, TeamTripPlanDraft draft) {
        store.put(userId, draft);
    }

    @Override
    public void clear(String userId) {
        store.remove(userId);
    }
}
