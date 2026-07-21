package com.youkeda.exercise.claw.context;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Redis 上下文配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "context.redis")
public class RedisContextProperties {

    /** 是否启用 Redis 存储（false 时用内存存储） */
    private boolean enabled = false;

    /** 消息过期天数 */
    private int ttlDays = 7;

    /** 单用户最大消息条数 */
    private int maxMessages = 50;
}
