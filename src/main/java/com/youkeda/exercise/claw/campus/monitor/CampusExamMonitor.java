package com.youkeda.exercise.claw.campus.monitor;

import com.youkeda.exercise.claw.domain.campus.CampusConfig;
import com.youkeda.exercise.claw.campus.processor.CampusNoticeProcessor;
import com.youkeda.exercise.claw.campus.store.CampusConfigStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "campus.enabled", havingValue = "true")
public class CampusExamMonitor {

    private static final Logger log = LoggerFactory.getLogger(CampusExamMonitor.class);

    private final CampusConfigStore configStore;
    private final CampusNoticeProcessor processor;

    public CampusExamMonitor(CampusConfigStore configStore,
                              CampusNoticeProcessor processor) {
        this.configStore = configStore;
        this.processor = processor;
    }

    /**
     * 每天 08:00 检查考试通知
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void checkExams() {
        CampusConfig config = configStore.get();
        if (config == null || !config.isEnabled()) {
            log.debug("尚未配置学校信息，跳过考试通知检查");
            return;
        }
        log.info("===== CampusExamMonitor 启动 | school={} =====", config.getSchool());
        processor.process(config);
    }
}
