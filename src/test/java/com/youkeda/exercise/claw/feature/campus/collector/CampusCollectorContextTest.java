package com.youkeda.exercise.claw.feature.campus.collector;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * collector 包 Spring 装配测试。
 *
 * <p>修复背景：CompetitionCollector/CampusNoticeCollector 有注入构造器 + 测试 seam
 * 两个构造器，未标 @Autowired 时 Spring 回退找无参构造器而失败
 * （No default constructor found，应用启动即崩）。本测试用真实 Spring 上下文
 * 验证这两个 bean 能被正确实例化并注入 CampusPageFetcher，防再次踩装配坑。
 */
class CampusCollectorContextTest {

    @Test
    void collectorsInstantiateInSpringContext() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.getEnvironment().setActiveProfiles();
            // 让 @ConditionalOnProperty(campus.enabled=true) 生效
            System.setProperty("campus.enabled", "true");
            ctx.scan("com.youkeda.exercise.claw.feature.campus.collector");
            ctx.refresh();

            CampusPageFetcher fetcher = ctx.getBean(CampusPageFetcher.class);
            assertNotNull(fetcher);

            CompetitionCollector competition = ctx.getBean("campusCompetitionCollector", CompetitionCollector.class);
            assertNotNull(competition, "CompetitionCollector 应能被 Spring 实例化（注入构造器被正确选用）");

            CampusNoticeCollector notice = ctx.getBean(CampusNoticeCollector.class);
            assertNotNull(notice, "CampusNoticeCollector 应能被 Spring 实例化（注入构造器被正确选用）");

            System.clearProperty("campus.enabled");
        } finally {
            System.clearProperty("campus.enabled");
        }
    }
}
