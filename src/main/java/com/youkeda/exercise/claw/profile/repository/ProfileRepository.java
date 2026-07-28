package com.youkeda.exercise.claw.profile.repository;

import com.youkeda.exercise.claw.profile.model.UserProfile;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户画像 SQLite 持久化仓库
 *
 * <p>独立数据库文件 {@code ./data/claw-profiles.db}，与记忆系统分离。
 * JDBC 直连 + WAL 模式。
 *
 * <p>表结构：profile_summary（单表存储所有画像条目）
 */
@Repository
public class ProfileRepository {

    private static final Logger log = LoggerFactory.getLogger(ProfileRepository.class);

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== DDL ====================

    private static final String TABLE_DDL = """
            CREATE TABLE IF NOT EXISTS profile_summary (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id       TEXT NOT NULL,
                profile_type  TEXT NOT NULL,
                profile_key   TEXT NOT NULL,
                profile_value TEXT NOT NULL,
                updated_time  TEXT NOT NULL DEFAULT (datetime('now','localtime'))
            )
            """;

    private static final String INDEX_USER = """
            CREATE INDEX IF NOT EXISTS idx_profile_user ON profile_summary(user_id, profile_type)
            """;

    // ==================== SQL ====================

    private static final String INSERT = """
            INSERT INTO profile_summary (user_id, profile_type, profile_key, profile_value, updated_time)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String SELECT_BY_USER = """
            SELECT id, user_id, profile_type, profile_key, profile_value, updated_time
            FROM profile_summary
            WHERE user_id = ?
            ORDER BY profile_type ASC, profile_key ASC
            """;

    private static final String SELECT_BY_USER_AND_TYPE = """
            SELECT id, user_id, profile_type, profile_key, profile_value, updated_time
            FROM profile_summary
            WHERE user_id = ? AND profile_type = ?
            ORDER BY profile_key ASC
            """;

    private static final String DELETE = """
            DELETE FROM profile_summary WHERE id = ? AND user_id = ?
            """;

    private static final String DELETE_BY_TYPE_AND_KEY = """
            DELETE FROM profile_summary WHERE user_id = ? AND profile_type = ? AND profile_key = ?
            """;

    @Value("${profile.db-path:./data/claw-profiles.db}")
    private String dbPath;

    public ProfileRepository() {
    }

    @PostConstruct
    public void init() {
        ensureDirectoryExists();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(TABLE_DDL);
            stmt.execute(INDEX_USER);
            log.info("画像表初始化完成 | path={}", dbPath);
        } catch (SQLException e) {
            log.error("画像表初始化失败 | path={}", dbPath, e);
            throw new RuntimeException("画像表初始化失败", e);
        }
    }

    // ==================== 写入 ====================

    /**
     * 保存画像条目（若已存在相同 type+key 则覆盖）
     */
    public void saveProfile(UserProfile profile) {
        // 先删除旧的相同 type+key
        deleteProfileByTypeAndKey(profile.getUserId(), profile.getProfileType(), profile.getProfileKey());

        String now = LocalDateTime.now().format(DTF);
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setString(1, profile.getUserId());
            ps.setString(2, profile.getProfileType());
            ps.setString(3, profile.getProfileKey());
            ps.setString(4, profile.getProfileValue());
            ps.setString(5, now);
            ps.executeUpdate();
            log.debug("画像已保存 | userId={} | type={} | key={} | value={}",
                    profile.getUserId(), profile.getProfileType(),
                    profile.getProfileKey(), profile.getProfileValue());
        } catch (SQLException e) {
            log.error("保存画像失败 | userId={} | type={} | key={}",
                    profile.getUserId(), profile.getProfileType(), profile.getProfileKey(), e);
        }
    }

    // ==================== 查询 ====================

    /**
     * 查询用户全部画像
     */
    public List<UserProfile> findProfiles(String userId) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_USER)) {
            ps.setString(1, userId);
            List<UserProfile> results = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapProfile(rs));
                }
            }
            return results;
        } catch (SQLException e) {
            log.error("查询画像失败 | userId={}", userId, e);
            return List.of();
        }
    }

    /**
     * 按类型查询画像
     */
    public List<UserProfile> findProfilesByType(String userId, String profileType) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_USER_AND_TYPE)) {
            ps.setString(1, userId);
            ps.setString(2, profileType);
            List<UserProfile> results = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapProfile(rs));
                }
            }
            return results;
        } catch (SQLException e) {
            log.error("查询类型画像失败 | userId={} | type={}", userId, profileType, e);
            return List.of();
        }
    }

    // ==================== 删除 ====================

    /**
     * 删除单条画像
     */
    public boolean deleteProfile(Long id, String userId) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {
            ps.setLong(1, id);
            ps.setString(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("删除画像失败 | id={} | userId={}", id, userId, e);
            return false;
        }
    }

    /**
     * 按类型和 key 删除画像（upsert 用）
     */
    private void deleteProfileByTypeAndKey(String userId, String profileType, String profileKey) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_BY_TYPE_AND_KEY)) {
            ps.setString(1, userId);
            ps.setString(2, profileType);
            ps.setString(3, profileKey);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("删除画像失败 | userId={} | type={} | key={}",
                    userId, profileType, profileKey, e);
        }
    }

    // ==================== 内部方法 ====================

    private UserProfile mapProfile(ResultSet rs) throws SQLException {
        UserProfile p = new UserProfile();
        p.setId(rs.getLong("id"));
        p.setUserId(rs.getString("user_id"));
        p.setProfileType(rs.getString("profile_type"));
        p.setProfileKey(rs.getString("profile_key"));
        p.setProfileValue(rs.getString("profile_value"));
        p.setUpdatedTimeFromString(rs.getString("updated_time"));
        return p;
    }

    private Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
        }
        return conn;
    }

    private void ensureDirectoryExists() {
        File dbFile = new File(dbPath);
        File parentDir = dbFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (created) {
                log.info("已创建数据库目录 | path={}", parentDir.getAbsolutePath());
            }
        }
    }
}