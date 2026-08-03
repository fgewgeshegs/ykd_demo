package com.youkeda.exercise.claw.feature.travel;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 默认旅游方案状态存储（基于内存），按 userId 隔离。 */
@Component
@ConditionalOnMissingBean(TravelPlanStateStore.class)
public class DefaultTravelPlanStateStore implements TravelPlanStateStore {

    private final Map<String, TravelPlanDraft> stores = new ConcurrentHashMap<>();

    @Override
    public TravelPlanDraft get(String userId) {
        return stores.get(userId);
    }

    @Override
    public void save(String userId, TravelPlanDraft draft) {
        stores.put(userId, draft);
    }

    @Override
    public void clear(String userId) {
        stores.remove(userId);
    }
}
