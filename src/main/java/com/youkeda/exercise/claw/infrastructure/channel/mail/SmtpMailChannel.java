package com.youkeda.exercise.claw.infrastructure.channel.mail;

import com.youkeda.exercise.claw.infrastructure.channel.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * SMTP 邮件通知通道。
 *
 * <p>使用 Spring Boot 自动配置的 {@link JavaMailSender}，
 * 通过 QQ 邮箱 SMTP 发送通知。不走 token，配置正确即永远可用。
 *
 * <p>通过 {@code mail.enabled} 控制开关，关闭时 Bean 不注册。
 */
@Component
@ConditionalOnProperty(name = "mail.enabled", havingValue = "true")
public class SmtpMailChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailChannel.class);
    private static final String SUBJECT_PREFIX = "[Claw] ";

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    public SmtpMailChannel(JavaMailSender mailSender, MailProperties mailProperties) {
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
    }

    @Override
    public boolean send(String recipient, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailProperties.getUsername());
            message.setTo(recipient);
            message.setSubject(buildSubject(text));
            message.setText(text);
            mailSender.send(message);
            log.debug("邮件发送成功 | to={}", recipient);
            return true;
        } catch (MailException e) {
            log.error("邮件发送失败 | to={} | error={}", recipient, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String name() {
        return "smtp-mail";
    }

    private String buildSubject(String text) {
        if (text == null || text.isBlank()) {
            return SUBJECT_PREFIX + "通知";
        }
        String firstLine = text.lines()
                .filter(l -> !l.isBlank())
                .findFirst()
                .orElse(text);
        int maxLen = Math.min(firstLine.length(), 50);
        return SUBJECT_PREFIX + firstLine.substring(0, maxLen);
    }
}
