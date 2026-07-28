package com.youkeda.exercise.claw.agent.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite 会话上下文存储
 *
 * 数据结构：
 * - 表: context_messages，每行是一条 Message 的 JSON
 * - 使用 INSERT 追加、DELETE 限长、created_at 做 TTL
 */
@Component
@ConditionalOnProperty(name = "storage.enabled", havingValue = "true")
public class SqliteContextStore implements ContextStore {

    private static final Logger log = LoggerFactory.getLogger(SqliteContextStore.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final StorageProperties props;

    public SqliteContextStore(JdbcTemplate jdbc, ObjectMapper mapper, StorageProperties props) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.props = props;
    }

    // ==================== 查询 ====================

    @Override
    public List<Message> getHistory(String userId, int maxMessages) {
        // 查询最近 maxMessages 条消息（按时间正序）
        String sql = """
            SELECT message_json FROM context_messages
            WHERE user_id = ?
            ORDER BY created_at DESC
            LIMIT ?
        """;

        List<String> jsons = jdbc.queryForList(sql, String.class, userId, maxMessages);
        if (jsons.isEmpty()) {
            return List.of();
        }

        // 反转为正序（查询是 DESC，需要反转）
        List<Message> result = new ArrayList<>();
        for (int i = jsons.size() - 1; i >= 0; i--) {
            try {
                Message msg = mapper.readValue(jsons.get(i), Message.class);
                if (msg.role() != null && msg.content() != null) {
                    result.add(msg);
                }
            } catch (Exception e) {
                log.warn("反序列化消息失败 | json={}", jsons.get(i));
            }
        }
        return result;
    }

    @Override
    public Message findLastByPrefix(String userId, String contentPrefix) {
        String sql = """
            SELECT message_json FROM context_messages
            WHERE user_id = ?
            ORDER BY created_at DESC
        """;

        List<String> jsons = jdbc.queryForList(sql, String.class, userId);
        for (String json : jsons) {
            try {
                Message msg = mapper.readValue(json, Message.class);
                if (msg.content() != null && msg.content().startsWith(contentPrefix)) {
                    return msg;
                }
            } catch (JsonProcessingException ignored) {}
        }
        return null;
    }

    @Override
    public List<Message> findAllByPrefix(String userId, String contentPrefix) {
        String sql = """
            SELECT message_json FROM context_messages
            WHERE user_id = ?
            ORDER BY created_at ASC
        """;

        List<String> jsons = jdbc.queryForList(sql, String.class, userId);
        List<Message> result = new ArrayList<>();
        for (String json : jsons) {
            try {
                Message msg = mapper.readValue(json, Message.class);
                if (msg.content() != null && msg.content().startsWith(contentPrefix)) {
                    result.add(msg);
                }
            } catch (JsonProcessingException ignored) {}
        }
        return result;
    }

    // ==================== 写入 ====================

    @Override
    public void append(String userId, String role, String content) {
        append(userId, role, content, null, null, null);
    }

    @Override
    public void append(String userId, String role, String content,
                        String mediaEncryptParam, String mediaAesKey,
                        String mediaUrl) {
        try {
            Message msg = new Message(role, content, mediaEncryptParam, mediaAesKey, mediaUrl);
            String json = mapper.writeValueAsString(msg);

            // 插入新消息
            jdbc.update("INSERT INTO context_messages (user_id, message_json) VALUES (?, ?)",
                userId, json);

            // 删除超出限制的旧消息（保留最新的 maxMessages 条）
            jdbc.update("""
                DELETE FROM context_messages
                WHERE user_id = ? AND id NOT IN (
                    SELECT id FROM context_messages
                    WHERE user_id = ?
                    ORDER BY created_at DESC
                    LIMIT ?
                )
            """, userId, userId, props.getMaxMessages());

        } catch (JsonProcessingException e) {
            log.error("序列化消息失败 | userId={}", userId, e);
        }
    }

    // ==================== 清除 ====================

    @Override
    public void clear(String userId) {
        jdbc.update("DELETE FROM context_messages WHERE user_id = ?", userId);
        log.debug("已清除用户 SQLite 上下文 | userId={}", userId);
    }
}
