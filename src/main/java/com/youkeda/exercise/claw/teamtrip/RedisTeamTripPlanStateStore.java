package com.youkeda.exercise.claw.teamtrip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.memory.RedisContextProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Redis 版团建方案状态存储。 */
@Component
@ConditionalOnProperty(name = "context.redis.enabled", havingValue = "true")
public class RedisTeamTripPlanStateStore implements TeamTripPlanStateStore {

    private static final Logger log = LoggerFactory.getLogger(RedisTeamTripPlanStateStore.class);
    private static final String KEY_PREFIX = "trip-plan:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final RedisContextProperties properties;

    public RedisTeamTripPlanStateStore(StringRedisTemplate redis, ObjectMapper objectMapper,
                                       RedisContextProperties properties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public TeamTripPlanDraft get(String userId) {
        String json = redis.opsForValue().get(key(userId));
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, TeamTripPlanDraft.class);
        } catch (Exception e) {
            log.warn("读取团建方案状态失败 | user={} | error={}", userId, e.getMessage());
            return null;
        }
    }

    @Override
    public void save(String userId, TeamTripPlanDraft draft) {
        try {
            redis.opsForValue().set(key(userId), objectMapper.writeValueAsString(draft),
                    Duration.ofDays(properties.getTtlDays()));
        } catch (Exception e) {
            log.error("保存团建方案状态失败 | user={} | error={}", userId, e.getMessage());
        }
    }

    @Override
    public void clear(String userId) {
        redis.delete(key(userId));
    }

    private String key(String userId) {
        return KEY_PREFIX + userId;
    }
}
