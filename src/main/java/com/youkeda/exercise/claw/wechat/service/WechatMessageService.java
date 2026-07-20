package com.youkeda.exercise.claw.wechat.service;

import com.github.wechat.ilink.sdk.core.model.WeixinMessage;
import com.youkeda.exercise.claw.wechat.MessageRouter;
import com.youkeda.exercise.claw.wechat.client.WechatILinkClient;
import com.youkeda.exercise.claw.wechat.config.WechatProperties;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 微信消息监听服务
 *
 * 职责：
 * - 定时轮询微信消息（新 SDK getUpdates）
 * - 将消息交由 MessageRouter 路由分发
 * - 根据 WechatReply 类型（TEXT/IMAGE/VOICE）调用对应的发送方法
 */
@Slf4j
@Service
public class WechatMessageService {

    private final WechatILinkClient wechatClient;
    private final WechatProperties wechatProperties;
    private final MessageRouter messageRouter;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread pollThread;

    private static final int ERROR_SLEEP_MS = 5000;

    public WechatMessageService(WechatILinkClient wechatClient,
                                WechatProperties wechatProperties,
                                MessageRouter messageRouter) {
        this.wechatClient = wechatClient;
        this.wechatProperties = wechatProperties;
        this.messageRouter = messageRouter;
    }

    @PostConstruct
    public void start() {
        if (!wechatProperties.isEnabled()) {
            log.info("微信消息服务未启用 (wechat.ilink.enabled=false)");
            return;
        }

        // 等待登录完成（最多等 60 秒）
        long deadline = System.currentTimeMillis() + 60_000;
        while (!wechatClient.isLoggedIn() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        if (!wechatClient.isLoggedIn()) {
            log.warn("微信未登录，消息服务不启动");
            return;
        }

        log.info("微信消息服务启动，开始监听消息");
        running.set(true);
        pollThread = new Thread(this::pollLoop, "wechat-poll-thread");
        pollThread.setDaemon(true);
        pollThread.start();
    }

    /**
     * 消息轮询主循环（适配新 SDK v2.3.3 getUpdates + snake_case 模型）
     */
    private void pollLoop() {
        while (running.get()) {
            try {
                // 1. 使用新 SDK 的 getUpdates 获取消息（无需 cursor 管理）
                List<WeixinMessage> messages = wechatClient.receiveMessages();

                if (messages != null && !messages.isEmpty()) {
                    for (WeixinMessage msg : messages) {
                        String fromUserId = msg.getFrom_user_id();
                        String contextToken = msg.getContext_token();

                        // 忽略没有发送者的消息
                        if (fromUserId == null || fromUserId.isEmpty()) {
                            continue;
                        }

                        // 处理消息中的每个 item
                        if (msg.getItem_list() != null) {
                            msg.getItem_list().forEach(item -> {
                                WechatMessage wechatMsg = buildWechatMessage(item, fromUserId, contextToken);
                                if (wechatMsg == null) return;

                                // 立即显示"正在输入"，让用户知道机器人已收到消息
                                wechatClient.startTyping(fromUserId);

                                // 路由处理
                                try {
                                    WechatReply reply = messageRouter.route(wechatMsg);
                                    if (reply != null && reply.hasContent()) {
                                        sendReply(fromUserId, reply);
                                    }
                                } catch (Exception e) {
                                    log.error("消息路由处理异常 | error={}", e.getMessage());
                                }
                            });
                        }
                    }
                }

                // 正常轮询间隔
                Thread.sleep(wechatProperties.getPollIntervalMs());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("微信消息轮询服务被中断");
                break;
            } catch (Exception e) {
                log.warn("接收消息异常，将在{}ms后重试: {}", ERROR_SLEEP_MS, e.getMessage());
                try {
                    Thread.sleep(ERROR_SLEEP_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.info("微信消息轮询服务已停止");
    }

    /**
     * 将 SDK 的 MessageItem 转换为统一消息模型 WechatMessage
     *
     * 新 SDK 使用 getText_item() / getImage_item() / getVoice_item() 替代 isText()/getText()/isImage() 等
     */
    private WechatMessage buildWechatMessage(
            com.github.wechat.ilink.sdk.core.model.MessageItem item,
            String fromUserId, String contextToken) {

        WechatMessage wechatMsg = new WechatMessage();
        wechatMsg.setUserId(fromUserId);
        wechatMsg.setContextToken(contextToken);

        // 文本消息
        if (item.getText_item() != null && item.getText_item().getText() != null
                && !item.getText_item().getText().isEmpty()) {
            wechatMsg.setType(MessageType.TEXT);
            wechatMsg.setText(item.getText_item().getText());
            log.info("收到文本消息 | from={} | text={}", fromUserId, item.getText_item().getText());
            return wechatMsg;
        }

        // 图片消息
        if (item.getImage_item() != null) {
            var img = item.getImage_item();
            wechatMsg.setType(MessageType.IMAGE);
            wechatMsg.setImageUrl(img.getUrl());
            if (img.getMedia() != null) {
                wechatMsg.setEncryptQueryParam(img.getMedia().getEncrypt_query_param());
                wechatMsg.setAesKey(img.getMedia().getAes_key());
            }
            log.info("收到图片消息 | from={}", fromUserId);
            return wechatMsg;
        }

        // 语音消息
        if (item.getVoice_item() != null) {
            var voice = item.getVoice_item();
            wechatMsg.setType(MessageType.VOICE);
            wechatMsg.setVoiceText(voice.getText());
            wechatMsg.setPlaytime(voice.getPlaytime());
            wechatMsg.setEncodeType(voice.getEncode_type());
            wechatMsg.setVoiceSampleRate(voice.getSample_rate());
            if (voice.getMedia() != null) {
                wechatMsg.setVoiceEncryptQueryParam(voice.getMedia().getEncrypt_query_param());
                wechatMsg.setVoiceAesKey(voice.getMedia().getAes_key());
            }
            log.info("收到语音消息 | from={} | voiceText={} | playtime={}ms | sampleRate={}Hz",
                    fromUserId, voice.getText(), voice.getPlaytime(), voice.getSample_rate());
            return wechatMsg;
        }

        // 暂不支持的消息类型
        return null;
    }

    /**
     * 根据 WechatReply 类型发送回复
     *
     * 新 SDK sendText / sendImage / sendVoice 均为原生调用，无需手动 uploadMedia
     */
    private void sendReply(String toUserId, WechatReply reply) {
        switch (reply.getType()) {
            case VOICE -> {
                wechatClient.sendVoiceMessage(toUserId,
                        reply.getVoiceBytes(), reply.getPlaytime(), reply.getSampleRate(),
                        reply.getTextFallback());
            }
            case IMAGE -> {
                wechatClient.sendImageMessage(toUserId, reply.getImageBytes());
            }
            case FILE -> {
                wechatClient.sendFileMessage(toUserId, reply.getFileBytes(),
                        reply.getFileName(), reply.getFileDescription());
            }
            default -> {
                wechatClient.sendTextMessage(toUserId, reply.getText());
            }
        }
    }

    @PreDestroy
    public void stop() {
        log.info("微信消息服务正在关闭...");
        running.set(false);
        if (pollThread != null) {
            pollThread.interrupt();
        }
    }

}
