package com.youkeda.exercise.claw.infrastructure.channel.wechat.login;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wechat.ilink.sdk.core.context.ResumeContext;
import com.youkeda.exercise.claw.agent.activity.AgentActivityStore;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.bot.BotStatusManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

/**
 * Bot 实例管理器（单 bot 模式）。
 * 启动时从 SQLite 恢复 session 使机器人立即工作，同时在后台弹二维码供换号用。
 */
@Component
public class BotManager {

    private static final Logger log = LoggerFactory.getLogger(BotManager.class);

    private final BotSessionStore botSessionStore;
    private final ObjectMapper objectMapper;
    private final ResumeContextCodec codec;
    private final BotStatusManager botStatusManager;
    private final AgentActivityStore activityStore;
    private volatile BotInstance bot;
    private volatile LoginPageServer pageServer;
    /** 最近一次已落盘的 session JSON，用于周期持久化时判断是否有变化 */
    private volatile String lastPersistedJson;

    public BotManager(BotSessionStore botSessionStore, ObjectMapper objectMapper,
                      BotStatusManager botStatusManager,
                      AgentActivityStore activityStore) {
        this.botSessionStore = botSessionStore;
        this.objectMapper = objectMapper;
        this.codec = new ResumeContextCodec(objectMapper);
        this.botStatusManager = botStatusManager;
        this.activityStore = activityStore;
    }

    @PostConstruct
    public void init() {
        LoginStateManager stateManager = new LoginStateManager();

        // 1. 仅恢复仍在七天有效期内、且 SDK 确认有效的 session
        List<BotSessionStore.BotSessionRow> sessions = botSessionStore.getActiveBotSessions();
        for (BotSessionStore.BotSessionRow row : sessions) {
            try {
                ResumeContext ctx = deserializeResumeContext(row.resumeContextJson());
                BotInstance instance = new BotInstance(ctx);
                if (instance.isLoggedIn()) {
                    bot = instance;
                    botStatusManager.markConnected();
                    stateManager.updateStatus(LoginStatus.SUCCESS);
                    log.info("Bot 会话恢复成功 | botId={} | expiresAt={}",
                            row.botId(), row.expiresAt());
                    break;
                } else {
                    log.warn("Bot 会话已过期 | botId={}", row.botId());
                    botSessionStore.disableBotSession(row.botId());
                }
            } catch (Exception e) {
                log.error("Bot 会话恢复失败 | botId={}", row.botId(), e);
                botSessionStore.disableBotSession(row.botId());
            }
        }

        // 2. 恢复成功直接进入控制台；否则才申请二维码
        if (bot != null) {
            startPageServer(stateManager, "/dashboard");
        } else {
            log.info("没有可恢复的有效会话，进入扫码登录");
            startLoginFlow(stateManager);
        }
    }

    /**
     * 后台线程获取二维码并等待扫码。
     * 扫码成功则替换当前 bot；超时不影响现有会话。
     */
    private void startLoginFlow(LoginStateManager stateManager) {
        Thread t = new Thread(() -> {
            BotInstance qrBot = new BotInstance();
            String qrResult;
            try {
                qrResult = qrBot.executeLogin();
            } catch (Exception e) {
                log.error("获取二维码失败", e);
                return;
            }

            try {
                if (qrResult.startsWith("http")) {
                    stateManager.updateQrUrl(qrResult);
                    stateManager.updateStatus(LoginStatus.WAITING_SCAN);

                    startPageServer(stateManager, "/login");

                    long deadline = System.currentTimeMillis() + 120_000;
                    while (!qrBot.isLoggedIn() && System.currentTimeMillis() < deadline) {
                        Thread.sleep(1000);
                    }
                    if (qrBot.isLoggedIn()) {
                        stateManager.updateStatus(LoginStatus.SUCCESS);
                        log.info("扫码成功，切换账号");
                        replaceSession(qrBot);
                        botStatusManager.markConnected();
                    } else {
                        stateManager.updateStatus(LoginStatus.TIMEOUT);
                        log.info("扫码超时，保持当前 session");
                        Thread.sleep(10000);
                    }
                } else {
                    String qrBase64 = qrResult.contains(",")
                            ? qrResult.substring(qrResult.indexOf(",") + 1)
                            : qrResult;
                    byte[] qrBytes = Base64.getDecoder().decode(qrBase64);
                    Path qrFile = Path.of("qrcode.png");
                    Files.write(qrFile, qrBytes);
                    log.info("请扫码登录 → {}", qrFile.toAbsolutePath());

                    long deadline = System.currentTimeMillis() + 120_000;
                    while (!qrBot.isLoggedIn() && System.currentTimeMillis() < deadline) {
                        Thread.sleep(1000);
                    }
                    if (qrBot.isLoggedIn()) {
                        log.info("扫码成功，切换账号");
                        replaceSession(qrBot);
                    } else {
                        log.info("扫码超时");
                    }
                }
            } catch (Exception e) {
                log.error("扫码流程异常", e);
            }
        }, "qr-login");
        t.setDaemon(true);
        t.start();
    }

    private void startPageServer(LoginStateManager stateManager, String initialPath) {
        try {
            pageServer = new LoginPageServer(
                    stateManager, botStatusManager, activityStore, objectMapper);
            int port = pageServer.start();
            openBrowser("http://127.0.0.1:" + port + initialPath);
        } catch (IOException e) {
            log.error("启动 ClawBot 本地页面失败", e);
        }
    }

    /**
     * 用新扫码的 bot 替换当前 session，旧 bot 关闭并禁用。
     *
     * <p>先置空 {@code bot} 再 close：避免 60s 周期持久化线程在旧 bot close 后
     * 读到它，导出空 context 池并把刚禁用的旧行重新写回 ACTIVE。
     */
    private void replaceSession(BotInstance newBot) {
        if (bot != null) {
            botSessionStore.disableBotSession(bot.getBotId());
            bot = null;
            bot.close();
        } else {
            bot = null;
        }
        saveSession(newBot, null);
    }

    /** 打开系统默认浏览器 */
    private static void openBrowser(String url) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
            } else {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            }
            log.info("已自动打开浏览器 → {}", url);
        } catch (IOException e) {
            log.warn("无法自动打开浏览器，请手动访问: {}", url, e);
        }
    }

    /** 获取当前 BotInstance */
    public BotInstance getPrimaryBot() {
        return bot;
    }

    /** 登录成功后保存 session */
    public void saveSession(BotInstance instance, String wxNickname) {
        ResumeContext ctx = instance.exportResumeContext();
        String json = codec.serialize(ctx);
        botSessionStore.saveBotSession(instance.getBotId(), json, wxNickname);
        lastPersistedJson = json;
        bot = instance;
    }

    /**
     * 周期持久化 session（每 60 秒）。
     *
     * <p>仅在当前 session 与上次落盘内容不同时写库（如用户发消息刷新了 context token、
     * 心跳推进了 updatesCursor），避免每 60 秒无谓的 DB 写入与日志噪音。
     * 这样重启后无需用户重新发消息即可向已对话过的用户主动推送。
     */
    @Scheduled(fixedDelay = 60_000)
    public void persistSessionPeriodically() {
        BotInstance current = bot;
        if (current == null) return;
        try {
            ResumeContext ctx = current.exportResumeContext();
            if (ctx == null || ctx.getLoginContext() == null) return;
            String json = codec.serialize(ctx);
            if (json.equals(lastPersistedJson)) return;
            // 写库前重检：期间可能已换号（replaceSession 置空/替换 bot），避免把旧 bot
            // 的空 context 池写回、复活刚禁用的行。
            if (current != bot) return;
            botSessionStore.saveBotSession(current.getBotId(), json, null);
            lastPersistedJson = json;
            log.info("Bot 会话周期持久化完成 | botId={} | contexts={}",
                    current.getBotId(), ctx.getConversationContexts().size());
        } catch (Exception e) {
            log.warn("Bot 会话周期持久化失败", e);
        }
    }

    // ==================== 序列化 / 反序列化 ====================

    private String serializeResumeContext(ResumeContext ctx) {
        return codec.serialize(ctx);
    }

    private ResumeContext deserializeResumeContext(String json) {
        return codec.deserialize(json);
    }

    @PreDestroy
    public void destroy() {
        if (pageServer != null) {
            pageServer.stop();
        }
        if (bot != null) {
            bot.close();
        }
    }
}
