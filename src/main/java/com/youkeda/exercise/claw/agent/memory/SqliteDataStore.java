package com.youkeda.exercise.claw.agent.memory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "context.sqlite.enabled", havingValue = "true")
public class SqliteDataStore implements ContextStore {

    private static final Logger log = LoggerFactory.getLogger(SqliteDataStore.class);

    private final SqliteContextProperties props;
    private final DataSource dataSource;

    // 定时清理
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public SqliteDataStore(SqliteContextProperties props) {
        this.props = props;

        // 确保数据库父目录存在
        try {
            Path dbFile = Path.of(props.getDbPath());
            Path parent = dbFile.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
                log.info("创建数据库目录 | path={}", parent);
            }
        } catch (Exception e) {
            throw new RuntimeException("创建数据库目录失败 | path=" + props.getDbPath(), e);
        }

        // 初始化 HikariCP 连接池
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + props.getDbPath());
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionInitSql(
            "PRAGMA journal_mode=WAL;" +
            "PRAGMA busy_timeout=" + props.getBusyTimeoutMs() + ";" +
            "PRAGMA synchronous=NORMAL;" +
            "PRAGMA cache_size=-64000;" +
            "PRAGMA foreign_keys=ON;"
        );
        config.setPoolName("claw-sqlite");
        this.dataSource = new HikariDataSource(config);
    }

    @PostConstruct
    public void init() {
        initTables();
        startTtlCleanup();
        log.info("SQLite 数据存储初始化完成 | path={}", props.getDbPath());
    }

    @PreDestroy
    public void destroy() {
        scheduler.shutdown();
        if (dataSource instanceof HikariDataSource hds) {
            hds.close();
        }
    }

    // ==================== 表初始化 ====================

    private void initTables() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bot_session (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    bot_id          TEXT    NOT NULL UNIQUE,
                    resume_context  TEXT    NOT NULL,
                    wx_nickname     TEXT,
                    status          TEXT    NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'DISABLED')),
                    created_at      INTEGER NOT NULL,
                    last_active_at  INTEGER NOT NULL
                )
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS chat_messages (
                    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                    bot_id              TEXT    NOT NULL,
                    role                TEXT    NOT NULL CHECK (role IN ('user', 'assistant', 'system')),
                    content             TEXT,
                    media_encrypt_param TEXT,
                    media_aes_key       TEXT,
                    media_url           TEXT,
                    created_at          INTEGER NOT NULL
                )
            """);
            stmt.execute("""
                CREATE INDEX IF NOT EXISTS idx_msg_created_at
                    ON chat_messages(created_at DESC)
            """);
        } catch (SQLException e) {
            throw new RuntimeException("初始化 SQLite 表结构失败", e);
        }
    }

    // ==================== 定时 TTL 清理 ====================

    private void startTtlCleanup() {
        scheduler.scheduleAtFixedRate(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM chat_messages WHERE created_at < ?")) {
                long cutoff = System.currentTimeMillis() - (long) props.getTtlDays() * 86400000L;
                ps.setLong(1, cutoff);
                int deleted = ps.executeUpdate();
                if (deleted > 0) {
                    log.info("TTL 清理完成 | deleted={} | cutoffDays={}", deleted, props.getTtlDays());
                }
            } catch (SQLException e) {
                log.warn("TTL 清理失败", e);
            }
        }, 1, 1, TimeUnit.DAYS);
    }

    // ==================== BotSession 操作 ====================

    /**
     * 保存 bot 登录会话（插入或更新）
     */
    public void saveBotSession(String botId, String resumeContextJson, String wxNickname) {
        long now = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO bot_session(bot_id, resume_context, wx_nickname, status, created_at, last_active_at)
                VALUES (?, ?, ?, 'ACTIVE', ?, ?)
                ON CONFLICT(bot_id) DO UPDATE SET
                    resume_context = excluded.resume_context,
                    wx_nickname = excluded.wx_nickname,
                    status = 'ACTIVE',
                    last_active_at = excluded.last_active_at
            """)) {
            ps.setString(1, botId);
            ps.setString(2, resumeContextJson);
            ps.setString(3, wxNickname);
            ps.setLong(4, now);
            ps.setLong(5, now);
            ps.executeUpdate();
            log.info("Bot 会话已保存 | botId={}", botId);
        } catch (SQLException e) {
            log.error("保存 Bot 会话失败 | botId={}", botId, e);
        }
    }

    /**
     * 获取所有活跃的 bot 会话
     */
    public List<BotSessionRow> getActiveBotSessions() {
        List<BotSessionRow> rows = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT bot_id, resume_context, wx_nickname FROM bot_session WHERE status = 'ACTIVE'");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new BotSessionRow(
                    rs.getString("bot_id"),
                    rs.getString("resume_context"),
                    rs.getString("wx_nickname")
                ));
            }
        } catch (SQLException e) {
            log.error("读取活跃 Bot 会话失败", e);
        }
        return rows;
    }

    /**
     * 禁用 bot 会话（过期/异常时调用）
     */
    public void disableBotSession(String botId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "UPDATE bot_session SET status = 'DISABLED' WHERE bot_id = ?")) {
            ps.setString(1, botId);
            ps.executeUpdate();
            log.info("Bot 会话已禁用 | botId={}", botId);
        } catch (SQLException e) {
            log.error("禁用 Bot 会话失败 | botId={}", botId, e);
        }
    }

    public record BotSessionRow(String botId, String resumeContextJson, String wxNickname) {}

    // ==================== ContextStore 实现 ====================

    /**
     * 获取当前活动的 botId（单 bot 模式取第一个活跃 session 的 bot_id）
     * 如果没有活跃 session 则返回 "default"
     */
    private String resolveBotId() {
        List<BotSessionRow> sessions = getActiveBotSessions();
        if (!sessions.isEmpty()) {
            return sessions.get(0).botId();
        }
        return "default";
    }

    @Override
    public List<Message> getHistory(int maxMessages) {
        String botId = resolveBotId();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                SELECT role, content, media_encrypt_param, media_aes_key, media_url
                FROM chat_messages
                WHERE bot_id = ?
                ORDER BY id DESC
                LIMIT ?
            """)) {
            ps.setString(1, botId);
            ps.setInt(2, maxMessages);
            ResultSet rs = ps.executeQuery();
            List<Message> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new Message(
                    rs.getString("role"),
                    rs.getString("content"),
                    rs.getString("media_encrypt_param"),
                    rs.getString("media_aes_key"),
                    rs.getString("media_url")
                ));
            }
            Collections.reverse(result);
            return result;
        } catch (SQLException e) {
            log.error("读取对话历史失败", e);
            return List.of();
        }
    }

    @Override
    public void append(String role, String content) {
        append(role, content, null, null, null);
    }

    @Override
    public void append(String role, String content,
                       String mediaEncryptParam, String mediaAesKey, String mediaUrl) {
        String botId = resolveBotId();
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 插入新消息
                try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO chat_messages(bot_id, role, content,
                        media_encrypt_param, media_aes_key, media_url, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
                    ps.setString(1, botId);
                    ps.setString(2, role);
                    ps.setString(3, content);
                    ps.setString(4, mediaEncryptParam);
                    ps.setString(5, mediaAesKey);
                    ps.setString(6, mediaUrl);
                    ps.setLong(7, System.currentTimeMillis());
                    ps.executeUpdate();
                }

                // 惰性淘汰：只保留最近 N 条
                try (PreparedStatement ps = conn.prepareStatement("""
                    DELETE FROM chat_messages
                    WHERE bot_id = ? AND id NOT IN (
                        SELECT id FROM chat_messages
                        WHERE bot_id = ?
                        ORDER BY id DESC
                        LIMIT ?
                    )
                """)) {
                    ps.setString(1, botId);
                    ps.setString(2, botId);
                    ps.setInt(3, props.getMaxMessages());
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            log.error("追加消息失败", e);
        }
    }

    @Override
    public Message findLastByPrefix(String contentPrefix) {
        String botId = resolveBotId();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                SELECT role, content, media_encrypt_param, media_aes_key, media_url
                FROM chat_messages
                WHERE bot_id = ? AND content LIKE ? || '%'
                ORDER BY id DESC
                LIMIT 1
            """)) {
            ps.setString(1, botId);
            ps.setString(2, contentPrefix);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Message(
                    rs.getString("role"),
                    rs.getString("content"),
                    rs.getString("media_encrypt_param"),
                    rs.getString("media_aes_key"),
                    rs.getString("media_url")
                );
            }
        } catch (SQLException e) {
            log.error("findLastByPrefix 失败", e);
        }
        return null;
    }

    @Override
    public List<Message> findAllByPrefix(String contentPrefix) {
        String botId = resolveBotId();
        List<Message> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                SELECT role, content, media_encrypt_param, media_aes_key, media_url
                FROM chat_messages
                WHERE bot_id = ? AND content LIKE ? || '%'
                ORDER BY id ASC
            """)) {
            ps.setString(1, botId);
            ps.setString(2, contentPrefix);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(new Message(
                    rs.getString("role"),
                    rs.getString("content"),
                    rs.getString("media_encrypt_param"),
                    rs.getString("media_aes_key"),
                    rs.getString("media_url")
                ));
            }
        } catch (SQLException e) {
            log.error("findAllByPrefix 失败", e);
        }
        return result;
    }

    @Override
    public void clear() {
        String botId = resolveBotId();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "DELETE FROM chat_messages WHERE bot_id = ?")) {
            ps.setString(1, botId);
            ps.executeUpdate();
            log.debug("已清除对话历史");
        } catch (SQLException e) {
            log.error("清除对话历史失败", e);
        }
    }
}
