package com.youkeda.exercise.claw.agent;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Agent 执行线程池。
 *
 * <p>职责：
 * <ul>
 *   <li>将 Agent 执行从微信轮询线程中解耦到独立线程池</li>
 *   <li>保证同一 userId 的任务串行执行（per-user lock）</li>
 *   <li>不同 userId 的任务并行执行（共享线程池）</li>
 * </ul>
 *
 * <p>线程模型：
 * <pre>
 *   wechat-poll-thread  →  AgentExecutionPool.execute(userId, task)
 *                               ↓
 *                         agent-executor-N (N=poolSize)
 *                               ↓
 *                         per-user ReentrantLock（串行保证）
 * </pre>
 */
@Component
public class AgentExecutionPool {

    private static final Logger log = LoggerFactory.getLogger(AgentExecutionPool.class);

    private final ExecutorService executor;
    private final ConcurrentHashMap<String, Lock> userLocks = new ConcurrentHashMap<>();

    public AgentExecutionPool(
            @Value("${agent.executor.pool-size:4}") int poolSize) {
        this.executor = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "agent-executor");
            t.setDaemon(true);
            return t;
        });
        log.info("AgentExecutionPool 初始化完成 | poolSize={}", poolSize);
    }

    /**
     * 提交一个 Agent 执行任务。
     *
     * <p>同一 userId 的任务保证串行执行（FIFO），不同 userId 并行。
     *
     * @param userId 用户标识
     * @param task   待执行任务
     */
    public void execute(String userId, Runnable task) {
        Lock lock = userLocks.computeIfAbsent(userId, k -> new ReentrantLock());
        try {
            executor.submit(() -> {
                lock.lock();
                try {
                    task.run();
                } finally {
                    lock.unlock();
                }
            });
        } catch (RejectedExecutionException e) {
            log.error("Agent 执行线程池已关闭或过载，任务被拒绝 | userId={}", userId, e);
            // 任务未提交，锁不会被持有，无需 unlock
            throw e;
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("AgentExecutionPool 正在关闭...");
        executor.shutdown();
    }
}