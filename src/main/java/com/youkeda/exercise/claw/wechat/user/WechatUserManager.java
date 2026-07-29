package com.youkeda.exercise.claw.wechat.user;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 微信用户活跃记录管理器。
 *
 * <p>每次用户发消息时记录或更新用户信息，
 * 与 {@link com.youkeda.exercise.claw.agent.memory.ContextStore} 配合使用——
 * ContextStore 存聊天内容，此模块存用户身份元数据。
 *
 * <p>表结构 {@code wechat_users}：
 * <pre>
 * user_id            TEXT PRIMARY KEY      -- 微信 SDK 提供的用户标识
 * nickname           TEXT                  -- 昵称（当前 SDK 未提供，预留）
 * avatar_url         TEXT                  -- 头像 URL（预留）
 * last_active_time   DATETIME              -- 最近活跃时间
 * first_active_time  DATETIME              -- 首次活跃时间
 * interaction_count  INTEGER DEFAULT 1     -- 交互次数
 * </pre>
 */
@Component
public class WechatUserManager {

    private static final Logger log = LoggerFactory.getLogger(WechatUserManager.class);

    private static final String TABLE_DDL = """
            CREATE TABLE IF NOT EXISTS wechat_users (
                user_id            TEXT PRIMARY KEY,
                nickname           TEXT,
                avatar_url         TEXT,
                school_id          INTEGER,
                last_active_time   DATETIME DEFAULT (datetime('now', 'localtime')),
                first_active_time  DATETIME DEFAULT (datetime('now', 'localtime')),
                interaction_count  INTEGER DEFAULT 1
            )
            """;

    /** 旧数据库迁移：新增 school_id 列 */
    private static final String MIGRATE_ADD_SCHOOL_ID = """
            ALTER TABLE wechat_users ADD COLUMN school_id INTEGER
            """;

    private static final String UPSERT_SQL = """
            INSERT INTO wechat_users (user_id, nickname, last_active_time, interaction_count)
            VALUES (?, ?, datetime('now', 'localtime'), 1)
            ON CONFLICT(user_id) DO UPDATE SET
                last_active_time   = datetime('now', 'localtime'),
                interaction_count  = wechat_users.interaction_count + 1
            """;

    private final String dbPath;

    @Autowired
    public WechatUserManager(@Value("${bot.db-path:./data/claw-bot.db}") String dbPath) {
        this.dbPath = dbPath;
    }

    @PostConstruct
    public void init() {
        ensureDirectory();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(TABLE_DDL);
            // 迁移：为已有数据库添加 school_id 列
            try {
                stmt.execute(MIGRATE_ADD_SCHOOL_ID);
                log.info("数据库迁移完成：已添加 school_id 列");
            } catch (SQLException e) {
                log.debug("school_id 列已存在，跳过迁移");
            }
            log.info("wechat_users 表初始化完成 | path={}", dbPath);
        } catch (SQLException e) {
            log.error("wechat_users 表初始化失败", e);
            throw new RuntimeException("wechat_users 表初始化失败", e);
        }
    }

    // ==================== 公开方法 ====================

    /**
     * 记录用户的一次交互。
     * <p>新用户自动插入，老用户更新活跃时间和交互次数。
     *
     * @param userId  微信 SDK 提供的用户标识，不可为空
     * @param nickname 用户昵称（当前 SDK 未提供，传 null 即可）
     */
    public void recordInteraction(String userId, String nickname) {
        if (userId == null || userId.isBlank()) return;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(UPSERT_SQL)) {
            ps.setString(1, userId);
            if (nickname != null && !nickname.isBlank()) {
                ps.setString(2, nickname);
            } else {
                ps.setNull(2, Types.VARCHAR);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("记录用户交互失败 | userId={}", userId, e);
        }
    }

    /** 简化调用：不传昵称 */
    public void recordInteraction(String userId) {
        recordInteraction(userId, null);
    }

    /**
     * 获取用户交互次数。
     *
     * @return 交互次数，用户不存在返回 0
     */
    public int getInteractionCount(String userId) {
        if (userId == null) return 0;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT interaction_count FROM wechat_users WHERE user_id = ?")) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("interaction_count");
                }
            }
        } catch (SQLException e) {
            log.error("查询用户交互次数失败 | userId={}", userId, e);
        }
        return 0;
    }

    /** 获取所有用户数 */
    public int getUserCount() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM wechat_users")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("查询用户数失败", e);
        }
        return 0;
    }

    // ==================== 学校绑定 ====================

    /**
     * 获取用户绑定的学校 ID
     *
     * @param userId 用户标识
     * @return 学校 ID，未绑定返回 null
     */
    public Long getUserSchoolId(String userId) {
        if (userId == null || userId.isBlank()) return null;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT school_id FROM wechat_users WHERE user_id = ?")) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long val = rs.getLong("school_id");
                    if (!rs.wasNull()) {
                        return val;
                    }
                }
            }
        } catch (SQLException e) {
            log.error("查询用户学校绑定失败 | userId={}", userId, e);
        }
        return null;
    }

    /**
     * 设置用户绑定的学校 ID
     *
     * @param userId   用户标识
     * @param schoolId 学校 ID（传 null 解除绑定）
     */
    public void setUserSchoolId(String userId, Long schoolId) {
        if (userId == null || userId.isBlank()) return;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE wechat_users SET school_id = ? WHERE user_id = ?")) {
            if (schoolId != null) {
                ps.setLong(1, schoolId);
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, userId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                log.info("用户学校绑定已更新 | userId={} | schoolId={}", userId, schoolId);
            }
        } catch (SQLException e) {
            log.error("设置用户学校绑定失败 | userId={} | schoolId={}", userId, schoolId, e);
        }
    }

    /** 用户记录，用于控制台展示 */
    public record UserRecord(String userId, String nickname, String avatarUrl,
                             String lastActiveTime, String firstActiveTime,
                             int interactionCount) {}

    /** 获取所有用户记录（按最近活跃时间降序） */
    public List<UserRecord> getAllUsers() {
        List<UserRecord> result = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT user_id, nickname, avatar_url, last_active_time, " +
                     "first_active_time, interaction_count FROM wechat_users " +
                     "ORDER BY last_active_time DESC")) {
            while (rs.next()) {
                result.add(new UserRecord(
                        rs.getString("user_id"),
                        rs.getString("nickname"),
                        rs.getString("avatar_url"),
                        rs.getString("last_active_time"),
                        rs.getString("first_active_time"),
                        rs.getInt("interaction_count")
                ));
            }
        } catch (SQLException e) {
            log.error("查询所有用户失败", e);
        }
        return result;
    }

    /**
     * 获取这份个人助手最近交互的主人标识。
     *
     * <p>项目按单用户本地安装设计；标识来自 SQLite，应用重启后仍然有效。
     */
    public String getOwnerUserId() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT user_id FROM wechat_users "
                             + "ORDER BY last_active_time DESC LIMIT 1")) {
            return rs.next() ? rs.getString("user_id") : null;
        } catch (SQLException e) {
            log.error("查询个人助手主人失败", e);
            return null;
        }
    }

    // ==================== 内部方法 ====================

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
}
