package com.youkeda.exercise.claw.infrastructure.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.io.File;

/**
 * SQLite 数据库初始化器
 *
 * 应用启动时自动创建表结构，清理过期记录
 */
@Component
@ConditionalOnProperty(name = "storage.enabled", havingValue = "true")
public class SqliteDatabaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(SqliteDatabaseInitializer.class);

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    public SqliteDatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        ensureDatabaseDirectory();
        log.info("正在初始化 SQLite 数据库...");
        createTables();
        cleanExpiredRecords();
        log.info("SQLite 数据库初始化完成");
    }

    private void createTables() {
        // 创建对话上下文表
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS context_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id TEXT NOT NULL,
                message_json TEXT NOT NULL,
                created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
            )
        """);

        // 创建索引
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_context_user_id ON context_messages(user_id)
        """);

        // 创建旅游方案草稿表
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS travel_plans (
                user_id TEXT PRIMARY KEY,
                plan_json TEXT NOT NULL,
                updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
            )
        """);

        // 创建校园配置表
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS campus_config (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                school      TEXT NOT NULL,
                class_name  TEXT NOT NULL DEFAULT '',
                enabled     INTEGER NOT NULL DEFAULT 1,
                extra_config TEXT,
                created_at  INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
                updated_at  INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
            )
        """);

        // 创建校园通知表
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS campus_notice (
                id                INTEGER PRIMARY KEY AUTOINCREMENT,
                title             TEXT NOT NULL,
                url               TEXT NOT NULL UNIQUE,
                publish_at        TEXT,
                content           TEXT DEFAULT '',
                type              TEXT DEFAULT 'UNKNOWN',
                confidence        REAL DEFAULT 0,
                score_source      TEXT DEFAULT 'NONE',
                classifier_reason TEXT DEFAULT '',
                status            TEXT DEFAULT 'UNPROCESSED',
                processed_at      INTEGER,
                created_at        INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
            )
        """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_notice_url ON campus_notice(url)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_notice_status ON campus_notice(status)");

        // 创建待确认通知表
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS campus_pending_ask (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                notice_type   TEXT NOT NULL,
                question      TEXT NOT NULL,
                answer        TEXT DEFAULT '',
                status        TEXT DEFAULT 'PENDING',
                asked_at      INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
                answered_at   INTEGER
            )
        """);
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_pending_type ON campus_pending_ask(notice_type, status)
        """);

        // 创建技能会话表
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS skill_sessions (
                user_id TEXT PRIMARY KEY,
                active_skill TEXT NOT NULL,
                previous_skill TEXT,
                context_json TEXT NOT NULL DEFAULT '{}',
                activated_at INTEGER NOT NULL,
                last_activity_at INTEGER NOT NULL,
                inactivity_count INTEGER NOT NULL DEFAULT 0
            )
        """);

        // === 校园通知框架迁移：为 campus_notice 添加 source 列 ===
        try {
            jdbcTemplate.execute("ALTER TABLE campus_notice ADD COLUMN source TEXT NOT NULL DEFAULT 'EXAM'");
            log.info("DB迁移完成：campus_notice 添加 source 列");
        } catch (Exception e) {
            log.debug("campus_notice.source 列已存在，跳过迁移");
        }

        try {
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_notice_source ON campus_notice(source)");
        } catch (Exception e) {
            log.debug("idx_notice_source 索引已存在，跳过");
        }

        // === 迁移：为 campus_pending_ask 添加 source 列（默认 'EXAM' 兼容旧数据） ===
        try {
            jdbcTemplate.execute("ALTER TABLE campus_pending_ask ADD COLUMN source TEXT NOT NULL DEFAULT 'EXAM'");
            log.info("DB迁移完成：campus_pending_ask 添加 source 列");
        } catch (Exception e) {
            log.debug("campus_pending_ask.source 列已存在，跳过迁移");
        }

        try {
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_pending_source_type ON campus_pending_ask(source, notice_type, status)");
        } catch (Exception e) {
            log.debug("idx_pending_source_type 索引已存在，跳过");
        }

        // === 动漫通知表 ===
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS anime_subscription (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                anilist_id  INTEGER NOT NULL UNIQUE,
                title       TEXT NOT NULL,
                title_ja    TEXT DEFAULT '',
                cover_url   TEXT DEFAULT '',
                status      TEXT DEFAULT 'RELEASING',
                genres      TEXT DEFAULT '[]',
                created_at  INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
            )
        """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS anime_schedule (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                anilist_id  INTEGER NOT NULL,
                episode     INTEGER NOT NULL,
                airing_at   INTEGER NOT NULL,
                notified    INTEGER NOT NULL DEFAULT 0,
                created_at  INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
                UNIQUE(anilist_id, episode)
            )
        """);
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_anime_schedule_airing
            ON anime_schedule(airing_at, notified)
        """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS anime_reminder_task (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                anilist_id  INTEGER NOT NULL,
                episode     INTEGER NOT NULL,
                remind_time INTEGER NOT NULL,
                status      TEXT NOT NULL DEFAULT 'PENDING',
                created_at  INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
            )
        """);
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_reminder_status_time
            ON anime_reminder_task(status, remind_time)
        """);

        log.debug("数据库表结构创建完成");
    }

    private void cleanExpiredRecords() {
        // 清理 7 天前的对话记录（默认 TTL）
        long expireTime = System.currentTimeMillis() / 1000 - 7 * 24 * 3600;
        int deleted = jdbcTemplate.update(
            "DELETE FROM context_messages WHERE created_at < ?", expireTime);
        if (deleted > 0) {
            log.info("已清理 {} 条过期对话记录", deleted);
        }

        // 清理 30 天前的团建方案（团建方案保留更长时间）
        long planExpireTime = System.currentTimeMillis() / 1000 - 30 * 24 * 3600;
        int planDeleted = jdbcTemplate.update(
            "DELETE FROM travel_plans WHERE updated_at < ?", planExpireTime);
        if (planDeleted > 0) {
            log.info("已清理 {} 条过期旅游方案", planDeleted);
        }
    }

    /**
     * 确保 SQLite 数据库文件所在目录存在
     */
    private void ensureDatabaseDirectory() {
        // jdbc:sqlite:data/claw.db → data/claw.db
        String path = datasourceUrl.replace("jdbc:sqlite:", "");
        File dbFile = new File(path);
        File parentDir = dbFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (created) {
                log.info("已创建数据库目录: {}", parentDir.getAbsolutePath());
            }
        }
    }
}
