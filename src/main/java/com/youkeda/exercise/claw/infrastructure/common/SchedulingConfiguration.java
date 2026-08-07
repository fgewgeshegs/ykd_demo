package com.youkeda.exercise.claw.infrastructure.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 全局 {@code @Scheduled} 调度器配置。
 *
 * <p>背景：Spring {@code @Scheduled} 默认单线程调度，本项目 9 个定时点（campus 每日检查、
 * anime 每日检查/每分钟播出提醒、scout 采集/推荐/画像更新/清理、memory 过期清理等）共享
 * 1 个线程——任一任务跑得慢（LLM 决策、页面采集）就会推迟其他任务的触发，例如每分钟的
 * 番剧播出提醒被早 8 点的 LLM 译名回填阻塞。
 *
 * <p>此配置提供一个多线程 {@link ThreadPoolTaskScheduler}（池大小可经
 * {@code spring.task.scheduling.pool.size} 覆盖，默认 4），各定时点分线程执行互不阻塞。
 * 定义了本 bean 后，Spring Boot 的 {@code TaskSchedulingAutoConfiguration} 会自动退避，
 * {@code @EnableScheduling} 的后处理器选用本调度器。
 */
@Configuration
public class SchedulingConfiguration {

    @Bean(name = "appTaskScheduler", destroyMethod = "shutdown")
    public TaskScheduler appTaskScheduler(
            @Value("${spring.task.scheduling.pool.size:4}") int poolSize) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix("app-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }
}
