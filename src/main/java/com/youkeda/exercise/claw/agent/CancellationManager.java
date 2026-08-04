package com.youkeda.exercise.claw.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 用户任务取消状态管理器。
 *
 * <p>职责：
 * <ul>
 *   <li>保存每个 userId 的当前取消状态</li>
 *   <li>支持设置取消标记 {@link #cancel(String)}</li>
 *   <li>支持查询取消状态 {@link #isCancelled(String)}</li>
 *   <li>支持清除取消标记 {@link #clear(String)}</li>
 * </ul>
 *
 * <p>线程安全：基于 {@link ConcurrentHashMap} 和 {@link AtomicBoolean}。
 * 不引入 Redis，纯进程内内存存储。
 */
@Component
public class CancellationManager {

    private static final Logger log = LoggerFactory.getLogger(CancellationManager.class);

    private final ConcurrentHashMap<String, AtomicBoolean> cancellations = new ConcurrentHashMap<>();

    /**
     * 设置指定用户的取消标记。
     *
     * @param userId 用户标识
     */
    public void cancel(String userId) {
        cancellations.computeIfAbsent(userId, k -> new AtomicBoolean()).set(true);
        log.info("用户取消标记已设置 | userId={}", userId);
    }

    /**
     * 查询指定用户是否已被取消。
     *
     * @param userId 用户标识
     * @return true 表示已取消
     */
    public boolean isCancelled(String userId) {
        AtomicBoolean flag = cancellations.get(userId);
        return flag != null && flag.get();
    }

    /**
     * 清除指定用户的取消标记（新任务开始前调用）。
     *
     * @param userId 用户标识
     */
    public void clear(String userId) {
        AtomicBoolean flag = cancellations.get(userId);
        if (flag != null) {
            flag.set(false);
            log.debug("用户取消标记已清除 | userId={}", userId);
        }
    }
}