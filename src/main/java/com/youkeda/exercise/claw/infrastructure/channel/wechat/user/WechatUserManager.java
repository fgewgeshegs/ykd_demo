package com.youkeda.exercise.claw.infrastructure.channel.wechat.user;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 微信用户活跃记录管理器。
 *
 * <p>每次用户发消息时记录或更新用户信息，
 * 与 {@link com.youkeda.exercise.claw.agent.memory.ContextStore} 配合使用——
 * ContextStore 存聊天内容，此模块存用户身份元数据。
 *
 * <p>使用 Spring 主数据源（{@code claw.db}）。此前曾独立连 {@code claw-bot.db}，
 * 已在 DB 收敛重构中迁至主库，原独立库与 {@code bot.db-path} 配置一并退役。
 *
 * <p>表结构 {@code wechat_users}：
 * <pre>
 * user_id            TEXT PRIMARY KEY      -- 微信 SDK 提供的用户标识
 * nickname           TEXT                  -- 昵称（当前 SDK 未提供，预留）
 * avatar_url         TEXT                  -- 头像 URL（预留）
 * last_active_time   DATETIME              -- 最近活跃时间
 * first_active_time  DATETIME              -- 首次活跃时间
 * interaction_count  INTEGER DEFAULT 1     -- 交互次数
 * school_id          INTEGER               -- 用户绑定的学校 ID
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

    private static final String UPSERT_SQL = """
            INSERT INTO wechat_users (user_id, nickname, last_active_time, interaction_count)
            VALUES (?, ?, datetime('now', 'localtime'), 1)
            ON CONFLICT(user_id) DO UPDATE SET
                last_active_time   = datetime('now', 'localtime'),
                interaction_count  = wechat_users.interaction_count + 1
            """;

    private final JdbcTemplate jdbc;

    public WechatUserManager(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void init() {
        try {
            jdbc.execute(TABLE_DDL);
            log.info("wechat_users 表初始化完成 | datasource=claw.db");
        } catch (Exception e) {
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
        try {
            jdbc.update(UPSERT_SQL, userId,
                    nickname != null && !nickname.isBlank() ? nickname : null);
        } catch (Exception e) {
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
        try {
            Integer count = jdbc.queryForObject(
                    "SELECT interaction_count FROM wechat_users WHERE user_id = ?",
                    Integer.class, userId);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("查询用户交互次数失败 | userId={}", userId, e);
        }
        return 0;
    }

    /** 获取所有用户数 */
    public int getUserCount() {
        try {
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM wechat_users", Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
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
        try {
            return jdbc.queryForObject(
                    "SELECT school_id FROM wechat_users WHERE user_id = ?",
                    (rs, rowNum) -> {
                        long val = rs.getLong("school_id");
                        return rs.wasNull() ? null : val;
                    }, userId);
        } catch (Exception e) {
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
        try {
            int rows = jdbc.update("UPDATE wechat_users SET school_id = ? WHERE user_id = ?",
                    schoolId, userId);
            if (rows > 0) {
                log.info("用户学校绑定已更新 | userId={} | schoolId={}", userId, schoolId);
            }
        } catch (Exception e) {
            log.error("设置用户学校绑定失败 | userId={} | schoolId={}", userId, schoolId, e);
        }
    }

    /** 用户记录，用于控制台展示 */
    public record UserRecord(String userId, String nickname, String avatarUrl,
                             String lastActiveTime, String firstActiveTime,
                             int interactionCount) {}

    /** 获取所有用户记录（按最近活跃时间降序） */
    public List<UserRecord> getAllUsers() {
        try {
            return jdbc.query(
                    "SELECT user_id, nickname, avatar_url, last_active_time, " +
                    "first_active_time, interaction_count FROM wechat_users " +
                    "ORDER BY last_active_time DESC",
                    (rs, rowNum) -> new UserRecord(
                            rs.getString("user_id"),
                            rs.getString("nickname"),
                            rs.getString("avatar_url"),
                            rs.getString("last_active_time"),
                            rs.getString("first_active_time"),
                            rs.getInt("interaction_count")
                    ));
        } catch (Exception e) {
            log.error("查询所有用户失败", e);
        }
        return List.of();
    }

    /**
     * 获取这份个人助手的主人标识。
     *
     * <p>C 类收紧：单 owner 语义取「首次交互者」而非「最近交互者」——
     * 第一个给机器人发消息的人即为 owner，之后其他人发消息只更新
     * {@code last_active_time}，不会顶替 owner（避免 owner 悄悄漂移）。
     * 首次交互者 ID 由 SDK 在收到消息时自动捕获并写入 {@code wechat_users}，
     * 无需用户手工配置。
     */
    public String getOwnerUserId() {
        try {
            return jdbc.queryForObject(
                    "SELECT user_id FROM wechat_users "
                            + "ORDER BY first_active_time ASC LIMIT 1",
                    String.class);
        } catch (Exception e) {
            log.error("查询个人助手主人失败", e);
            return null;
        }
    }
}
