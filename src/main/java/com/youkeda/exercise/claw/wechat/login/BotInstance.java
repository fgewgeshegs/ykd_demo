package com.youkeda.exercise.claw.wechat.login;

import com.github.wechat.ilink.sdk.ILinkClient;
import com.github.wechat.ilink.sdk.core.config.ILinkConfig;
import com.github.wechat.ilink.sdk.core.context.ResumeContext;
import com.github.wechat.ilink.sdk.core.login.LoginContext;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 包装一个 ILinkClient + 对应的 ResumeContext。
 * 提供消息收发委托方法，屏蔽 SDK 细节。
 */
public class BotInstance {

    private static final Logger log = LoggerFactory.getLogger(BotInstance.class);

    private final ILinkClient client;
    private final String botId;
    private final String botUserId;

    public BotInstance(ResumeContext resumeContext) {
        LoginContext lc = resumeContext.getLoginContext();
        this.botId = lc.getBotId();
        this.botUserId = lc.getUserId();

        ILinkConfig config = ILinkConfig.builder()
                .heartbeatEnabled(false)
                .autoReconnectEnabled(true)
                .reconnectMaxAttempts(5)
                .build();

        this.client = ILinkClient.builder()
                .config(config)
                .resumeContext(resumeContext)
                .build();

        log.info("BotInstance 创建 | botId={} | botUserId={}", botId, botUserId);
    }

    /** 首次扫码登录创建 */
    public BotInstance() {
        ILinkConfig config = ILinkConfig.builder()
                .heartbeatEnabled(false)
                .autoReconnectEnabled(true)
                .reconnectMaxAttempts(5)
                .build();

        this.client = ILinkClient.builder()
                .config(config)
                .build();

        LoginContext lc = client.getLoginContext();
        this.botId = lc != null ? lc.getBotId() : "unknown";
        this.botUserId = lc != null ? lc.getUserId() : "unknown";
    }

    public String getBotId() { return botId; }

    public String getBotUserId() { return botUserId; }

    public boolean isLoggedIn() { return client.isLoggedIn(); }

    public LoginContext getLoginContext() { return client.getLoginContext(); }

    /** 导出 ResumeContext 用于持久化 */
    public ResumeContext exportResumeContext() { return client.exportResumeContext(); }

    /** 执行扫码登录，返回二维码 URL/Base64 */
    public String executeLogin() { return client.executeLogin(); }

    /** 接收消息 */
    public List<WeixinMessage> receiveMessages() {
        try {
            return client.getUpdates();
        } catch (Exception e) {
            log.warn("接收消息异常: {}", e.getMessage());
            return List.of();
        }
    }

    /** 发送文本 */
    public void sendText(String toUserId, String text) {
        try {
            client.sendText(toUserId, text);
        } catch (Exception e) {
            log.error("发送文本失败 | to={}", toUserId, e);
        }
    }

    /** 发送图片 */
    public void sendImage(String toUserId, byte[] imageBytes, String textFallback) {
        try {
            client.startTyping(toUserId);
        } catch (Exception e) {
            log.warn("startTyping 失败 | to={}", toUserId);
        }
        byte[] copy = imageBytes.clone();
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            int maxRetries = 2;
            for (int i = 0; i <= maxRetries; i++) {
                try {
                    client.sendImage(toUserId, copy, "image.jpg", "");
                    log.info("发送图片成功 | to={}", toUserId);
                    return;
                } catch (Exception e) {
                    if (i < maxRetries) {
                        try { Thread.sleep(1000L * (i + 1)); } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    } else if (textFallback != null && !textFallback.isEmpty()) {
                        try { client.sendText(toUserId, textFallback); } catch (Exception ex) {
                            log.error("降级文字发送也失败 | to={}", toUserId);
                        }
                    }
                }
            }
        });
    }

    /** 下载媒体 */
    public byte[] downloadMedia(String encryptQueryParam, String aesKey) {
        try {
            var media = new com.github.wechat.ilink.sdk.core.model.CDNMedia();
            media.setEncrypt_query_param(encryptQueryParam);
            media.setAes_key(aesKey);
            return client.downloadMedia(media);
        } catch (Exception e) {
            log.error("媒体下载失败: {}", e.getMessage());
            return null;
        }
    }

    public byte[] downloadMediaFromMessageItem(com.github.wechat.ilink.sdk.core.model.MessageItem item) {
        try {
            return client.downloadMediaFromMessageItem(item);
        } catch (Exception e) {
            log.error("媒体下载失败: {}", e.getMessage());
            return null;
        }
    }

    /** 显示正在输入 */
    public void startTyping(String toUserId) {
        try {
            client.startTyping(toUserId);
        } catch (Exception e) {
            log.warn("startTyping 失败 | to={}", toUserId);
        }
    }

    /** 发送文件 */
    public void sendFile(String toUserId, byte[] fileBytes, String fileName, String fileDescription) {
        byte[] copy = fileBytes.clone();
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                client.sendFile(toUserId, copy, fileName, fileDescription);
                log.info("发送文件成功 | to={} | file={}", toUserId, fileName);
            } catch (Exception e) {
                log.error("发送文件失败 | to={} | file={}", toUserId, fileName, e);
            }
        });
    }

    public void close() {
        try {
            client.close();
        } catch (Exception e) {
            log.warn("关闭 BotInstance 异常", e);
        }
    }
}
