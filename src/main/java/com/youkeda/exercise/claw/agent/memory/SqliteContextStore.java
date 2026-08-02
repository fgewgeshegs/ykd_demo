package com.youkeda.exercise.claw.agent.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.user.WechatUserManager;
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
 * <p>单用户模式下，通过 {@link WechatUserManager} 自动解析当前活跃 userId。
 * 外部组件若需指定特定 userId 查询，可调用带 userId 参数的重载方法。
 *
 * <p>数据结构：
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
    private final WechatUserManager userManager;

    public SqliteContextStore(JdbcTemplate jdbc, ObjectMapper mapper,
                               StorageProperties props, WechatUserManager userManager) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.props = props;
        this.userManager = userManager;
    }

    // ==================== ContextStore 接口实现（单用户自动兜底） ====================

    @Override
    public List<Message> getHistory(int maxMessages) {
        return getHistory(resolveUserId(), maxMessages);
    }

    @Override
    public void append(String role, String content) {
        append(resolveUserId(), role, content, null, null, null);
    }

    @Override
    public void append(String role, String content,
                        String mediaEncryptParam, String mediaAesKey,
                        String mediaUrl) {
        append(resolveUserId(), role, content, mediaEncryptParam, mediaAesKey, mediaUrl);
    }

    @Override
    public void append(Message message) {
        append(resolveUserId(), message);
    }

    @Override
    public Message findLastByPrefix(String contentPrefix) {
        return findLastByPrefix(resolveUserId(), contentPrefix);
    }

    @Override
    public List<Message> findAllByPrefix(String contentPrefix) {
        return findAllByPrefix(resolveUserId(), contentPrefix);
    }

    @Override
    public void clear() {
        clear(resolveUserId());
    }

    // ==================== 指定 userId 的查询（供 UserBehaviorAnalyzer 等组件直接调用） ====================

    public List<Message> getHistory(String userId, int maxMessages) {
        // 查询最近 maxMessages 条消息（按时间正序）
        String sql = """
            SELECT message_json FROM context_messages
            WHERE user_id = ?
            ORDER BY created_at DESC, id DESC
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

    public Message findLastByPrefix(String userId, String contentPrefix) {
        String sql = """
            SELECT message_json FROM context_messages
            WHERE user_id = ?
            ORDER BY created_at DESC, id DESC
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

    public List<Message> findAllByPrefix(String userId, String contentPrefix) {
        String sql = """
            SELECT message_json FROM context_messages
            WHERE user_id = ?
            ORDER BY created_at ASC, id ASC
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

    public void append(String userId, String role, String content) {
        append(userId, role, content, null, null, null);
    }

    public void append(String userId, String role, String content,
                        String mediaEncryptParam, String mediaAesKey,
                        String mediaUrl) {
        append(userId, new Message(role, content, mediaEncryptParam, mediaAesKey, mediaUrl));
    }

    public void append(String userId, Message message) {
        try {
            String json = mapper.writeValueAsString(message);

            // 插入新消息
            jdbc.update("INSERT INTO context_messages (user_id, message_json) VALUES (?, ?)",
                userId, json);

            // 删除超出限制的旧消息（保留最新的 maxMessages 条）
            jdbc.update("""
                DELETE FROM context_messages
                WHERE user_id = ? AND id NOT IN (
                    SELECT id FROM context_messages
                    WHERE user_id = ?
                    ORDER BY created_at DESC, id DESC
                    LIMIT ?
                )
            """, userId, userId, props.getMaxMessages());

        } catch (JsonProcessingException e) {
            log.error("序列化消息失败 | userId={}", userId, e);
        }
    }

    public void clear(String userId) {
        jdbc.update("DELETE FROM context_messages WHERE user_id = ?", userId);
        log.debug("已清除用户 SQLite 上下文 | userId={}", userId);
    }

    // ==================== 内部方法 ====================

    private String resolveUserId() {
        String id = userManager.getOwnerUserId();
        return id != null ? id : "default";
    }
}
