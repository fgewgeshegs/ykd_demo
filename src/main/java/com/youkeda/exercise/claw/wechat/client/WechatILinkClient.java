package com.youkeda.exercise.claw.wechat.client;

import com.lth.wechat.ilink.ILinkClient;
import com.lth.wechat.ilink.LoginCredentials;
import com.lth.wechat.ilink.dto.login.LoginStatus;
import com.lth.wechat.ilink.dto.login.QrCodeInfo;
import com.lth.wechat.ilink.dto.message.ReceiveMessagesResult;
import com.youkeda.exercise.claw.wechat.config.WechatProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 微信 iLink 客户端封装
 *
 * 职责：
 * - SDK 初始化和登录
 * - 消息收发
 * - 异常处理（确保不因 SDK 异常导致服务退出）
 */
@Slf4j
@Component
public class WechatILinkClient {

    private final WechatProperties wechatProperties;

    private ILinkClient client;
    private LoginCredentials credentials;

    @Getter
    private volatile boolean loggedIn = false;

    private static final int MAX_LOGIN_RETRIES = 3;
    private static final int LOGIN_RETRY_INTERVAL_MS = 5000;

    public WechatILinkClient(WechatProperties wechatProperties) {
        this.wechatProperties = wechatProperties;
    }

    @PostConstruct
    public void init() {
        if (!wechatProperties.isEnabled()) {
            log.info("微信 iLink 功能未启用 (wechat.ilink.enabled=false)");
            return;
        }

        log.info("微信 iLink 客户端初始化开始");
        client = new ILinkClient();
        login();
    }

    /**
     * 扫码登录流程
     */
    private void login() {
        try {
            // 1. 获取二维码
            QrCodeInfo qrCode = client.getQrCodeInfo();
            log.info("请扫码登录: imageUrl={}", qrCode.getImageUrl());
            log.info("二维码ID: {}", qrCode.getQrcodeId());

            // 2. 等待扫码登录（带重试）
            LoginStatus status = pollLoginWithRetry(qrCode.getQrcodeId());

            if (status == null || !status.isSuccess()) {
                log.error("微信登录失败，已重试{}次，服务已禁用", MAX_LOGIN_RETRIES);
                return;
            }

            // 3. 保存登录凭证
            credentials = new LoginCredentials(
                    status.getToken(),
                    status.getUserId(),
                    status.getApiBaseUrl()
            );
            loggedIn = true;
            log.info("微信登录成功，开始监听消息");

        } catch (Exception e) {
            log.error("微信登录过程中发生异常，服务已禁用", e);
        }
    }

    /**
     * 轮询登录状态（带重试）
     */
    private LoginStatus pollLoginWithRetry(String qrcodeId) {
        int retries = 0;
        while (retries < MAX_LOGIN_RETRIES) {
            try {
                while (true) {
                    Thread.sleep(wechatProperties.getLoginPollIntervalMs());
                    LoginStatus status = client.pollLoginStatus(qrcodeId);
                    log.info("登录状态: {}", status.getStatus());

                    if (status.isSuccess()) {
                        return status;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("登录轮询被中断");
                return null;
            } catch (Exception e) {
                retries++;
                log.warn("登录轮询异常 (第{}次)，{}ms后重试: {}",
                        retries, LOGIN_RETRY_INTERVAL_MS, e.getMessage());
                if (retries < MAX_LOGIN_RETRIES) {
                    try {
                        Thread.sleep(LOGIN_RETRY_INTERVAL_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 发送文本消息
     */
    public void sendTextMessage(String toUserId, String contextToken, String text) {
        if (!loggedIn || credentials == null) {
            log.warn("微信未登录，无法发送消息");
            return;
        }
        try {
            client.sendTextMessage(credentials, toUserId, contextToken, text);
            log.info("发送回复 | to={} | text={}", toUserId, text);
        } catch (Exception e) {
            log.error("发送消息失败 | to={} | error={}", toUserId, e.getMessage());
        }
    }

    /**
     * 接收消息
     *
     * @param cursor 分页游标，首次传空字符串
     * @return 接收消息结果，异常时返回 null
     */
    public ReceiveMessagesResult receiveMessages(String cursor) {
        if (!loggedIn || credentials == null) {
            return null;
        }
        try {
            return client.receiveMessages(credentials, cursor);
        } catch (Exception e) {
            log.warn("接收消息异常: {}", e.getMessage());
            return null;
        }
    }
}
