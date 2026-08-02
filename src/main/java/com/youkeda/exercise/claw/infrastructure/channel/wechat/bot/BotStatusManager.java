package com.youkeda.exercise.claw.infrastructure.channel.wechat.bot;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 微信机器人登录状态管理器。
 *
 * <p>职责：持久化机器人登录状态（连接/断开/失败），
 * 项目重启后可通过 {@link #getLastStatus()} 快速判断上次登录结果。
 *
 * <p>注意：SDK 不支持 Token 持久化，每次重启必须重新扫码。
 * 此模块只记录状态元数据，不保存登录凭证。
 *
 * <p>P0-7 收敛：原「BotSessionManager」用裸 JDBC 写 {@code claw-bot.db}，
 * 与 {@link com.youkeda.exercise.claw.infrastructure.channel.wechat.login.BotSessionStore}
 * 的 {@code claw.db} 同名表 {@code bot_session} 双库并存，状态语义互不相关。
 * 现改为 JdbcTemplate + 主库 {@code claw.db}，表名改为 {@code bot_status}，消除同名双库。
 *
 * <p>表结构 {@code bot_status}：
 * <pre>
 * id          INTEGER PRIMARY KEY AUTOINCREMENT
 * account_id  TEXT NOT NULL              -- 固定 "default"，预留多账号
 * status      TEXT NOT NULL              -- CONNECTED / DISCONNECTED / FAILED
 * error_msg   TEXT                       -- 失败原因
 * login_time  TEXT                       -- 最近一次登录时间
 * create_time TEXT DEFAULT CURRENT_TIMESTAMP
 * </pre>
 */
@Component
public class BotStatusManager {

    private static final Logger log = LoggerFactory.getLogger(BotStatusManager.class);

    private static final String TABLE_DDL = """
            CREATE TABLE IF NOT EXISTS bot_status (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                account_id  TEXT NOT NULL,
                status      TEXT NOT NULL,
                error_msg   TEXT,
                login_time  TEXT,
                create_time TEXT DEFAULT (datetime('now','localtime'))
            )
            """;

    private static final String INDEX_SQL = """
            CREATE UNIQUE INDEX IF NOT EXISTS idx_bot_status_account
                ON bot_status(account_id)
            """;

    private static final String SELECT_SQL = """
            SELECT status, error_msg, login_time FROM bot_status
            WHERE account_id = ?
            ORDER BY id DESC LIMIT 1
            """;

    private static final String UPSERT_SQL = """
            INSERT INTO bot_status (account_id, status, error_msg, login_time)
            VALUES (?, ?, ?, datetime('now','localtime'))
            ON CONFLICT(account_id) DO UPDATE SET
                status         = excluded.status,
                error_msg      = excluded.error_msg,
                login_time     = excluded.login_time,
                create_time    = datetime('now','localtime')
            """;

    private final JdbcTemplate jdbc;

    /** 上次已知状态，启动时从 SQLite 加载 */
    private volatile String lastStatus;
    private volatile String lastError;
    private volatile String lastLoginTime;

    public BotStatusManager(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void init() {
        jdbc.execute(TABLE_DDL);
        jdbc.execute(INDEX_SQL);
        log.info("bot_status 表初始化完成（主库 claw.db）");
        loadLastState();
    }

    // ==================== 公开方法 ====================

    /** 记录机器人登录成功 */
    public void markConnected() {
        lastStatus = "CONNECTED";
        lastError = null;
        lastLoginTime = nowString();
        jdbc.update(UPSERT_SQL, "default", "CONNECTED", null);
        log.info("机器人登录状态已持久化: CONNECTED");
    }

    /** 记录机器人登录失败 */
    public void markFailed(String error) {
        lastStatus = "FAILED";
        lastError = error;
        jdbc.update(UPSERT_SQL, "default", "FAILED",
                error != null ? truncate(error, 500) : "未知错误");
        log.info("机器人登录状态已持久化: FAILED | error={}", error);
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
        jdbc.query(SELECT_SQL, rs -> {
            if (rs.next()) {
                lastStatus = rs.getString("status");
                lastError = rs.getString("error_msg");
                lastLoginTime = rs.getString("login_time");
                log.info("加载上次机器人登录状态: status={} | time={}",
                        lastStatus, lastLoginTime);
            } else {
                log.info("无历史登录状态，首次启动");
            }
            return null;
        }, "default");
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
