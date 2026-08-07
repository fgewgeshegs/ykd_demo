package com.youkeda.exercise.claw.feature.anime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 番剧专用异步执行器。
 *
 * <p>译名回填的 LLM 调用是慢阻塞操作，若在 {@link AnimeScheduler#dailyCheck()}
 * 主线程执行，会占住 Spring {@code @Scheduled} 单线程调度器（推迟每分钟的播出提醒）
 * 并拖慢应用就绪事件。此 executor 让回填异步执行，调用方立即返回。
 *
 * <p>仿 {@code MemoryAsyncConfiguration#memoryTaskExecutor()} 的模式：
 * 有界单线程池 + 拒绝时等待（不丢已提交任务） + 优雅关停。
 */
@Configuration
public class AnimeAsyncConfiguration {

    @Bean(name = "animeBackfillExecutor", destroyMethod = "shutdown")
    public Executor animeBackfillExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 回填是低频批量任务，串行即可；最多 1 线程避免并发 LLM 调用放大限流风险
        executor.setThreadNamePrefix("anime-backfill-");
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(100);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
