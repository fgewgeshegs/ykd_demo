package com.youkeda.exercise.claw.infrastructure.channel.wechat.login;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Bot 登录会话持久化存储。
 *
 * <p>启动时自动恢复上次登录的微信 session，避免每次重启都重新扫码。
 * 使用 Spring 管理的 DataSource（connection pool），与 {@link SqliteContextStore} 共用同一数据库。
 */
@Component
public class BotSessionStore {

    private static final Logger log = LoggerFactory.getLogger(BotSessionStore.class);
    static final long SESSION_VALIDITY_SECONDS = Duration.ofDays(7).toSeconds();

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
                last_active_at  INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
                authenticated_at INTEGER,
                expires_at       INTEGER
            )
        """);
        ensureColumn("authenticated_at", "INTEGER");
        ensureColumn("expires_at", "INTEGER");
        log.info("bot_session 表初始化完成");
    }

    /**
     * 获取所有活跃的 bot 会话（ACTIVE 状态）
     */
    public List<BotSessionRow> getActiveBotSessions() {
        long now = Instant.now().getEpochSecond();
        int expired = jdbc.update("""
            UPDATE bot_session
            SET status = 'DISABLED'
            WHERE status = 'ACTIVE'
              AND (expires_at IS NULL OR expires_at <= ?)
            """, now);
        if (expired > 0) {
            log.info("已禁用过期的 Bot 会话 | count={}", expired);
        }
        return jdbc.query(
            """
            SELECT bot_id, resume_context, wx_nickname, authenticated_at, expires_at
            FROM bot_session
            WHERE status = 'ACTIVE' AND expires_at > ?
            ORDER BY authenticated_at DESC
            """,
            (rs, rowNum) -> new BotSessionRow(
                rs.getString("bot_id"),
                rs.getString("resume_context"),
                rs.getString("wx_nickname"),
                rs.getLong("authenticated_at"),
                rs.getLong("expires_at")
            ),
            now
        );
    }

    /**
     * 保存或更新 bot 登录会话
     */
    public void saveBotSession(String botId, String resumeContextJson, String wxNickname) {
        long now = Instant.now().getEpochSecond();
        long expiresAt = now + SESSION_VALIDITY_SECONDS;
        jdbc.update("""
            INSERT INTO bot_session(
                bot_id, resume_context, wx_nickname, status,
                created_at, last_active_at, authenticated_at, expires_at)
            VALUES (?, ?, ?, 'ACTIVE', ?, ?, ?, ?)
            ON CONFLICT(bot_id) DO UPDATE SET
                resume_context  = excluded.resume_context,
                wx_nickname     = excluded.wx_nickname,
                status          = 'ACTIVE',
                last_active_at  = excluded.last_active_at,
                authenticated_at = excluded.authenticated_at,
                expires_at       = excluded.expires_at
            """, botId, resumeContextJson, wxNickname, now, now, now, expiresAt);
        log.info("Bot 会话已保存 | botId={} | expiresAt={}", botId, expiresAt);
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
    private void ensureColumn(String columnName, String definition) {
        List<Map<String, Object>> columns = jdbc.queryForList("PRAGMA table_info(bot_session)");
        boolean exists = columns.stream()
                .anyMatch(column -> columnName.equals(column.get("name")));
        if (!exists) {
            jdbc.execute("ALTER TABLE bot_session ADD COLUMN " + columnName + " " + definition);
        }
    }

    public record BotSessionRow(
            String botId,
            String resumeContextJson,
            String wxNickname,
            long authenticatedAt,
            long expiresAt) {}
}
