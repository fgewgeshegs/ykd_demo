package com.youkeda.exercise.claw.wechat.client;

import com.github.wechat.ilink.sdk.ILinkClient;
import com.github.wechat.ilink.sdk.core.model.CDNMedia;
import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;
import com.youkeda.exercise.claw.context.ContextStore;
import com.youkeda.exercise.claw.wechat.config.WechatProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 微信 iLink 客户端封装（SDK 2.3.3）
 *
 * 职责：SDK 初始化、登录、消息收发、媒体下载
 */
@Slf4j
@Component
public class WechatILinkClient {

    private final WechatProperties wechatProperties;
    private final ContextStore contextStore;

    private ILinkClient client;

    @Getter
    private volatile boolean loggedIn = false;

    public WechatILinkClient(WechatProperties wechatProperties, ContextStore contextStore) {
        this.wechatProperties = wechatProperties;
        this.contextStore = contextStore;
    }

    @PostConstruct
    public void init() {
        if (!wechatProperties.isEnabled()) {
            log.info("微信 iLink 功能未启用");
            return;
        }

        log.info("微信 iLink 客户端初始化开始");
        client = ILinkClient.builder().build();

        CompletableFuture.runAsync(() -> {
            try {
                String qrBase64 = client.executeLogin();
                log.info("请扫码登录（qrcode={}，图片长度={}）", client.getQrcode(), qrBase64.length());
                // 等待登录完成
                long deadline = System.currentTimeMillis() + 120_000;
                while (!client.isLoggedIn() && System.currentTimeMillis() < deadline) {
                    Thread.sleep(1000);
                }
                if (client.isLoggedIn()) {
                    loggedIn = true;
                    log.info("微信登录成功");
                } else {
                    log.error("微信登录超时");
                }
            } catch (Exception e) {
                log.error("微信登录异常", e);
            }
        });
    }

    /** 发送文本消息 */
    public void sendTextMessage(String toUserId, String text) {
        if (!loggedIn || client == null) {
            log.warn("微信未登录，无法发送消息");
            return;
        }
        try {
            client.sendText(toUserId, text);
            log.info("发送回复 | to={} | text={}", toUserId, text);
        } catch (Exception e) {
            log.error("发送消息失败 | to={} | error={}", toUserId, e.getMessage());
        }
    }

    /** 接收消息 */
    public List<WeixinMessage> receiveMessages() {
        if (!loggedIn || client == null) return null;
        try {
            return client.getUpdates();
        } catch (Exception e) {
            log.warn("接收消息异常: {}", e.getMessage());
            return null;
        }
    }

    /** 发送图片消息 */
    public void sendImageMessage(String toUserId, byte[] imageBytes, String fileName) {
        if (!loggedIn || client == null) {
            log.warn("微信未登录，无法发送图片");
            return;
        }
        try {
            client.sendImage(toUserId, imageBytes, fileName, null);
            log.info("发送图片 | to={} | size={} bytes", toUserId, imageBytes.length);
        } catch (Exception e) {
            log.error("发送图片失败 | to={} | error={}", toUserId, e.getMessage());
        }
    }

    /** 发送语音消息 */
    public void sendVoiceMessage(String toUserId, byte[] voiceBytes, String fileName,
                                  int playTimeMs, int sampleRate) {
        if (!loggedIn || client == null) {
            log.warn("微信未登录，无法发送语音");
            return;
        }
        try {
            client.sendVoice(toUserId, voiceBytes, fileName, playTimeMs, sampleRate);
            log.info("发送语音 | to={} | size={} bytes | duration={}ms",
                    toUserId, voiceBytes.length, playTimeMs);
        } catch (Exception e) {
            log.error("发送语音失败 | to={} | error={}", toUserId, e.getMessage());
        }
    }

    /** 从 CDN 下载媒体（兼容旧接口） */
    public byte[] downloadMedia(String encryptQueryParam, String aesKey) {
        CDNMedia media = new CDNMedia();
        media.setEncrypt_query_param(encryptQueryParam);
        media.setAes_key(aesKey);
        return downloadMedia(media);
    }

    /** 从 CDN 下载媒体 */
    public byte[] downloadMedia(CDNMedia media) {
        if (!loggedIn || client == null) return null;
        try {
            return client.downloadMedia(media);
        } catch (Exception e) {
            log.error("媒体下载失败: {}", e.getMessage());
            return null;
        }
    }

    /** 从 MessageItem 下载媒体（图片/语音/文件通用） */
    public byte[] downloadMediaFromMessageItem(MessageItem item) {
        if (!loggedIn || client == null) return null;
        try {
            return client.downloadMediaFromMessageItem(item);
        } catch (Exception e) {
            log.error("媒体下载失败: {}", e.getMessage());
            return null;
        }
    }
}
