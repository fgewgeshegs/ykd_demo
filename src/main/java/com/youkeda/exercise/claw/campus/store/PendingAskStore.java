package com.youkeda.exercise.claw.campus.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "storage.enabled", havingValue = "true")
public class PendingAskStore {

    private static final Logger log = LoggerFactory.getLogger(PendingAskStore.class);

    private final JdbcTemplate jdbc;

    public PendingAskStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 查指定考试类型的最新一条回答 */
    public Optional<String> findLatestAnswer(String noticeType) {
        try {
            String answer = jdbc.queryForObject("""
                SELECT answer FROM campus_pending_ask
                WHERE notice_type = ? AND status = 'ANSWERED'
                ORDER BY asked_at DESC LIMIT 1
                """, String.class, noticeType);
            return Optional.ofNullable(answer);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void save(String noticeType, String question, String status) {
        long now = System.currentTimeMillis() / 1000;
        jdbc.update("""
            INSERT INTO campus_pending_ask (notice_type, question, status, asked_at)
            VALUES (?, ?, ?, ?)
            """, noticeType, question, status, now);
    }

    public void updateAnswer(String noticeType, String answer) {
        long now = System.currentTimeMillis() / 1000;
        jdbc.update("""
            UPDATE campus_pending_ask SET answer=?, status='ANSWERED', answered_at=?
            WHERE notice_type=? AND status='PENDING'
            """, answer, now, noticeType);
    }
}
