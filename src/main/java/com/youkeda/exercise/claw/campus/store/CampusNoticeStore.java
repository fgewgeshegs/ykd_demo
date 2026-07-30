package com.youkeda.exercise.claw.campus.store;

import com.youkeda.exercise.claw.domain.campus.NoticeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
@ConditionalOnProperty(name = "storage.enabled", havingValue = "true")
public class CampusNoticeStore {

    private static final Logger log = LoggerFactory.getLogger(CampusNoticeStore.class);

    private final JdbcTemplate jdbc;

    public CampusNoticeStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 与已有记录去重，返回全新的通知列表（并插入 DB）
     */
    public List<NoticeItem> deduplicate(List<NoticeItem> fetched) {
        List<NoticeItem> newNotices = new ArrayList<>();
        for (NoticeItem item : fetched) {
            try {
                jdbc.update("""
                    INSERT OR IGNORE INTO campus_notice (title, url, publish_at, status)
                    VALUES (?, ?, ?, 'UNPROCESSED')
                    """, item.getTitle(), item.getUrl(), item.getPublishAt());

                int count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM campus_notice WHERE url = ? AND status = 'UNPROCESSED'",
                    Integer.class, item.getUrl());
                if (count > 0) {
                    // 新插入的（INSERT OR IGNORE 成功，status=UNPROCESSED）
                    newNotices.add(item);
                }
            } catch (Exception e) {
                log.warn("去重写入失败 | url={}", item.getUrl(), e);
            }
        }
        if (!newNotices.isEmpty()) {
            log.info("发现新通知 | count={}", newNotices.size());
        }
        return newNotices;
    }

    public void update(NoticeItem notice) {
        String typeName = notice.getType() != null ? notice.getType().name() : "UNKNOWN";
        jdbc.update("""
            UPDATE campus_notice SET type=?, confidence=?, score_source=?,
                classifier_reason=?, status=?, processed_at=?
            WHERE url=?
            """, typeName, notice.getConfidence(), notice.getScoreSource(),
            notice.getClassifierReason(), "CLASSIFIED",
            System.currentTimeMillis() / 1000, notice.getUrl());
    }

    public void updateContent(Long id, String content) {
        jdbc.update("UPDATE campus_notice SET content = ? WHERE id = ?", content, id);
    }

    public List<NoticeItem> getUnprocessed() {
        return jdbc.query(
            "SELECT * FROM campus_notice WHERE status = 'UNPROCESSED' ORDER BY created_at ASC",
            new NoticeRowMapper());
    }

    private static class NoticeRowMapper implements RowMapper<NoticeItem> {
        @Override
        public NoticeItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            NoticeItem item = new NoticeItem();
            item.setId(rs.getLong("id"));
            item.setTitle(rs.getString("title"));
            item.setUrl(rs.getString("url"));
            item.setPublishAt(rs.getString("publish_at"));
            item.setContent(rs.getString("content"));
            item.setStatus(rs.getString("status"));
            return item;
        }
    }
}
