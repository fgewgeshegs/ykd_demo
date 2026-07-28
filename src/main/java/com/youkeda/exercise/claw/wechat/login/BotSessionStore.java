package com.youkeda.exercise.claw.wechat.login;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.util.List;

/**
 * Bot 登录会话持久化存储。
 *
 * <p>启动时自动恢复上次登录的微信 session，避免每次重启都重新扫码。
 * 使用 Spring 管理的 DataSource（connection pool），与 {@link SqliteContextStore} 共用同一数据库。
 */
@Component
public class BotSessionStore {

    private static final Logger log = LoggerFactory.getLogger(BotSessionStore.class);

    private final JdbcTemplate jdbc;

    public BotSessionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void init() {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS bot_session (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                bot_id          TEXT    NOT NULL UNIQUE,
                resume_context  TEXT    NOT NULL,
                wx_nickname     TEXT,
                status          TEXT    NOT NULL DEFAULT 'ACTIVE'
                                      CHECK (status IN ('ACTIVE', 'DISABLED')),
                created_at      INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
                last_active_at  INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
            )
        """);
        log.info("bot_session 表初始化完成");
    }

    /**
     * 获取所有活跃的 bot 会话（ACTIVE 状态）
     */
    public List<BotSessionRow> getActiveBotSessions() {
        return jdbc.query(
            "SELECT bot_id, resume_context, wx_nickname FROM bot_session WHERE status = 'ACTIVE'",
            (rs, rowNum) -> new BotSessionRow(
                rs.getString("bot_id"),
                rs.getString("resume_context"),
                rs.getString("wx_nickname")
            )
        );
    }

    /**
     * 保存或更新 bot 登录会话
     */
    public void saveBotSession(String botId, String resumeContextJson, String wxNickname) {
        long now = System.currentTimeMillis() / 1000;
        jdbc.update("""
            INSERT INTO bot_session(bot_id, resume_context, wx_nickname, status, created_at, last_active_at)
            VALUES (?, ?, ?, 'ACTIVE', ?, ?)
            ON CONFLICT(bot_id) DO UPDATE SET
                resume_context  = excluded.resume_context,
                wx_nickname     = excluded.wx_nickname,
                status          = 'ACTIVE',
                last_active_at  = excluded.last_active_at
            """, botId, resumeContextJson, wxNickname, now, now);
        log.info("Bot 会话已保存 | botId={}", botId);
    }

    /**
     * 禁用 bot 会话（切换账号或过期时调用）
     */
    public void disableBotSession(String botId) {
        jdbc.update("UPDATE bot_session SET status = 'DISABLED' WHERE bot_id = ?", botId);
        log.info("Bot 会话已禁用 | botId={}", botId);
    }

    /**
     * Bot 会话行记录
     */
    public record BotSessionRow(String botId, String resumeContextJson, String wxNickname) {}
}
