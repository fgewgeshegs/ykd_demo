package com.youkeda.exercise.claw.teamtrip;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/** 内存版团建方案状态存储。 */
@Component
@ConditionalOnProperty(name = "context.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryTeamTripPlanStateStore implements TeamTripPlanStateStore {

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
