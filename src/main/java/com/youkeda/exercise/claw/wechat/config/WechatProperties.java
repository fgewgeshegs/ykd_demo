package com.youkeda.exercise.claw.wechat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信 iLink 配置属性
 *
 * 从 application.properties 读取 wechat.ilink.* 前缀的配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat.ilink")
public class WechatProperties {

    /**
     * 是否启用微信 iLink 功能
     */
    private boolean enabled = false;

    /**
     * 消息轮询间隔（毫秒）
     */
    private int pollIntervalMs = 3000;

    /**
     * 登录状态轮询间隔（毫秒）
     */
    private int loginPollIntervalMs = 3000;
}
