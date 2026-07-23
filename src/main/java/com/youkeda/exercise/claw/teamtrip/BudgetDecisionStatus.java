package com.youkeda.exercise.claw.teamtrip;

/** 方案成本与用户预算上限比较后的决策状态。 */
public enum BudgetDecisionStatus {
    NOT_REQUIRED,
    WITHIN_BUDGET,
    AWAITING_USER_DECISION,
    OVERRUN_ACCEPTED,
    REVISION_REQUIRED,
    LIMIT_UPDATED
}
