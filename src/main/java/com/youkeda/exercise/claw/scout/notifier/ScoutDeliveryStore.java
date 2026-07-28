package com.youkeda.exercise.claw.scout.notifier;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Scout 本地投递记录。
 *
 * <p>与微信用户信息共用本地 SQLite 文件，防止应用重启后重复推送。
 */
@Component
public class ScoutDeliveryStore {

    private static final Logger log = LoggerFactory.getLogger(ScoutDeliveryStore.class);

    private static final String TABLE_DDL = """
            CREATE TABLE IF NOT EXISTS scout_deliveries (
                user_id       TEXT NOT NULL,
                item_key      TEXT NOT NULL,
                delivered_at  INTEGER NOT NULL,
                PRIMARY KEY (user_id, item_key)
            )
            """;

    private final String dbPath;

    public ScoutDeliveryStore(@Value("${bot.db-path:./data/claw-bot.db}") String dbPath) {
        this.dbPath = dbPath;
    }

    @PostConstruct
    public void init() {
        ensureDirectory();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(TABLE_DDL);
            stmt.execute("""
                    CREATE INDEX IF NOT EXISTS idx_scout_deliveries_time
                    ON scout_deliveries(delivered_at)
                    """);
        } catch (SQLException e) {
            throw new IllegalStateException("Scout 投递记录初始化失败", e);
        }
    }

    public boolean wasDeliveredSince(String userId, String itemKey, long sinceMillis) {
        if (userId == null || userId.isBlank() || itemKey == null || itemKey.isBlank()) {
            return false;
        }
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     SELECT 1 FROM scout_deliveries
                     WHERE user_id = ? AND item_key = ? AND delivered_at >= ?
                     LIMIT 1
                     """)) {
            ps.setString(1, userId);
            ps.setString(2, itemKey);
            ps.setLong(3, sinceMillis);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("查询 Scout 投递记录失败 | userId={}", userId, e);
            return false;
        }
    }

    public void markDelivered(String userId, String itemKey, long deliveredAt) {
        if (userId == null || userId.isBlank() || itemKey == null || itemKey.isBlank()) {
            return;
        }
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("""
                     INSERT INTO scout_deliveries(user_id, item_key, delivered_at)
                     VALUES (?, ?, ?)
                     ON CONFLICT(user_id, item_key) DO UPDATE SET
                         delivered_at = excluded.delivered_at
                     """)) {
            ps.setString(1, userId);
            ps.setString(2, itemKey);
            ps.setLong(3, deliveredAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("记录 Scout 投递失败 | userId={}", userId, e);
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
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw new IllegalStateException("无法创建 Scout 数据目录: " + parentDir);
        }
    }
}
