package com.youkeda.exercise.claw.wechat.login;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wechat.ilink.sdk.core.context.ResumeContext;
import com.github.wechat.ilink.sdk.core.login.LoginContext;
import com.youkeda.exercise.claw.agent.memory.SqliteDataStore;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final SqliteDataStore sqliteDataStore;
    private final ObjectMapper objectMapper;
    private BotInstance bot;

    public BotManager(SqliteDataStore sqliteDataStore, ObjectMapper objectMapper) {
        this.sqliteDataStore = sqliteDataStore;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        // 1. 恢复旧 session，机器人立即开始工作
        List<SqliteDataStore.BotSessionRow> sessions = sqliteDataStore.getActiveBotSessions();
        for (SqliteDataStore.BotSessionRow row : sessions) {
            try {
                ResumeContext ctx = deserializeResumeContext(row.resumeContextJson());
                BotInstance instance = new BotInstance(ctx);
                if (instance.isLoggedIn()) {
                    bot = instance;
                    log.info("Bot 会话恢复成功 | botId={}", row.botId());
                } else {
                    log.warn("Bot 会话已过期 | botId={}", row.botId());
                    sqliteDataStore.disableBotSession(row.botId());
                }
            } catch (Exception e) {
                log.error("Bot 会话恢复失败 | botId={}", row.botId(), e);
                sqliteDataStore.disableBotSession(row.botId());
            }
        }

        // 2. 后台弹二维码供换号用（不阻塞启动，超时不影响旧 session）
        log.info("后台弹出二维码（扫码可切换微信账号）");
        startLoginFlow();
    }

    /**
     * 后台线程获取二维码并等待扫码。
     * 扫码成功则替换当前 bot；超时不影响现有会话。
     */
    private void startLoginFlow() {
        Thread t = new Thread(() -> {
            BotInstance qrBot = new BotInstance();
            LoginPageServer pageServer = null;
            String qrResult;
            try {
                qrResult = qrBot.executeLogin();
            } catch (Exception e) {
                log.error("获取二维码失败", e);
                return;
            }

            try {
                if (qrResult.startsWith("http")) {
                    LoginStateManager stateManager = new LoginStateManager();
                    stateManager.updateQrUrl(qrResult);
                    stateManager.updateStatus(LoginStatus.WAITING_SCAN);

                    pageServer = new LoginPageServer(stateManager);
                    int port = pageServer.start();
                    openBrowser("http://127.0.0.1:" + port + "/login");

                    long deadline = System.currentTimeMillis() + 120_000;
                    while (!qrBot.isLoggedIn() && System.currentTimeMillis() < deadline) {
                        Thread.sleep(1000);
                    }
                    if (qrBot.isLoggedIn()) {
                        stateManager.updateStatus(LoginStatus.SUCCESS);
                        log.info("扫码成功，切换账号");
                        replaceSession(qrBot);
                        Thread.sleep(3000);
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
            } finally {
                if (pageServer != null) {
                    pageServer.stop();
                }
            }
        }, "qr-login");
        t.setDaemon(true);
        t.start();
    }

    /** 用新扫码的 bot 替换当前 session，旧 bot 关闭并禁用 */
    private void replaceSession(BotInstance newBot) {
        if (bot != null) {
            sqliteDataStore.disableBotSession(bot.getBotId());
            bot.close();
        }
        bot = null;
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
        String json = serializeResumeContext(ctx);
        sqliteDataStore.saveBotSession(instance.getBotId(), json, wxNickname);
        bot = instance;
    }

    // ==================== 序列化 / 反序列化 ====================

    private String serializeResumeContext(ResumeContext ctx) {
        try {
            LoginContext lc = ctx.getLoginContext();
            var map = new java.util.LinkedHashMap<String, Object>();
            map.put("botToken", lc.getBotToken());
            map.put("userId", lc.getUserId());
            map.put("botId", lc.getBotId());
            map.put("baseUrl", lc.getBaseUrl());
            map.put("updatesCursor", ctx.getUpdatesCursor());
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化 ResumeContext 失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private ResumeContext deserializeResumeContext(String json) {
        try {
            var map = objectMapper.readValue(json, java.util.Map.class);
            String botToken = (String) map.get("botToken");
            String userId = (String) map.get("userId");
            String botId = (String) map.get("botId");
            String baseUrl = (String) map.get("baseUrl");
            String updatesCursor = (String) map.get("updatesCursor");

            LoginContext lc = new LoginContext(botToken, userId, botId, baseUrl);
            ResumeContext.Builder builder = ResumeContext.builder(lc);
            if (updatesCursor != null) {
                builder.updatesCursor(updatesCursor);
            }
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException("反序列化 ResumeContext 失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (bot != null) {
            bot.close();
        }
    }
}
