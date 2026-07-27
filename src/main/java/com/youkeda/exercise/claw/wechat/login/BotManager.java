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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Bot 实例管理器。
 * 负责从 SQLite 恢复登录、首次扫码登录、生命周期管理。
 * 当前仅支持单 bot（列表中一个元素），预留多 bot 扩展。
 */
@Component
public class BotManager {

    private static final Logger log = LoggerFactory.getLogger(BotManager.class);

    private final SqliteDataStore sqliteDataStore;
    private final ObjectMapper objectMapper;
    private final List<BotInstance> bots = new CopyOnWriteArrayList<>();

    public BotManager(SqliteDataStore sqliteDataStore, ObjectMapper objectMapper) {
        this.sqliteDataStore = sqliteDataStore;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        // 尝试从 SQLite 恢复已有 session
        List<SqliteDataStore.BotSessionRow> sessions = sqliteDataStore.getActiveBotSessions();
        if (!sessions.isEmpty()) {
            for (SqliteDataStore.BotSessionRow row : sessions) {
                try {
                    ResumeContext ctx = deserializeResumeContext(row.resumeContextJson());
                    BotInstance bot = new BotInstance(ctx);
                    if (bot.isLoggedIn()) {
                        bots.add(bot);
                        log.info("Bot 会话恢复成功 | botId={}", row.botId());
                        return;
                    } else {
                        log.warn("Bot 会话已过期，已禁用 | botId={}", row.botId());
                        sqliteDataStore.disableBotSession(row.botId());
                    }
                } catch (Exception e) {
                    log.error("Bot 会话恢复失败 | botId={}", row.botId(), e);
                    sqliteDataStore.disableBotSession(row.botId());
                }
            }
        }

        // 无有效 session，发起首次扫码登录
        log.info("无已保存的 Bot 会话，发起扫码登录");
        startLoginFlow();
    }

    /**
     * 发起首次扫码登录流程：创建 BotInstance → 获取二维码 → 展示 → 等待扫码 → 保存 session
     */
    private void startLoginFlow() {
        BotInstance bot = new BotInstance();
        LoginPageServer pageServer = null;
        String qrResult;
        try {
            qrResult = bot.executeLogin();
        } catch (Exception e) {
            log.error("获取二维码失败", e);
            return;
        }

        try {
            if (qrResult.startsWith("http")) {
                // URL 格式：走本地登录页面
                LoginStateManager stateManager = new LoginStateManager();
                stateManager.updateQrUrl(qrResult);
                stateManager.updateStatus(LoginStatus.WAITING_SCAN);

                pageServer = new LoginPageServer(stateManager);
                int port = pageServer.start();
                openBrowser("http://127.0.0.1:" + port + "/login");

                // 轮询登录状态
                long deadline = System.currentTimeMillis() + 120_000;
                while (!bot.isLoggedIn() && System.currentTimeMillis() < deadline) {
                    Thread.sleep(1000);
                }
                if (bot.isLoggedIn()) {
                    stateManager.updateStatus(LoginStatus.SUCCESS);
                    log.info("微信登录成功");
                    saveSession(bot, null);
                    Thread.sleep(3000);
                } else {
                    stateManager.updateStatus(LoginStatus.TIMEOUT);
                    log.error("微信登录超时");
                    Thread.sleep(10000);
                }
            } else {
                // base64 格式：写文件兜底
                String qrBase64 = qrResult.contains(",")
                        ? qrResult.substring(qrResult.indexOf(",") + 1)
                        : qrResult;
                byte[] qrBytes = Base64.getDecoder().decode(qrBase64);
                Path qrFile = Path.of("qrcode.png");
                Files.write(qrFile, qrBytes);
                log.info("请扫码登录 → {}", qrFile.toAbsolutePath());

                long deadline = System.currentTimeMillis() + 120_000;
                while (!bot.isLoggedIn() && System.currentTimeMillis() < deadline) {
                    Thread.sleep(1000);
                }
                if (bot.isLoggedIn()) {
                    log.info("微信登录成功");
                    saveSession(bot, null);
                } else {
                    log.error("微信登录超时");
                }
            }
        } catch (Exception e) {
            log.error("微信登录异常", e);
        } finally {
            if (pageServer != null) {
                pageServer.stop();
            }
        }
    }

    /**
     * 打开系统默认浏览器
     */
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

    /** 获取当前活跃的 BotInstance（第一个） */
    public BotInstance getPrimaryBot() {
        if (bots.isEmpty()) return null;
        return bots.get(0);
    }

    /** 登录成功后保存 session */
    public void saveSession(BotInstance bot, String wxNickname) {
        ResumeContext ctx = bot.exportResumeContext();
        String json = serializeResumeContext(ctx);
        sqliteDataStore.saveBotSession(bot.getBotId(), json, wxNickname);
        if (!bots.contains(bot)) {
            bots.add(bot);
        }
    }

    /** 登录失败时清理 */
    public void disableBot(String botId) {
        sqliteDataStore.disableBotSession(botId);
        bots.removeIf(b -> b.getBotId().equals(botId));
    }

    public boolean hasActiveBot() {
        return bots.stream().anyMatch(BotInstance::isLoggedIn);
    }

    // ==================== 序列化 / 反序列化 ====================

    /**
     * 将 ResumeContext 序列化为 JSON。
     * ResumeContext 包含 LoginContext(botToken, userId, botId, baseUrl) + updatesCursor。
     */
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

    /**
     * 从 JSON 反序列化 ResumeContext。
     */
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
        CountDownLatch latch = new CountDownLatch(bots.size());
        for (BotInstance bot : bots) {
            Thread t = new Thread(() -> {
                try {
                    bot.close();
                } catch (Exception e) {
                    log.warn("关闭 BotInstance 异常 | botId={}", bot.getBotId(), e);
                } finally {
                    latch.countDown();
                }
            }, "close-bot-" + bot.getBotId());
            t.setDaemon(true);
            t.start();
        }
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                log.warn("Bot 关闭超时，强制退出 | remaining={}", latch.getCount());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        bots.clear();
    }
}
