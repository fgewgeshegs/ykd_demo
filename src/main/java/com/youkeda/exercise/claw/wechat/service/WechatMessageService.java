package com.youkeda.exercise.claw.wechat.service;

import com.github.wechat.ilink.sdk.core.model.WeixinMessage;
import com.youkeda.exercise.claw.agent.memory.ContextStore;
import com.youkeda.exercise.claw.wechat.MessageRouter;
import com.youkeda.exercise.claw.wechat.client.WechatILinkClient;
import com.youkeda.exercise.claw.wechat.config.WechatProperties;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 微信消息监听服务。
 * 轮询线程只负责收消息 + 入线程池，worker 并行处理消息。
 */
@Service
public class WechatMessageService {

    private static final Logger log = LoggerFactory.getLogger(WechatMessageService.class);

    private static final int WORKER_COUNT = 4;

    private final WechatILinkClient wechatClient;
    private final WechatProperties wechatProperties;
    private final MessageRouter messageRouter;
    private final ContextStore contextStore;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread pollThread;
    private final ExecutorService workers = Executors.newFixedThreadPool(WORKER_COUNT);

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
        if (!wechatProperties.isEnabled()) {
            log.info("微信消息服务未启用");
            return;
        }

        // 等待 BotManager 加载完成
        long deadline = System.currentTimeMillis() + 60_000;
        while (System.currentTimeMillis() < deadline) {
            var msgs = wechatClient.receiveMessages();
            if (msgs != null) {
                break;  // client 就绪
            }
            try { Thread.sleep(1000); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        log.info("微信消息服务启动，开始监听消息（worker={}）", WORKER_COUNT);
        running.set(true);
        pollThread = new Thread(this::pollLoop, "wechat-poll-thread");
        pollThread.setDaemon(true);
        pollThread.start();
    }

    private void pollLoop() {
        while (running.get()) {
            try {
                List<WeixinMessage> messages = wechatClient.receiveMessages();
                if (messages != null && !messages.isEmpty()) {
                    log.info("收到{}条消息", messages.size());
                    for (WeixinMessage msg : messages) {
                        String fromUserId = msg.getFrom_user_id();
                        if (fromUserId == null || fromUserId.isEmpty()) continue;

                        if (msg.getItem_list() != null) {
                            for (var item : msg.getItem_list()) {
                                WechatMessage wechatMsg = buildWechatMessage(item, fromUserId, msg.getContext_token());
                                if (wechatMsg == null) continue;

                                // 入线程池，worker 并行处理
                                workers.submit(() -> processMessage(wechatMsg));
                            }
                        }
                    }
                }
                Thread.sleep(wechatProperties.getPollIntervalMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("微信消息轮询服务被中断");
                break;
            } catch (Exception e) {
                log.warn("接收消息异常，{}ms后重试: {}", ERROR_SLEEP_MS, e.getMessage());
                try { Thread.sleep(ERROR_SLEEP_MS); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.info("微信消息轮询服务已停止");
    }

    /** Worker 处理单条消息 */
    private void processMessage(WechatMessage wechatMsg) {
        String userId = wechatMsg.getUserId();
        try {
            // 1. 保存用户消息
            saveMessageToContext(wechatMsg);

            // 2. 显示正在输入
            wechatClient.startTyping(userId);

            // 3. Agent 处理
            WechatReply reply = messageRouter.route(wechatMsg);
            if (reply != null && reply.hasContent()) {
                // 4. 发送回复
                sendReply(userId, reply);
            }
        } catch (Exception e) {
            log.error("处理消息异常 | userId={} | error={}", userId, e.getMessage(), e);
        }
    }

    // ==================== buildWechatMessage / saveMessageToContext / sendReply 保持不变 ====================

    private WechatMessage buildWechatMessage(
            com.github.wechat.ilink.sdk.core.model.MessageItem item,
            String fromUserId, String contextToken) {

        WechatMessage wechatMsg = new WechatMessage();
        wechatMsg.setUserId(fromUserId);
        wechatMsg.setContextToken(contextToken);

        if (item.getText_item() != null && item.getText_item().getText() != null
                && !item.getText_item().getText().isEmpty()) {
            wechatMsg.setType(MessageType.TEXT);
            wechatMsg.setText(item.getText_item().getText());
            log.info("收到文本消息 | from={} | text={}", fromUserId, item.getText_item().getText());
            return wechatMsg;
        }

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

        if (item.getFile_item() != null) {
            var fileItem = item.getFile_item();
            wechatMsg.setType(MessageType.FILE);
            wechatMsg.setFileName(fileItem.getFile_name());
            wechatMsg.setFileMd5(fileItem.getMd5());
            wechatMsg.setFileLen(fileItem.getLen());
            if (fileItem.getMedia() != null) {
                wechatMsg.setFileEncryptQueryParam(fileItem.getMedia().getEncrypt_query_param());
                wechatMsg.setFileAesKey(fileItem.getMedia().getAes_key());
            }
            log.info("收到文件消息 | from={} | fileName={} | fileLen={}",
                    fromUserId, fileItem.getFile_name(), fileItem.getLen());
            return wechatMsg;
        }

        return null;
    }

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
            case FILE -> {
                String fEnc = msg.getFileEncryptQueryParam();
                String fKey = msg.getFileAesKey();
                String fName = msg.getFileName() != null ? msg.getFileName() : "";
                contextStore.append(userId, "user", "[文件: " + fName + "]", fEnc, fKey, null);
            }
        }
    }

    private void sendReply(String toUserId, WechatReply reply) {
        switch (reply.getType()) {
            case IMAGE ->
                wechatClient.sendImageMessage(toUserId, reply.getImageBytes(),
                    "抱歉，图片发送失败，请稍后再试。");
            case FILE ->
                wechatClient.sendFileMessage(toUserId, reply.getFileBytes(),
                    reply.getFileName(), reply.getFileDescription());
            default ->
                wechatClient.sendTextMessage(toUserId, reply.getText());
        }
    }

    @PreDestroy
    public void stop() {
        log.info("微信消息服务正在关闭...");
        running.set(false);
        if (pollThread != null) {
            pollThread.interrupt();
        }
        workers.shutdown();
        try {
            if (!workers.awaitTermination(5, TimeUnit.SECONDS)) {
                workers.shutdownNow();
            }
        } catch (InterruptedException e) {
            workers.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
