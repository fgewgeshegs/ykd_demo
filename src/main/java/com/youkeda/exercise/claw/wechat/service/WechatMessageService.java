package com.youkeda.exercise.claw.wechat.service;

import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;
import com.youkeda.exercise.claw.context.ContextStore;
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

@Slf4j
@Service
public class WechatMessageService {

    private static final int TYPE_TEXT = 1;
    private static final int TYPE_IMAGE = 2;
    private static final int TYPE_VOICE = 3;

    private final WechatILinkClient wechatClient;
    private final WechatProperties wechatProperties;
    private final MessageRouter messageRouter;
    private final ContextStore contextStore;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread pollThread;

    private static final int ERROR_SLEEP_MS = 5000;

    public WechatMessageService(WechatILinkClient wechatClient,
                                WechatProperties wechatProperties,
                                MessageRouter messageRouter,
                                ContextStore contextStore) {
        this.wechatClient = wechatClient;
        this.wechatProperties = wechatProperties;
        this.messageRouter = messageRouter;
        this.contextStore = contextStore;
    }

    @PostConstruct
    public void start() {
        if (!wechatProperties.isEnabled()) return;

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
        log.info("轮询线程启动");
        int loop = 0;
        while (running.get()) {
            loop++;
            try {
                if (loop <= 3 || loop % 10 == 0) {
                    log.info("轮询第{}次...", loop);
                }

                List<WeixinMessage> messages = wechatClient.receiveMessages();

                if (messages != null && !messages.isEmpty()) {
                    log.info("收到{}条消息", messages.size());
                    for (WeixinMessage msg : messages) {
                        String fromUserId = msg.getFrom_user_id();
                        if (fromUserId == null || fromUserId.isEmpty()) continue;

                        List<MessageItem> items = msg.getItem_list();
                        if (items == null) continue;

                        for (MessageItem item : items) {
                            WechatMessage wechatMsg = buildWechatMessage(fromUserId, msg.getContext_token(), item);
                            if (wechatMsg == null) continue;

                            // 统一上下文：发一条存一条（文字/语音/图片）
                            saveMessageToContext(wechatMsg);

                            wechatClient.startTyping(fromUserId);

                            try {
                                WechatReply reply = messageRouter.route(wechatMsg);
                                if (reply != null && reply.hasContent()) {
                                    sendReply(fromUserId, reply);
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
                log.error("轮询异常，{}ms后重试", ERROR_SLEEP_MS, e);
                try { Thread.sleep(ERROR_SLEEP_MS); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }
        log.info("轮询线程退出");
    }

    private WechatMessage buildWechatMessage(String fromUserId, String contextToken, MessageItem item) {
        WechatMessage m = new WechatMessage();
        m.setUserId(fromUserId);
        m.setContextToken(contextToken);

        int type = item.getType();

        if (type == TYPE_TEXT && item.getText_item() != null && item.getText_item().getText() != null) {
            m.setType(MessageType.TEXT);
            m.setText(item.getText_item().getText());
            log.info("收到消息 | from={} | text={}", fromUserId, item.getText_item().getText());
        } else if (type == TYPE_IMAGE && item.getImage_item() != null) {
            var img = item.getImage_item();
            m.setType(MessageType.IMAGE);
            m.setImageUrl(img.getUrl());
            if (img.getMedia() != null) {
                m.setEncryptQueryParam(img.getMedia().getEncrypt_query_param());
                m.setAesKey(img.getMedia().getAes_key());
            }
            log.info("收到图片消息 | from={}", fromUserId);
        } else if (type == TYPE_VOICE && item.getVoice_item() != null) {
            var voice = item.getVoice_item();
            m.setType(MessageType.VOICE);
            if (voice.getMedia() != null) {
                m.setVoiceEncryptQueryParam(voice.getMedia().getEncrypt_query_param());
                m.setVoiceAesKey(voice.getMedia().getAes_key());
            }
            m.setVoiceText(voice.getText());
            m.setPlaytime(voice.getPlaytime());
            m.setEncodeType(voice.getEncode_type());
            m.setVoiceSampleRate(voice.getSample_rate());
            log.info("收到语音消息 | from={} | voiceText={} | playtime={}ms | sampleRate={}Hz",
                    fromUserId, voice.getText(), voice.getPlaytime(), voice.getSample_rate());
        } else {
            return null;
        }
        return m;
    }

    /**
     * 统一上下文存储：发一条存一条，文字/语音/图片全部进入对话历史
     * CDN 参数和媒体 URL 嵌入 Message，不再分开存两条
     */
    private void saveMessageToContext(WechatMessage msg) {
        String userId = msg.getUserId();
        switch (msg.getType()) {
            case TEXT -> {
                if (msg.getText() != null && !msg.getText().isEmpty()) {
                    contextStore.append(userId, "user", msg.getText());
                }
            }
            case VOICE -> {
                String vEnc = msg.getVoiceEncryptQueryParam();
                String vKey = msg.getVoiceAesKey();
                String vText = msg.getVoiceText() != null ? msg.getVoiceText() : "";
                String content = !vText.isEmpty() ? "[语音]" + vText : "[语音消息]";
                contextStore.append(userId, "user", content, vEnc, vKey, null);
            }
            case IMAGE -> {
                String iEnc = msg.getEncryptQueryParam();
                String iKey = msg.getAesKey();
                String iUrl = msg.getImageUrl() != null ? msg.getImageUrl() : "";
                contextStore.append(userId, "user", "[图片]", iEnc, iKey, iUrl);
            }
        }
    }

    /**
     * 根据 WechatReply 类型分发回复
     */
    private void sendReply(String toUserId, WechatReply reply) {
        switch (reply.getType()) {
            case VOICE -> wechatClient.sendVoiceMessage(toUserId,
                    reply.getVoiceBytes(), reply.getPlaytime(), reply.getSampleRate(),
                    reply.getTextFallback());
            case IMAGE -> wechatClient.sendImageMessage(toUserId, reply.getImageBytes(), "image.jpg");
            case FILE -> wechatClient.sendFileMessage(toUserId, reply.getFileBytes(),
                    reply.getFileName(), reply.getFileDescription());
            default -> wechatClient.sendTextMessage(toUserId, reply.getText());
        }
    }

    @PreDestroy
    public void stop() {
        log.info("微信消息服务正在关闭...");
        running.set(false);
        if (pollThread != null) pollThread.interrupt();
    }
}
