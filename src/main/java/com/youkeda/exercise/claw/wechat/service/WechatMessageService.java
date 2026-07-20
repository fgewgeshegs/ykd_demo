package com.youkeda.exercise.claw.wechat.service;

import com.lth.wechat.ilink.dto.message.MessageItemDto;
import com.lth.wechat.ilink.dto.message.ReceiveMessagesResult;
import com.lth.wechat.ilink.dto.message.RefMessage;
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
 * - 定时轮询微信消息
 * - 将消息交由 MessageRouter 路由分发
 * - 根据 WechatReply 类型（TEXT/IMAGE）调用对应的发送方法
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

                        List<MessageItemDto> items = msg.getItemList();
                        for (int i = 0; i < items.size(); i++) {
                            MessageItemDto item = items.get(i);

                            // ── 图文合并：IMAGE + TEXT ──
                            if (item.isImage() && item.getImage() != null
                                    && i + 1 < items.size() && items.get(i + 1).isText()
                                    && items.get(i + 1).getText() != null && !items.get(i + 1).getText().isEmpty()) {
                                WechatMessage wechatMsg = buildImageMessage(fromUserId, contextToken, item);
                                MessageItemDto textItem = items.get(i + 1);
                                wechatMsg.setHasAttachedImage(true);
                                wechatMsg.setType(MessageType.TEXT);
                                wechatMsg.setText(textItem.getText());
                                extractRefMessage(textItem, wechatMsg);
                                log.info("收到图文消息(图+文) | from={} | text={}", fromUserId, textItem.getText());
                                routeAndReply(wechatMsg, fromUserId, contextToken);
                                i++;
                                continue;
                            }

                            // ── 图文合并：TEXT + IMAGE ──
                            if (item.isText() && item.getText() != null && !item.getText().isEmpty()
                                    && i + 1 < items.size() && items.get(i + 1).isImage()
                                    && items.get(i + 1).getImage() != null) {
                                WechatMessage wechatMsg = buildImageMessage(fromUserId, contextToken, items.get(i + 1));
                                wechatMsg.setHasAttachedImage(true);
                                wechatMsg.setType(MessageType.TEXT);
                                wechatMsg.setText(item.getText());
                                extractRefMessage(item, wechatMsg);
                                log.info("收到图文消息(文+图) | from={} | text={}", fromUserId, item.getText());
                                routeAndReply(wechatMsg, fromUserId, contextToken);
                                i++;
                                continue;
                            }

                            // ── 单独处理 ──
                            WechatMessage wechatMsg = new WechatMessage();
                            wechatMsg.setUserId(fromUserId);
                            wechatMsg.setContextToken(contextToken);

                            if (item.isText() && item.getText() != null && !item.getText().isEmpty()) {
                                wechatMsg.setType(MessageType.TEXT);
                                wechatMsg.setText(item.getText());
                                log.info("收到消息 | from={} | text={}", fromUserId, item.getText());
                            } else if (item.isImage() && item.getImage() != null) {
                                fillImageFields(wechatMsg, item);
                                log.info("收到图片消息 | from={}", fromUserId);
                            } else {
                                continue;
                            }

                            extractRefMessage(item, wechatMsg);
                            routeAndReply(wechatMsg, fromUserId, contextToken);
                        }
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

    /**
     * 构建图片消息基础对象
     */
    private WechatMessage buildImageMessage(String fromUserId, String contextToken, MessageItemDto item) {
        WechatMessage msg = new WechatMessage();
        msg.setUserId(fromUserId);
        msg.setContextToken(contextToken);
        fillImageFields(msg, item);
        return msg;
    }

    /**
     * 填充图片相关字段
     */
    private void fillImageFields(WechatMessage msg, MessageItemDto item) {
        msg.setType(MessageType.IMAGE);
        msg.setImageUrl(item.getImage().getUrl());
        msg.setEncryptQueryParam(item.getImage().getEncryptQueryParam());
        msg.setAesKey(item.getImage().getAesKey());
    }

    /**
     * 路由消息并发送回复
     */
    private void routeAndReply(WechatMessage wechatMsg, String fromUserId, String contextToken) {
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
    }

    /**
     * 从消息项中提取引用消息的文字内容和图片参数
     */
    private void extractRefMessage(MessageItemDto item, WechatMessage wechatMsg) {
        RefMessage ref = item.getRefMessage();
        if (ref == null) {
            return;
        }

        MessageItemDto refItem = ref.getMessageItem();

        // ── 提取引用消息的图片参数 ──
        if (refItem != null && refItem.getImage() != null) {
            String encryptParam = refItem.getImage().getEncryptQueryParam();
            String aesKey = refItem.getImage().getAesKey();
            if (encryptParam != null && !encryptParam.isEmpty()
                    && aesKey != null && !aesKey.isEmpty()) {
                wechatMsg.setRefImageEncryptParam(encryptParam);
                wechatMsg.setRefImageAesKey(aesKey);
                log.debug("引用消息（图片）| from={}", wechatMsg.getUserId());
            }
        }

        // 提取引用消息的文字内容
        if (refItem != null && refItem.getText() != null && !refItem.getText().isEmpty()) {
            wechatMsg.setRefMessageText(refItem.getText());
            log.debug("引用消息（文本）| from={} | refText={}", wechatMsg.getUserId(), refItem.getText());
            return;
        }

        // 引用的是语音消息，取微信转写
        if (refItem != null && refItem.getVoice() != null
                && refItem.getVoice().getText() != null && !refItem.getVoice().getText().isEmpty()) {
            wechatMsg.setRefMessageText(refItem.getVoice().getText());
            log.debug("引用消息（语音）| from={}", wechatMsg.getUserId());
            return;
        }

        // 降级：用 title
        if (ref.getTitle() != null && !ref.getTitle().isEmpty()) {
            wechatMsg.setRefMessageText(ref.getTitle());
            log.debug("引用消息（title）| from={}", wechatMsg.getUserId());
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
