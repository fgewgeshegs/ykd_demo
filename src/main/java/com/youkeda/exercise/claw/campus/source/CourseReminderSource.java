package com.youkeda.exercise.claw.campus.source;

import com.youkeda.exercise.claw.campus.model.CampusConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 课程提醒占位 Source。
 * 等待 vae-tools 的 schedule 模块融合后启用。
 */
@Component
@ConditionalOnProperty(name = "campus.enabled", havingValue = "true")
public class CourseReminderSource implements NotificationSource {

    private static final Logger log = LoggerFactory.getLogger(CourseReminderSource.class);

    @Override
    public String getName() { return "COURSE"; }

    @Override
    public boolean supports(CampusConfig config) {
        return false; // 暂不启用，等待 vae-tools schedule 模块融合
    }

    @Override
    public void check() {
        // 空实现，融合 vae-tools 的 ScheduleReminderService 后填充
        log.debug("CourseReminderSource 未启用");
    }
}
