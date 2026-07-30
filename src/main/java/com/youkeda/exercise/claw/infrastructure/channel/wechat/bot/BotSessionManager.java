package com.youkeda.exercise.claw.infrastructure.channel.wechat.bot;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.*;

/**
 * 微信机器人登录状态管理器。
 *
 * <p>职责：持久化机器人登录状态（连接/断开/失败），
 * 项目重启后可通过 {@link #getLastStatus()} 快速判断上次登录结果。
 *
 * <p>注意：SDK 不支持 Token 持久化，每次重启必须重新扫码。
 * 此模块只记录状态元数据，不保存登录凭证。
 *
 * <p>表结构 {@code bot_session}：
 * <pre>
 * id          INTEGER PRIMARY KEY AUTOINCREMENT
 * account_id  TEXT NOT NULL              -- 固定 "default"，预留多账号
 * status      TEXT NOT NULL              -- CONNECTED / DISCONNECTED / FAILED
 * error_msg   TEXT                       -- 失败原因
 * login_time  DATETIME                   -- 最近一次登录时间
 * create_time DATETIME DEFAULT CURRENT_TIMESTAMP
 * </pre>
 */
@Component
public class BotSessionManager {

    private static final Logger log = LoggerFactory.getLogger(BotSessionManager.class);

    private static final String TABLE_DDL = """
            CREATE TABLE IF NOT EXISTS bot_session (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                account_id  TEXT NOT NULL,
                status      TEXT NOT NULL,
                error_msg   TEXT,
                login_time  DATETIME,
                create_time DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """;

    private static final String INDEX_SQL = """
            CREATE UNIQUE INDEX IF NOT EXISTS idx_bot_session_account
                ON bot_session(account_id)
            """;

    private static final String SELECT_SQL = """
            SELECT status, error_msg, login_time FROM bot_session
            WHERE account_id = ?
            ORDER BY id DESC LIMIT 1
            """;

    private static final String UPSERT_SQL = """
            INSERT INTO bot_session (account_id, status, error_msg, login_time)
            VALUES (?, ?, ?, datetime('now', 'localtime'))
            ON CONFLICT(account_id) DO UPDATE SET
                status         = excluded.status,
                error_msg      = excluded.error_msg,
                login_time     = excluded.login_time,
                create_time    = datetime('now', 'localtime')
            """;

    private final String dbPath;

    /** 上次已知状态，启动时从 SQLite 加载 */
    private volatile String lastStatus;
    private volatile String lastError;
    private volatile String lastLoginTime;

    @Autowired
    public BotSessionManager(@Value("${bot.db-path:./data/claw-bot.db}") String dbPath) {
        this.dbPath = dbPath;
    }

    @PostConstruct
    public void init() {
        ensureDirectory();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(TABLE_DDL);
            stmt.execute(INDEX_SQL);
            log.info("bot_session 表初始化完成 | path={}", dbPath);
        } catch (SQLException e) {
            log.error("bot_session 表初始化失败", e);
            throw new RuntimeException("bot_session 表初始化失败", e);
        }
        loadLastState();
    }

    // ==================== 公开方法 ====================

    /** 记录机器人登录成功 */
    public void markConnected() {
        lastStatus = "CONNECTED";
        lastError = null;
        lastLoginTime = nowString();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
            ps.setString(1, "default");
            ps.setString(2, "CONNECTED");
            ps.setNull(3, Types.VARCHAR);
            ps.executeUpdate();
            log.info("机器人登录状态已持久化: CONNECTED");
        } catch (SQLException e) {
            log.error("保存登录状态失败", e);
        }
    }

    /** 记录机器人登录失败 */
    public void markFailed(String error) {
        lastStatus = "FAILED";
        lastError = error;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
            ps.setString(1, "default");
            ps.setString(2, "FAILED");
            ps.setString(3, error != null ? truncate(error, 500) : "未知错误");
            ps.executeUpdate();
            log.info("机器人登录状态已持久化: FAILED | error={}", error);
        } catch (SQLException e) {
            log.error("保存登录状态失败", e);
        }
    }

    /** 获取上次登录状态：CONNECTED / FAILED / null（从未登录） */
    public String getLastStatus() {
        return lastStatus;
    }

    /** 获取上次错误信息 */
    public String getLastError() {
        return lastError;
    }

    /** 获取上次登录时间（字符串格式） */
    public String getLastLoginTime() {
        return lastLoginTime;
    }

    /** 上次是否登录成功过 */
    public boolean wasPreviouslyConnected() {
        return "CONNECTED".equals(lastStatus);
    }

    // ==================== 内部方法 ====================

    private void loadLastState() {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_SQL)) {
            ps.setString(1, "default");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    lastStatus = rs.getString("status");
                    lastError = rs.getString("error_msg");
                    lastLoginTime = rs.getString("login_time");
                    log.info("加载上次机器人登录状态: status={} | time={}",
                            lastStatus, lastLoginTime);
                } else {
                    log.info("无历史登录状态，首次启动");
                }
            }
        } catch (SQLException e) {
            log.error("加载登录状态失败", e);
        }
    }

    private Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
        }
        return conn;
    }

    private void ensureDirectory() {
        File dbFile = new File(dbPath);
        File parentDir = dbFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
    }

    private static String nowString() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date());
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}