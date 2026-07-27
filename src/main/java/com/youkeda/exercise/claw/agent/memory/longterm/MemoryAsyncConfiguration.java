package com.youkeda.exercise.claw.agent.memory.longterm;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/** Dedicated bounded executor for memory extraction and persistence. */
@Configuration
public class MemoryAsyncConfiguration {

    @Bean(name = "memoryTaskExecutor", destroyMethod = "shutdown")
    public Executor memoryTaskExecutor(LongTermMemoryProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int coreSize = Math.max(1, properties.getAsyncCoreSize());
        int maxSize = Math.max(coreSize, properties.getAsyncMaxSize());
        executor.setThreadNamePrefix("memory-worker-");
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(Math.max(1, properties.getAsyncQueueCapacity()));
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
