package com.youkeda.exercise.claw.feature.campus;

import com.youkeda.exercise.claw.notification.NotificationSource;

/**
 * 校园通知 Source 标记接口。
 *
 * <p>用于按类型隔离通知源：{@link com.youkeda.exercise.claw.feature.campus.CampusNotifier}
 * 只注入 {@code List<CampusSource>}，避免把所有 NotificationSource（含动漫 AnimeSource /
 * AnimeSeasonSource）都当校园源跑——那是注入语义错误，导致动漫每天被校园调度重复执行。
 *
 * <p>新增通知类型时：校园域实现此接口，非校园域（动漫/天气/信息猎手等）实现
 * {@link NotificationSource} 即可，天然被校园调度排除。
 */
public interface CampusSource extends NotificationSource {
}
