package com.youkeda.exercise.claw.wechat.service;

import com.github.wechat.ilink.sdk.core.model.MessageItem;
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
 * 微信消息监听服务（SDK 2.3.3）
 */
@Slf4j
@Service
public class WechatMessageService {

    // MessageItem type 常量
    private static final int TYPE_TEXT = 1;
    private static final int TYPE_IMAGE = 2;
    private static final int TYPE_VOICE = 3;

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
            log.info("微信消息服务未启用");
            return;
        }

        long deadline = System.currentTimeMillis() + 60_000;
        while (!wechatClient.isLoggedIn() && System.currentTimeMillis() < deadline) {
            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }

        if (!wechatClient.isLoggedIn()) {
            log.warn("微信未登录，消息服务不启动");
            return;
        }

        log.info("微信消息服务启动");
        running.set(true);
        pollThread = new Thread(this::pollLoop, "wechat-poll-thread");
        pollThread.setDaemon(true);
        pollThread.start();
    }

    private void pollLoop() {
        while (running.get()) {
            try {
                List<WeixinMessage> messages = wechatClient.receiveMessages();

                if (messages != null) {
                    for (WeixinMessage msg : messages) {
                        String fromUserId = msg.getFrom_user_id();
                        if (fromUserId == null || fromUserId.isEmpty()) continue;

                        List<MessageItem> items = msg.getItem_list();
                        if (items == null) continue;

                        for (MessageItem item : items) {
                            WechatMessage wechatMsg = new WechatMessage();
                            wechatMsg.setUserId(fromUserId);
                            wechatMsg.setContextToken(msg.getContext_token());

                            int type = item.getType();

                            if (type == TYPE_TEXT && item.getText_item() != null
                                    && item.getText_item().getText() != null) {
                                wechatMsg.setType(MessageType.TEXT);
                                wechatMsg.setText(item.getText_item().getText());
                                log.info("收到消息 | from={} | text={}", fromUserId, item.getText_item().getText());

                            } else if (type == TYPE_IMAGE && item.getImage_item() != null) {
                                var img = item.getImage_item();
                                wechatMsg.setType(MessageType.IMAGE);
                                wechatMsg.setImageUrl(img.getUrl());
                                if (img.getMedia() != null) {
                                    wechatMsg.setEncryptQueryParam(img.getMedia().getEncrypt_query_param());
                                }
                                wechatMsg.setAesKey(img.getAeskey());
                                log.info("收到图片消息 | from={}", fromUserId);

                            } else if (type == TYPE_VOICE && item.getVoice_item() != null) {
                                var voice = item.getVoice_item();
                                wechatMsg.setType(MessageType.VOICE);
                                if (voice.getMedia() != null) {
                                    wechatMsg.setVoiceEncryptQueryParam(voice.getMedia().getEncrypt_query_param());
                                    wechatMsg.setVoiceAesKey(voice.getMedia().getAes_key());
                                }
                                wechatMsg.setVoiceText(voice.getText());
                                log.info("收到语音消息 | from={} | text={}", fromUserId, voice.getText());

                            } else {
                                continue;
                            }

                            try {
                                WechatReply reply = messageRouter.route(wechatMsg);
                                if (reply != null && reply.hasContent()) {
                                    if (reply.getType() == MessageType.IMAGE) {
                                        wechatClient.sendImageMessage(fromUserId, reply.getImageBytes(), "image.jpg");
                                    } else {
                                        wechatClient.sendTextMessage(fromUserId, reply.getText());
                                    }
                                }
                            } catch (Exception e) {
                                log.error("消息路由处理异常 | error={}", e.getMessage());
                            }
                        }
                    }
                }

                Thread.sleep(wechatProperties.getPollIntervalMs());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("接收消息异常，{}ms后重试: {}", ERROR_SLEEP_MS, e.getMessage());
                try { Thread.sleep(ERROR_SLEEP_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }
        log.info("微信消息轮询服务已停止");
    }

    @PreDestroy
    public void stop() {
        log.info("微信消息服务正在关闭...");
        running.set(false);
        if (pollThread != null) pollThread.interrupt();
    }
}
