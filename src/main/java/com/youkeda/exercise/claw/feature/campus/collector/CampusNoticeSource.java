package com.youkeda.exercise.claw.feature.campus.collector;

/**
 * 校园通知来源身份标识。
 *
 * <p>取代各 Collector 内硬编码的 source 字符串，由调用方传入真实身份，
 * 避免一个 Collector 被多个 Source 复用时污染命名空间（如 Activity 源
 * 拿到 COMPETITION 身份）。
 *
 * <p>落库用 {@link #name()}，取值必须与历史字符串常量一致
 * （ACTIVITY / COMPETITION / EXAM / JOB），否则去重与查询断档。
 */
public enum CampusNoticeSource {
    ACTIVITY,
    COMPETITION,
    EXAM,
    JOB
}
