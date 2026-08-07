package com.youkeda.exercise.claw.infrastructure.channel.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮件通道配置属性。
 *
 * <p>除 Spring Boot 标准 {@code spring.mail.*} 外，额外提供
 * {@code mail.default-recipient} 作为用户未绑定邮箱时的兜底收件地址。
 */
@Component
@ConfigurationProperties(prefix = "mail")
public class MailProperties {

    /** 默认收件人邮箱（用户未绑定时使用） */
    private String defaultRecipient;

    /** SMTP 发件人（与 spring.mail.username 一致） */
    private String username;

    public String getDefaultRecipient() {
        return defaultRecipient;
    }

    public void setDefaultRecipient(String defaultRecipient) {
        this.defaultRecipient = defaultRecipient;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
