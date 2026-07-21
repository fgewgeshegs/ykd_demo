package com.youkeda.exercise.claw.wechat.client;

import com.github.wechat.ilink.sdk.ILinkClient;
import com.github.wechat.ilink.sdk.core.config.ILinkConfig;
import com.github.wechat.ilink.sdk.core.model.CDNMedia;
import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;
import com.youkeda.exercise.claw.context.ContextStore;
import com.youkeda.exercise.claw.wechat.config.WechatProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 微信 iLink 客户端封装（SDK v2.3.3）
 *
 * 职责：
 * - SDK 初始化和登录（Builder 模式）
 * - 消息收发（getUpdates / sendText / sendImage / sendVoice / sendFile）
 * - 媒体下载（通过 CDNMedia 对象或 MessageItem）
 * - 上下文保存（通过 ContextStore 保存用户消息）
 *
 * 设计原则：
 * - 媒体发送（语音/图片/文件）异步执行，避免阻塞轮询线程
 * - 发送前先 startTyping 显示"正在输入"
 * - SDK 内置心跳关闭（和 getUpdates 共用一个锁，会抢锁丢消息）
 */
@Slf4j
@Component
public class WechatILinkClient {

    private final WechatProperties wechatProperties;
    private final ContextStore contextStore;

    private ILinkClient client;

    @Getter
    private volatile boolean loggedIn = false;

    private static final long LOGIN_TIMEOUT_MS = 120_000;

    public WechatILinkClient(WechatProperties wechatProperties, ContextStore contextStore) {
        this.wechatProperties = wechatProperties;
        this.contextStore = contextStore;
    }

    @PostConstruct
    public void init() {
        if (!wechatProperties.isEnabled()) {
            log.info("微信 iLink 功能未启用 (wechat.ilink.enabled=false)");
            return;
        }

        log.info("微信 iLink 客户端初始化开始 (SDK v2.3.3)");

        // 关闭 SDK 内置心跳（和 getUpdates 共用一个锁会冲突），自己轮询
        ILinkConfig config = ILinkConfig.builder()
                .heartbeatEnabled(false)
                .autoReconnectEnabled(true)
                .reconnectMaxAttempts(5)
                .build();

        client = ILinkClient.builder()
                .config(config)
                .build();

        CompletableFuture.runAsync(() -> {
            try {
                String qrResult = client.executeLogin();
                // executeLogin 返回二维码链接或 base64 图片
                if (qrResult.startsWith("http")) {
                    log.info("请扫码登录 → {}", qrResult);
                } else {
                    String qrBase64 = qrResult.contains(",") ? qrResult.substring(qrResult.indexOf(",") + 1) : qrResult;
                    byte[] qrBytes = Base64.getDecoder().decode(qrBase64);
                    Path qrFile = Path.of("qrcode.png");
                    Files.write(qrFile, qrBytes);
                    log.info("请扫码登录 → {}", qrFile.toAbsolutePath());
                }
                // 等待登录完成
                long deadline = System.currentTimeMillis() + LOGIN_TIMEOUT_MS;
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

    // ==================== 消息发送 ====================

    /**
     * 发送文本消息
     */
    public void sendTextMessage(String toUserId, String text) {
        if (!checkReady()) return;
        try {
            client.sendText(toUserId, text);
            log.info("发送文本消息 | to={} | text={}", toUserId, text);
        } catch (Exception e) {
            log.error("发送文本消息失败 | to={} | error={}", toUserId, e.getMessage());
        }
    }

    /**
     * 显示「正在输入」指示
     */
    public void startTyping(String toUserId) {
        if (!checkReady()) return;
        try {
            client.startTyping(toUserId);
        } catch (Exception e) {
            log.warn("startTyping 失败 | to={} | error={}", toUserId, e.getMessage());
        }
    }

    /**
     * 发送图片消息（异步，不阻塞轮询线程），发送前显示"正在输入"
     */
    public void sendImageMessage(String toUserId, byte[] imageBytes) {
        if (!checkReady()) return;
        try {
            client.startTyping(toUserId);
        } catch (Exception e) {
            log.warn("startTyping 失败 | to={}", toUserId);
        }
        CompletableFuture.runAsync(() -> {
            try {
                client.sendImage(toUserId, imageBytes, "image.jpg", "");
                log.info("发送图片消息成功 | to={} | size={} bytes", toUserId, imageBytes.length);
            } catch (Exception e) {
                log.error("发送图片消息失败 | to={} | error={}", toUserId, e.getMessage());
            }
        });
    }

    /**
     * 发送语音消息（异步，不阻塞轮询线程），失败时自动降级为文字消息
     *
     * 发送前先显示"正在输入"。
     *
     * @param toUserId     接收方 userId
     * @param voiceBytes   音频字节数据
     * @param playtime     音频时长（毫秒）
     * @param sampleRate   音频采样率（Hz）
     * @param textFallback 语音发送失败的兜底文字，为 null 则不降级
     */
    public void sendVoiceMessage(String toUserId,
                                  byte[] voiceBytes, int playtime, int sampleRate,
                                  String textFallback) {
        if (!checkReady()) return;

        try { client.startTyping(toUserId); } catch (Exception e) {
            log.warn("startTyping 失败 | to={}", toUserId);
        }

        byte[] bytesCopy = voiceBytes.clone();
        CompletableFuture.runAsync(() -> {
            try {
                log.info("异步发送语音 | to={} | size={} | playtime={}ms | sampleRate={}Hz",
                        toUserId, bytesCopy.length, playtime, sampleRate);
                client.sendVoice(toUserId, bytesCopy, "voice.mp3", playtime, sampleRate);
                log.info("发送语音消息成功 | to={}", toUserId);
            } catch (Exception e) {
                log.error("发送语音消息失败 | to={} | error={}", toUserId, e.getMessage(), e);
                if (textFallback != null && !textFallback.isEmpty()) {
                    log.warn("语音发送失败，降级为文字消息 | to={}", toUserId);
                    try { client.sendText(toUserId, textFallback); } catch (Exception ex) {
                        log.error("降级文字发送也失败 | to={}", toUserId);
                    }
                }
            }
        });
    }

    /**
     * 发送语音消息（完整参数，自定义编码），异步执行
     */
    public void sendVoiceMessage(String toUserId,
                                  byte[] voiceBytes, int playtime, int sampleRate,
                                  int encodeType, int bitsPerSample,
                                  String textFallback) {
        if (!checkReady()) return;

        try { client.startTyping(toUserId); } catch (Exception e) {
            log.warn("startTyping 失败 | to={}", toUserId);
        }

        byte[] bytesCopy = voiceBytes.clone();
        CompletableFuture.runAsync(() -> {
            try {
                log.info("异步发送语音（完整参数）| to={} | size={} | playtime={}ms | encodeType={}",
                        toUserId, bytesCopy.length, playtime, encodeType);
                client.sendVoice(toUserId, bytesCopy, "voice.mp3", playtime, sampleRate,
                        null, encodeType, bitsPerSample, null);
                log.info("发送语音消息成功（完整参数）| to={}", toUserId);
            } catch (Exception e) {
                log.error("发送语音消息失败 | to={} | error={}", toUserId, e.getMessage(), e);
                if (textFallback != null && !textFallback.isEmpty()) {
                    log.warn("语音发送失败，降级为文字消息 | to={}", toUserId);
                    try { client.sendText(toUserId, textFallback); } catch (Exception ex) {
                        log.error("降级文字发送也失败 | to={}", toUserId);
                    }
                }
            }
        });
    }

    /**
     * 发送文件消息（异步，不阻塞轮询线程），发送前显示"正在输入"
     *
     * SDK v2.3.3 支持直接发送文件（如 MP3、PDF 等）。
     *
     * @param toUserId       接收方 userId
     * @param fileBytes      文件字节数据
     * @param fileName       文件名（如 "AI语音回复.mp3"）
     * @param fileDescription 文件描述文本
     */
    public void sendFileMessage(String toUserId, byte[] fileBytes, String fileName, String fileDescription) {
        if (!checkReady()) return;
        try {
            client.startTyping(toUserId);
        } catch (Exception e) {
            log.warn("startTyping 失败 | to={}", toUserId);
        }
        byte[] bytesCopy = fileBytes.clone();
        CompletableFuture.runAsync(() -> {
            try {
                log.info("异步发送文件 | to={} | fileName={} | size={} bytes",
                        toUserId, fileName, bytesCopy.length);
                client.sendFile(toUserId, bytesCopy, fileName, fileDescription);
                log.info("发送文件消息成功 | to={} | fileName={}", toUserId, fileName);
            } catch (Exception e) {
                log.error("发送文件消息失败 | to={} | fileName={} | error={}",
                        toUserId, fileName, e.getMessage(), e);
            }
        });
    }

    // ==================== 消息接收 ====================

    /**
     * 接收消息 — 使用新 SDK 的 getUpdates()，内部管理 cursor
     *
     * @return 消息列表，异常时返回空列表
     */
    public List<WeixinMessage> receiveMessages() {
        if (!checkReady()) return Collections.emptyList();
        try {
            return client.getUpdates();
        } catch (Exception e) {
            log.warn("接收消息异常: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ==================== 媒体下载 ====================

    /**
     * 下载媒体文件（通过 encryptQueryParam + aesKey 创建 CDNMedia 对象）
     *
     * @param encryptQueryParam CDN 加密查询参数
     * @param aesKey            解密密钥
     * @return 媒体字节数组，失败时返回 null
     */
    public byte[] downloadMedia(String encryptQueryParam, String aesKey) {
        if (!checkReady()) return null;
        try {
            CDNMedia media = new CDNMedia();
            media.setEncrypt_query_param(encryptQueryParam);
            media.setAes_key(aesKey);
            byte[] data = client.downloadMedia(media);
            log.info("媒体下载成功 | size={} bytes", data.length);
            return data;
        } catch (Exception e) {
            log.error("媒体下载失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 MessageItem 下载媒体（图片/语音/文件通用）
     */
    public byte[] downloadMediaFromMessageItem(MessageItem item) {
        if (!checkReady()) return null;
        try {
            return client.downloadMediaFromMessageItem(item);
        } catch (Exception e) {
            log.error("媒体下载失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 生命周期 ====================

    @PreDestroy
    public void destroy() {
        if (client != null) {
            try {
                client.close();
                log.info("微信 iLink 客户端已关闭");
            } catch (Exception e) {
                log.warn("关闭微信 iLink 客户端异常", e);
            }
        }
    }

    /**
     * 检查客户端是否就绪
     */
    private boolean checkReady() {
        if (client == null || !loggedIn) {
            log.warn("微信未登录或客户端未就绪");
            return false;
        }
        return true;
    }
}
