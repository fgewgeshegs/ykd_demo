package com.youkeda.exercise.claw.campus.source;

import com.youkeda.exercise.claw.campus.model.CampusConfig;

/**
 * 通知来源接口。
 * 每个通知类型（考试/比赛/活动/就业）实现此接口。
 * Source 保持纤薄，只做编排：collect → dedup → classify → decide → push。
 */
public interface NotificationSource {

    /** Source 标识，用于日志和配置开关 */
    String getName();

    /** 判断此 Source 是否支持该全局配置（例如学校是否匹配、用户类型等） */
    boolean supports(CampusConfig globalConfig);

    /** 执行一次检查。Source 内部编排自己的 Collector/Classifier/Policy 等组件 */
    void check();
}
