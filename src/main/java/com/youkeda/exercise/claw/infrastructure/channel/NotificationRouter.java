package com.youkeda.exercise.claw.infrastructure.channel;

import com.youkeda.exercise.claw.infrastructure.channel.mail.SmtpMailChannel;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.WechatChannel;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.user.WechatUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 通知路由器。
 *
 * <p>通道选择 + fallback + 审计记录。优先微信，微信不可用时降级到邮件。
 * 每次发送（无论成功/失败/走哪个通道）都通过 {@link NotificationRecordRepository} 入库。
 */
@Component
public class NotificationRouter {

    private static final Logger log = LoggerFactory.getLogger(NotificationRouter.class);

    private final WechatChannel wechatChannel;
    private final SmtpMailChannel mailChannel;
    private final WechatUserManager userManager;
    private final NotificationRecordRepository recordRepo;

    public NotificationRouter(WechatChannel wechatChannel,
                              SmtpMailChannel mailChannel,
                              WechatUserManager userManager,
                              NotificationRecordRepository recordRepo) {
        this.wechatChannel = wechatChannel;
        this.mailChannel = mailChannel;
        this.userManager = userManager;
        this.recordRepo = recordRepo;
    }

    /**
     * 发送通知。
     *
     * <p>优先微信；微信不可用或发送失败时 fallback 到邮件。
     *
     * @param userId 用户标识
     * @param type   通知类型
     * @param text   通知正文
     * @return true 表示至少一个通道发送成功
     */
    public boolean send(String userId, NotificationType type, String text) {
        // 1. 主通道：微信
        if (wechatChannel.isAvailable()) {
            boolean ok = wechatChannel.send(userId, text);
            recordRepo.save(userId, type, text, "WECHAT", ok,
                    ok ? null : "wechat send returned false");
            if (ok) return true;
            log.warn("微信发送失败，尝试邮件 fallback | userId={}", userId);
        }

        // 2. 降级通道：邮件
        if (mailChannel != null && mailChannel.isAvailable()) {
            String email = userManager.getUserEmail(userId);
            if (email != null && !email.isBlank()) {
                try {
                    boolean ok = mailChannel.send(email, text);
                    recordRepo.save(userId, type, text, "EMAIL", ok,
                            ok ? null : "smtp send returned false");
                    if (ok) {
                        log.info("邮件降级推送成功 | userId={} | email={}", userId, email);
                        return true;
                    }
                } catch (Exception e) {
                    recordRepo.save(userId, type, text, "EMAIL", false, e.getMessage());
                    log.error("邮件降级推送异常 | userId={} | error={}", userId, e.getMessage());
                    return false;
                }
            }
        }

        // 3. 全挂
        recordRepo.save(userId, type, text, "NONE", false, "all channels unavailable");
        log.error("所有通知通道不可用 | userId={}", userId);
        return false;
    }

    /** 通道状态（Dashboard 展示用） */
    public String getChannelStatus() {
        if (wechatChannel.isAvailable()) return "wechat";
        if (mailChannel != null && mailChannel.isAvailable()) return "email(fallback)";
        return "offline";
    }
}
