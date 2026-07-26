package com.youkeda.exercise.claw.agent.plan;

import com.youkeda.exercise.claw.agent.model.PlanState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 {@link ConcurrentHashMap} 的 PlanStore 实现。
 *
 * <p>线程安全，Session 隔离通过 {@code userId} key。
 */
@Component
public class DefaultPlanStore implements PlanStore {

    private static final Logger log = LoggerFactory.getLogger(DefaultPlanStore.class);

    private final ConcurrentHashMap<String, PlanState> store = new ConcurrentHashMap<>();

    @Override
    public PlanState get(String userId) {
        return store.get(userId);
    }

    @Override
    public void save(String userId, PlanState state) {
        store.put(userId, state);
        log.debug("PlanState 已保存 | user={} | version={}", userId, state != null ? state.getVersion() : "null");
    }

    @Override
    public void clear(String userId) {
        store.remove(userId);
        log.debug("PlanState 已清除 | user={}", userId);
    }
}
