package com.youkeda.exercise.claw.agent.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis 会话上下文存储
 *
 * 数据结构：
 * - Key: ctx:{userId}:msgs → LIST，每个元素是 Message 的 JSON
 * - 使用 RPUSH 追加、LTRIM 限长、EXPIRE 设 TTL
 */
@Component
@ConditionalOnProperty(name = "context.redis.enabled", havingValue = "true")
public class RedisContextStore implements ContextStore {

    private static final Logger log = LoggerFactory.getLogger(RedisContextStore.class);

    private static final String KEY_PREFIX = "ctx:";
    private static final String KEY_SUFFIX = ":msgs";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final RedisContextProperties props;

    public RedisContextStore(StringRedisTemplate redis, ObjectMapper mapper,
                              RedisContextProperties props) {
        this.redis = redis;
        this.mapper = mapper;
        this.props = props;
    }

    private String key(String userId) {
        return KEY_PREFIX + userId + KEY_SUFFIX;
    }

    // ==================== 查询 ====================

    @Override
    public List<Message> getHistory(String userId, int maxMessages) {
        String k = key(userId);
        List<String> jsons = redis.opsForList().range(k, -maxMessages, -1);
        if (jsons == null || jsons.isEmpty()) {
            return List.of();
        }
        List<Message> result = new ArrayList<>();
        for (String json : jsons) {
            try {
                Message msg = mapper.readValue(json, Message.class);
                if (msg.role() != null && msg.content() != null) {
                    result.add(msg);
                }
            } catch (Exception e) {
                log.warn("反序列化消息失败 | json={}", json);
            }
        }
        return result;
    }

    @Override
    public Message findLastByPrefix(String userId, String contentPrefix) {
        String k = key(userId);
        List<String> jsons = redis.opsForList().range(k, 0, -1);
        if (jsons == null) return null;
        for (int i = jsons.size() - 1; i >= 0; i--) {
            try {
                Message msg = mapper.readValue(jsons.get(i), Message.class);
                if (msg.content() != null && msg.content().startsWith(contentPrefix)) {
                    return msg;
                }
            } catch (JsonProcessingException ignored) {}
        }
        return null;
    }

    @Override
    public List<Message> findAllByPrefix(String userId, String contentPrefix) {
        String k = key(userId);
        List<String> jsons = redis.opsForList().range(k, 0, -1);
        if (jsons == null) return List.of();
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
            String k = key(userId);
            redis.opsForList().rightPush(k, json);
            redis.opsForList().trim(k, -props.getMaxMessages(), -1);
            redis.expire(k, Duration.ofDays(props.getTtlDays()));
        } catch (JsonProcessingException e) {
            log.error("序列化消息失败 | userId={}", userId, e);
        }
    }

    // ==================== 清除 ====================

    @Override
    public void clear(String userId) {
        redis.delete(key(userId));
        log.debug("已清除用户 Redis 上下文 | userId={}", userId);
    }

}
