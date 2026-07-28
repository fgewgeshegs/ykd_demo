package com.youkeda.exercise.claw.common;

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

        // 创建团建方案草稿表
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS team_trip_plans (
                user_id TEXT PRIMARY KEY,
                plan_json TEXT NOT NULL,
                updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
            )
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
            "DELETE FROM team_trip_plans WHERE updated_at < ?", planExpireTime);
        if (planDeleted > 0) {
            log.info("已清理 {} 条过期团建方案", planDeleted);
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
