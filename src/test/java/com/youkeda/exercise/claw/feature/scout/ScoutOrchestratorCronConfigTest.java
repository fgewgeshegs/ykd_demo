package com.youkeda.exercise.claw.feature.scout;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 信息猎手定时任务 cron 可配置性守卫。
 *
 * <p>修复背景：画像更新（每天推送有感知的时点）硬编码 {@code 0 45 19 * * *}，与 campus/anime
 * 的配置化约定不一致，无法临时改时点测试。本测试防回归到硬编码。
 */
class ScoutOrchestratorCronConfigTest {

    @Test
    void profileUpdateCronIsConfigurableViaScoutProfileCron() throws NoSuchMethodException {
        Method method = ScoutOrchestrator.class.getMethod("scheduledProfileUpdate");
        Scheduled ann = method.getAnnotation(Scheduled.class);

        assertTrue(ann != null && ann.cron().contains("${scout.profile-cron:"),
                "scheduledProfileUpdate cron 应包含 ${scout.profile-cron: 占位符，实际="
                        + (ann != null ? ann.cron() : "<无 @Scheduled>"));
    }
}
