package com.youkeda.exercise.claw.wechat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信 iLink 配置属性
 *
 * 从 application.properties 读取 wechat.ilink.* 前缀的配置
 */
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(int pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public int getLoginPollIntervalMs() {
        return loginPollIntervalMs;
    }

    public void setLoginPollIntervalMs(int loginPollIntervalMs) {
        this.loginPollIntervalMs = loginPollIntervalMs;
    }
}