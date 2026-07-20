package com.youkeda.exercise.claw.wechat.service;

import com.lth.wechat.ilink.dto.message.ReceiveMessagesResult;
import com.youkeda.exercise.claw.wechat.MessageRouter;
import com.youkeda.exercise.claw.wechat.client.WechatILinkClient;
import com.youkeda.exercise.claw.wechat.config.WechatProperties;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 微信消息监听服务
 *
 * 职责：
 * - 定时轮询微信消息
 * - 将消息交由 MessageRouter 路由分发
 * - 将 Router 返回的回复内容通过客户端发送
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
     * 消息轮询主循环
     */
    private void pollLoop() {
        while (running.get()) {
            try {
                ReceiveMessagesResult result = wechatClient.receiveMessages(cursor);

                if (result != null && result.getMessages() != null) {
                    result.getMessages().forEach(msg -> {
                        String fromUserId = msg.getFromUserId();
                        String contextToken = msg.getContextToken();

                        // 忽略没有发送者的消息
                        if (fromUserId == null || fromUserId.isEmpty()) {
                            return;
                        }

                        msg.getItemList().forEach(item -> {
                            // 1. 构建统一消息模型
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
                            } else {
                                // 暂不支持的消息类型，跳过
                                return;
                            }

                            // 2. 交由 MessageRouter 路由处理
                            try {
                                String reply = messageRouter.route(wechatMsg);
                                if (reply != null && !reply.isEmpty()) {
                                    wechatClient.sendTextMessage(fromUserId, contextToken, reply);
                                }
                            } catch (Exception e) {
                                log.error("消息路由处理异常 | error={}", e.getMessage());
                            }
                        });
                    });
                }

                // 更新 cursor（使用 nextCursor 分页）
                if (result != null && result.getNextCursor() != null && !result.getNextCursor().isEmpty()) {
                    cursor = result.getNextCursor();
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

    @PreDestroy
    public void stop() {
        log.info("微信消息服务正在关闭...");
        running.set(false);
        if (pollThread != null) {
            pollThread.interrupt();
        }
    }
}
