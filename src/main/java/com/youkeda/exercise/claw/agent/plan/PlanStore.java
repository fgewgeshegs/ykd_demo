package com.youkeda.exercise.claw.agent.plan;

import com.youkeda.exercise.claw.agent.model.PlanState;

/**
 * PlanState 的读写接口。
 *
 * <p>Session 隔离通过 {@code userId} key 实现。
 */
public interface PlanStore {

    /** 获取当前会话的 PlanState，没有则返回 null */
    PlanState get(String userId);

    /** 保存（创建或更新）PlanState */
    void save(String userId, PlanState state);

    /** 清理会话 PlanState */
    void clear(String userId);
}
