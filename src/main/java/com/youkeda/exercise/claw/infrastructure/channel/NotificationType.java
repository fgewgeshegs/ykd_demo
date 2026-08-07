package com.youkeda.exercise.claw.infrastructure.channel;

/**
 * 通知类型枚举。
 *
 * <p>区分通知来源，便于按类型统计（如"本周课程提醒失败了几条"）。
 * 数据库存储 {@code type.name()}，与字符串完全兼容。
 */
public enum NotificationType {
    GENERAL,
    COURSE_REMINDER,
    AGENT_RESULT,
    NEWS_REPORT,
    CAMPUS_NOTICE,
    ANIME_REMINDER,
    TASK_REMINDER
}
