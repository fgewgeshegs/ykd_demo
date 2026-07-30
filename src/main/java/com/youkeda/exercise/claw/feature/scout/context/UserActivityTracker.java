package com.youkeda.exercise.claw.feature.scout.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户活跃时间追踪器
 *
 * 记录用户发送消息的时间，分析最活跃时段，用于优化推送时间
 */
@Component
public class UserActivityTracker {

    private static final Logger log = LoggerFactory.getLogger(UserActivityTracker.class);

    /** 保留最近 100 条活跃记录 */
    private static final int MAX_RECORDS = 100;

    private final List<Long> activityLog = new ArrayList<>();

    /**
     * 记录用户活跃时间戳
     */
    public synchronized void record() {
        activityLog.add(System.currentTimeMillis());

        // 限制记录数量
        if (activityLog.size() > MAX_RECORDS) {
            activityLog.subList(0, activityLog.size() - MAX_RECORDS).clear();
        }
    }

    /**
     * 获取用户最活跃的小时（0-23）
     *
     * 分析最近的活跃记录，返回最常出现的小时
     *
     * @return 最活跃小时，无记录时返回默认值 8（早 8 点）
     */
    public synchronized int getMostActiveHour() {
        if (activityLog.isEmpty()) {
            return 8; // 默认早 8 点
        }

        // 统计每个小时的消息数
        int[] hourCount = new int[24];
        for (Long timestamp : activityLog) {
            LocalTime time = Instant.ofEpochMilli(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime();
            hourCount[time.getHour()]++;
        }

        // 找到最活跃的小时
        int maxHour = 8;
        int maxCount = 0;
        for (int i = 0; i < 24; i++) {
            if (hourCount[i] > maxCount) {
                maxCount = hourCount[i];
                maxHour = i;
            }
        }

        log.debug("用户最活跃时段 | hour={} | count={}", maxHour, maxCount);
        return maxHour;
    }

    /**
     * 判断当前是否是用户的活跃时段（±1 小时）
     */
    public boolean isActiveHour() {
        int activeHour = getMostActiveHour();
        int currentHour = LocalTime.now().getHour();

        // 允许 ±1 小时的误差
        int diff = Math.abs(currentHour - activeHour);
        if (diff > 12) diff = 24 - diff; // 跨日处理

        return diff <= 1;
    }

    /**
     * 获取用户活跃记录数
     */
    public synchronized int getRecordCount() {
        return activityLog.size();
    }
}
