package com.youkeda.exercise.claw.feature.anime.scheduler;

import com.youkeda.exercise.claw.feature.anime.AnimeAsyncConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * AnimeScheduler 异步回填的 Spring 装配测试。
 *
 * <p>修复背景：AnimeScheduler 构造器新增 {@code @Qualifier("animeBackfillExecutor") Executor}
 * 参数，但单元测试直接 new 绕过 Spring，无法发现「bean 不存在 / 名字不匹配」类装配错误。
 * 本测试用真实 Spring 上下文验证 animeBackfillExecutor bean 可创建且按名可解析。
 */
class AnimeSchedulerContextTest {

    @Test
    void animeBackfillExecutorBeanResolvable() {
        try (AnnotationConfigApplicationContext ctx =
                     new AnnotationConfigApplicationContext(AnimeAsyncConfiguration.class)) {
            Executor executor = ctx.getBean("animeBackfillExecutor", Executor.class);
            assertNotNull(executor, "animeBackfillExecutor bean 应能被 Spring 创建");
        }
    }
}
