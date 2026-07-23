package com.youkeda.exercise.claw.teamtrip;

/** 多候选方案的用户选择状态。 */
public enum OptionDecisionStatus {
    NO_OPTIONS,
    OPTIONS_GENERATED,
    OPTIONS_COSTED,
    AWAITING_OPTION_SELECTION,
    OPTION_SELECTED,
    OPTION_COMBINING,
    FINALIZED
}
