package org.example.wechatilink.service;

import com.github.wechat.ilink.sdk.ILinkClient;
import com.github.wechat.ilink.sdk.core.config.ILinkConfig;
import com.github.wechat.ilink.sdk.core.listener.OnLoginListener;
import com.github.wechat.ilink.sdk.core.login.LoginContext;
import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.example.wechatilink.config.ILinkConfigProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * iLink Bot 核心服务。
 * <p>
 * 管理 Bot 生命周期：QR 扫码登录 → 长轮询收消息 → 固定内容自动回复。
 * 在独立线程中运行消息轮询循环，不阻塞 Spring Boot 主线程。
 * </p>
 */
@Slf4j
@Service
public class ILinkBotService {

    private final ILinkConfigProperties configProperties;
    private final AiChatService aiChatService;

    private volatile ILinkClient client;
    private volatile LoginContext loginContext;

    /** 当前的 QR 码内容（登录前可用） */
    private volatile String currentQrCode;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean loggedIn = new AtomicBoolean(false);

    private Thread pollThread;

    public ILinkBotService(ILinkConfigProperties configProperties, AiChatService aiChatService) {
        this.configProperties = configProperties;
        this.aiChatService = aiChatService;
    }

    // ==================== 公开 API ====================

    /**
     * 启动 Bot：构建客户端 → 获取 QR 码 → 等待扫码 → 进入消息轮询。
     * 整个过程在后台线程中执行，不会阻塞调用方。
     */
    public void start() {
        if (running.get()) {
            log.warn("Bot 已在运行中，忽略重复启动");
            return;
        }
        running.set(true);

        Thread.startVirtualThread(() -> {
            try {
                doStart();
            } catch (Exception e) {
                log.error("Bot 启动异常", e);
                running.set(false);
            }
        });

        log.info("Bot 启动流程已发起（后台线程）");
    }

    /**
     * 停止 Bot，释放资源。
     */
    public void stop() {
        log.info("正在停止 Bot...");
        running.set(false);
        loggedIn.set(false);

        if (pollThread != null) {
            pollThread.interrupt();
        }
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("关闭 ILinkClient 时异常", e);
            }
            client = null;
        }
        loginContext = null;
        currentQrCode = null;
        log.info("Bot 已停止");
    }

    /**
     * 重启 Bot。
     */
    public void restart() {
        stop();
        start();
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isLoggedIn() {
        return loggedIn.get();
    }

    public String getCurrentQrCode() {
        return currentQrCode;
    }

    public LoginContext getLoginContext() {
        return loginContext;
    }

    // ==================== 生命周期管理 ====================

    @PreDestroy
    public void destroy() {
        stop();
    }

    // ==================== 内部实现 ====================

    private void doStart() throws Exception {
        // 1. 构建配置
        ILinkConfig config = ILinkConfig.builder()
                .connectTimeoutMs(configProperties.getConnectTimeoutMs())
                .readTimeoutMs(configProperties.getReadTimeoutMs())
                .writeTimeoutMs(configProperties.getWriteTimeoutMs())
                .httpMaxRetries(configProperties.getHttpMaxRetries())
                .retryBaseDelayMs(configProperties.getRetryBaseDelayMs())
                .retryMaxDelayMs(configProperties.getRetryMaxDelayMs())
                .heartbeatEnabled(configProperties.isHeartbeatEnabled())
                .heartbeatIntervalMs(configProperties.getHeartbeatIntervalMs())
                .channelVersion("1.0.0")
                .build();

        // 2. 构建 Client（带登录监听器）
        client = ILinkClient.builder()
                .config(config)
                .onLogin(new OnLoginListener() {
                    @Override
                    public void onLoginSuccess(LoginContext context) {
                        loginContext = context;
                        loggedIn.set(true);
                        log.info("✅ 登录成功！botId = {}, userId = {}",
                                context.getBotId(), context.getUserId());
                    }

                    @Override
                    public void onLoginFailure(Throwable throwable) {
                        loggedIn.set(false);
                        log.error("❌ 登录失败: {}", throwable.getMessage());
                    }
                })
                .build();

        // 3. 执行登录 —— executeLogin() 返回 QR 码内容
        log.info("正在获取登录二维码...");
        currentQrCode = client.executeLogin();
        log.info("📱 QR 码已获取，请使用微信扫描二维码登录\n{}", currentQrCode);

        // 4. 阻塞等待扫码确认
        log.info("等待扫码确认...");
        LoginContext context = client.getLoginFuture().get();
        loginContext = context;
        loggedIn.set(true);
        currentQrCode = null; // 登录后清空 QR 码
        log.info("🎉 Bot 登录完成！botId = {}", context.getBotId());

        // 5. 进入消息轮询循环
        runPollLoop();
    }

    /**
     * 消息轮询主循环。在独立线程中持续长轮询，收到消息后自动回复。
     */
    private void runPollLoop() {
        pollThread = Thread.currentThread();
        log.info("🔄 开始消息轮询（间隔 {}ms）...", configProperties.getPollIntervalMs());

        while (running.get() && loggedIn.get()) {
            try {
                // getUpdates() 是长轮询（最长 35 秒），返回新消息列表
                List<WeixinMessage> messages = client.getUpdates();

                if (messages != null && !messages.isEmpty()) {
                    log.info("📩 收到 {} 条消息", messages.size());
                    handleMessages(messages);
                }
            } catch (Exception e) {
                if (running.get()) {
                    log.error("消息轮询异常，{}ms 后重试", configProperties.getPollIntervalMs(), e);
                    try {
                        Thread.sleep(configProperties.getPollIntervalMs());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.info("消息轮询循环已退出");
    }

    /**
     * 处理收到的消息列表 —— 对每条文本消息发送固定回复。
     */
    private void handleMessages(List<WeixinMessage> messages) {
        for (WeixinMessage msg : messages) {
            String fromUserId = msg.getFrom_user_id();
            log.info("📨 收到消息: fromUserId={}, msgId={}", fromUserId, msg.getMessage_id());

            List<MessageItem> items = msg.getItem_list();
            if (items == null || items.isEmpty()) {
                continue;
            }

            for (MessageItem item : items) {
                // 只处理文本消息
                if (item.getText_item() != null) {
                    String userText = item.getText_item().getText();
                    log.info("💬 文本消息: fromUserId={}, text={}", fromUserId, userText);

                    // 构造固定回复
                    String reply = buildReply(userText);
                    sendReply(fromUserId, reply);
                } else if (item.getImage_item() != null) {
                    log.info("🖼️ 收到图片消息: fromUserId={}", fromUserId);
                    sendReply(fromUserId, "收到你的图片啦！📷\n\n" +
                            "目前我只能处理文字消息，请发送文字给我吧~");
                } else {
                    log.info("📎 收到其他类型消息: fromUserId={}, type={}",
                            fromUserId, item.getType());
                    sendReply(fromUserId, "收到你的消息啦！\n\n"
                            + "🤖 这是 iLink Bot 的自动回复，目前还是固定回复模式哦~");
                }
            }
        }
    }

    /** 天气 HTTP 客户端（复用） */
    private static final HttpClient weatherHttpClient = HttpClient.newHttpClient();

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss EEEE");

    /**
     * 构造回复内容。AI 模式优先，失败则降级到固定模板。
     */
    private String buildReply(String userText) {
        // 1. 尝试 AI 回复
        if (configProperties.isAiEnabled()) {
            String aiReply = aiChatService.chat(userText);
            if (aiReply != null && !aiReply.isBlank()) {
                return aiReply;
            }
            log.warn("AI 回复失败，降级到固定模板");
        }

        // 2. 降级：固定模板
        String result = configProperties.getReplyTemplate();

        if (result.contains("{text}")) {
            result = result.replace("{text}", userText);
        }
        if (result.contains("{time}")) {
            result = result.replace("{time}", LocalDateTime.now().format(TIME_FORMATTER));
        }
        if (result.contains("{weather}")) {
            result = result.replace("{weather}", fetchWeather());
        }

        return result;
    }

    /**
     * 从 wttr.in 获取天气（免费，无需 API Key）。
     */
    private String fetchWeather() {
        try {
            String city = configProperties.getWeatherCity();
            URI uri = new URI("https", "wttr.in", "/" + city,
                    "format=%C+%25t&lang=zh", null);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();
            HttpResponse<String> response = weatherHttpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body().trim();
            }
        } catch (Exception e) {
            log.warn("获取天气失败", e);
        }
        return "天气暂不可用";
    }

    /**
     * 发送回复消息（带异常处理）。
     */
    private void sendReply(String userId, String text) {
        try {
            client.sendText(userId, text);
            log.info("✅ 回复成功: userId={}, text={}", userId,
                    text.length() > 50 ? text.substring(0, 50) + "..." : text);
        } catch (Exception e) {
            log.error("❌ 回复失败: userId={}", userId, e);
        }
    }
}
