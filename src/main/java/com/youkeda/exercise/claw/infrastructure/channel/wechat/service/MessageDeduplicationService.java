package com.youkeda.exercise.claw.infrastructure.channel.wechat.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信消息幂等去重服务。
 *
 * <p>以 SDK 的 {@code message_id} 为去重键，采用内存 LRU + TTL 过期策略：
 * <ul>
 *   <li>最多缓存 {@value #DEFAULT_MAX_CAPACITY} 条（超出后淘汰最旧条目，近似 LRU）</li>
 *   <li>条目写入后超过 {@value #DEFAULT_TTL_MILLIS} 毫秒视为过期，被惰性清理</li>
 * </ul>
 *
 * <p>用于兜底微信 SDK / 服务端的重复投递（getUpdates 游标丢失、断线重连等场景），
 * 与单实例锁相互独立、互为补充。
 *
 * <p>线程安全：单实例内由 {@code synchronized} 保护。进程重启后去重缓存清空——
 * 初版按此设计，真正的重启重投由 SDK 游标恢复 + 单实例锁共同兜底。
 */
@Component
public class MessageDeduplicationService {

    private static final int DEFAULT_MAX_CAPACITY = 10000;
    private static final long DEFAULT_TTL_MILLIS = 30 * 60 * 1000L;

    private final long ttlMillis;
    private final LinkedHashMap<String, Long> seen;

    public MessageDeduplicationService() {
        this(DEFAULT_MAX_CAPACITY, DEFAULT_TTL_MILLIS);
    }

    /**
     * 可配置容量与 TTL（测试用），生产使用默认值。
     *
     * @param maxCapacity 最大缓存条数
     * @param ttlMillis   条目存活时长（毫秒）
     */
    public MessageDeduplicationService(int maxCapacity, long ttlMillis) {
        this.ttlMillis = ttlMillis;
        this.seen = new LinkedHashMap<>(16, 0.75f, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                return size() > maxCapacity;
            }
        };
    }

    /**
     * 判断去重键是否已处理过，并将新键登记。
     *
     * @param dedupKey 去重键（微信消息 {@code messageId}；多 item 消息可追加 item 下标）。
     *                 为空时不做去重、直接放行。
     * @return true = 已处理过（重复消息，调用方应跳过）；false = 新消息（已登记，可正常处理）
     */
    public synchronized boolean isDuplicate(String dedupKey) {
        if (dedupKey == null || dedupKey.isBlank()) {
            // 无去重键（SDK 未返回 message_id）时不拦截，保证功能可用
            return false;
        }
        long now = System.currentTimeMillis();
        // 惰性清理过期条目：LinkedHashMap 按插入序迭代，遇到未过期条目即可停止
        var it = seen.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue() > ttlMillis) {
                it.remove();
            } else {
                break;
            }
        }
        if (seen.containsKey(dedupKey)) {
            return true;
        }
        seen.put(dedupKey, now);
        return false;
    }

    /** 当前缓存条目数（测试辅助） */
    public synchronized int size() {
        return seen.size();
    }
}
