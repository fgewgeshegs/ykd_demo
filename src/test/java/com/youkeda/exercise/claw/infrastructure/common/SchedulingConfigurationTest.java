package com.youkeda.exercise.claw.infrastructure.common;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 全局 {@code @Scheduled} 调度器装配测试。
 *
 * <p>修复背景：Spring {@code @Scheduled} 默认单线程调度，9 个定时点（campus/anime/scout/内存清理）
 * 共享 1 个线程——任一任务跑得慢（LLM 决策、采集）就会推迟其他任务的触发（如每分钟的番剧播出提醒）。
 * 本配置提供一个多线程 {@link TaskScheduler}，让各定时点互不阻塞。
 */
class SchedulingConfigurationTest {

    @Test
    void createsMultithreadedTaskScheduler() {
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(SchedulingConfiguration.class)) {
            TaskScheduler scheduler = ctx.getBean(TaskScheduler.class);
            assertNotNull(scheduler, "应提供一个 TaskScheduler bean 供 @Scheduled 使用");

            assertInstanceOf(ThreadPoolTaskScheduler.class, scheduler);
            ThreadPoolTaskScheduler pool = (ThreadPoolTaskScheduler) scheduler;

            assertEquals(4, pool.getScheduledThreadPoolExecutor().getCorePoolSize(),
                    "调度器应默认 4 线程，避免定时任务互相阻塞（可被 spring.task.scheduling.pool.size 覆盖）");
        }
    }
}
