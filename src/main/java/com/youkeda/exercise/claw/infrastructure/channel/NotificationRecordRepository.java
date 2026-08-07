package com.youkeda.exercise.claw.infrastructure.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 通知发送记录仓库（SQLite）。
 *
 * <p>每次通知发送（无论成功/失败/走哪个通道）都入库审计。
 * 保留最近 30 天记录，通过 {@link #deleteOlderThan(int)} 定期清理。
 */
@Repository
public class NotificationRecordRepository {

    private static final Logger log = LoggerFactory.getLogger(NotificationRecordRepository.class);

    private static final String TABLE_DDL = """
            CREATE TABLE IF NOT EXISTS notification_record (
                id                INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id           TEXT    NOT NULL,
                notification_type TEXT    NOT NULL DEFAULT 'GENERAL',
                title             TEXT,
                content           TEXT    NOT NULL,
                channel           TEXT    NOT NULL,
                status            TEXT    NOT NULL DEFAULT 'SUCCESS',
                error_msg         TEXT,
                created_time      TEXT    NOT NULL DEFAULT (datetime('now','localtime'))
            )
            """;

    private static final String INDEX_TIME = """
            CREATE INDEX IF NOT EXISTS idx_notification_record_time
                ON notification_record(created_time)
            """;

    private static final String INDEX_USER = """
            CREATE INDEX IF NOT EXISTS idx_notification_record_user
                ON notification_record(user_id, created_time)
            """;

    private static final String INDEX_TYPE = """
            CREATE INDEX IF NOT EXISTS idx_notification_record_type
                ON notification_record(notification_type, created_time)
            """;

    private static final String INSERT_SQL = """
            INSERT INTO notification_record
                (user_id, notification_type, title, content, channel, status, error_msg)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final int MAX_ERROR_LENGTH = 500;
    private static final int MAX_TITLE_LENGTH = 100;

    private final JdbcTemplate jdbc;

    public NotificationRecordRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void init() {
        jdbc.execute(TABLE_DDL);
        jdbc.execute(INDEX_TIME);
        jdbc.execute(INDEX_USER);
        jdbc.execute(INDEX_TYPE);
        log.info("notification_record 表初始化完成");
    }

    /**
     * 保存一条通知记录。
     *
     * @param userId   用户标识
     * @param type     通知类型
     * @param content  通知正文
     * @param channel  实际使用的通道（WECHAT / EMAIL / NONE）
     * @param success  是否发送成功
     * @param errorMsg 失败原因（成功时传 null）
     */
    public void save(String userId, NotificationType type, String content,
                     String channel, boolean success, String errorMsg) {
        String title = extractTitle(content);
        jdbc.update(INSERT_SQL,
                userId,
                type.name(),
                title,
                content,
                channel,
                success ? "SUCCESS" : "FAILED",
                errorMsg != null ? truncate(errorMsg, MAX_ERROR_LENGTH) : null);
    }

    /**
     * 清理超过指定天数的旧记录。
     *
     * @param days 保留天数
     * @return 删除的记录数
     */
    public int deleteOlderThan(int days) {
        String cutoff = LocalDateTime.now().minusDays(days)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        int deleted = jdbc.update(
                "DELETE FROM notification_record WHERE created_time < ?", cutoff);
        if (deleted > 0) {
            log.debug("通知记录清理完成 | deleted={} | cutoff={}", deleted, cutoff);
        }
        return deleted;
    }

    private static String extractTitle(String content) {
        if (content == null || content.isBlank()) {
            return "(无内容)";
        }
        String firstLine = content.lines()
                .filter(l -> !l.isBlank())
                .findFirst()
                .orElse(content);
        return truncate(firstLine, MAX_TITLE_LENGTH);
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
