package com.youkeda.exercise.claw.wechat.service;

import com.lth.wechat.ilink.dto.message.ReceiveMessagesResult;
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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 微信消息监听服务
 */
@Slf4j
@Service
public class WechatMessageService {

    private final WechatILinkClient wechatClient;
    private final WechatProperties wechatProperties;
    private final MessageRouter messageRouter;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread pollThread;
    private String cursor = "";

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

        log.info("微信消息服务启动");
        running.set(true);
        pollThread = new Thread(this::pollLoop, "wechat-poll-thread");
        pollThread.setDaemon(true);
        pollThread.start();
    }

    private void pollLoop() {
        while (running.get()) {
            try {
                ReceiveMessagesResult result = wechatClient.receiveMessages(cursor);

                if (result != null && result.getMessages() != null) {
                    result.getMessages().forEach(msg -> {
                        String fromUserId = msg.getFromUserId();
                        String contextToken = msg.getContextToken();

                        if (fromUserId == null || fromUserId.isEmpty()) {
                            return;
                        }

                        msg.getItemList().forEach(item -> {
                            WechatMessage wechatMsg = new WechatMessage();
                            wechatMsg.setUserId(fromUserId);
                            wechatMsg.setContextToken(contextToken);

                            if (item.isText() && item.getText() != null && !item.getText().isEmpty()) {
                                wechatMsg.setType(MessageType.TEXT);
                                wechatMsg.setText(item.getText());
                                log.info("收到消息 | from={} | text={}", fromUserId, item.getText());
                            } else if (item.isImage() && item.getImage() != null) {
                                wechatMsg.setType(MessageType.IMAGE);
                                wechatMsg.setImageUrl(item.getImage().getUrl());
                                wechatMsg.setEncryptQueryParam(item.getImage().getEncryptQueryParam());
                                wechatMsg.setAesKey(item.getImage().getAesKey());
                                log.info("收到图片消息 | from={}", fromUserId);
                            } else if (item.isVoice() && item.getVoice() != null) {
                                wechatMsg.setType(MessageType.VOICE);
                                wechatMsg.setVoiceEncryptQueryParam(item.getVoice().getEncryptQueryParam());
                                wechatMsg.setVoiceAesKey(item.getVoice().getAesKey());
                                wechatMsg.setVoiceText(item.getVoice().getText());
                                log.info("收到语音消息 | from={} | text={}", fromUserId, item.getVoice().getText());
                            } else {
                                return;
                            }

                            try {
                                WechatReply reply = messageRouter.route(wechatMsg);
                                if (reply != null && reply.hasContent()) {
                                    if (reply.getType() == MessageType.IMAGE) {
                                        wechatClient.sendImageMessage(fromUserId, contextToken, reply.getImageBytes());
                                    } else {
                                        wechatClient.sendTextMessage(fromUserId, contextToken, reply.getText());
                                    }
                                }
                            } catch (Exception e) {
                                log.error("消息路由处理异常 | error={}", e.getMessage());
                            }
                        });
                    });
                }

                if (result != null && result.getNextCursor() != null && !result.getNextCursor().isEmpty()) {
                    cursor = result.getNextCursor();
                }

                Thread.sleep(wechatProperties.getPollIntervalMs());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("接收消息异常，{}ms后重试: {}", ERROR_SLEEP_MS, e.getMessage());
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

    @PreDestroy
    public void stop() {
        log.info("微信消息服务正在关闭...");
        running.set(false);
        if (pollThread != null) {
            pollThread.interrupt();
        }
    }
}
