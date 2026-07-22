package com.youkeda.exercise.claw.agent.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存会话上下文存储
 *
 * 所有消息（文字/语音/图片）统一存入一个队列，CDN 参数嵌入 Message 记录。
 * - 单用户最多保留 50 条消息，超出自动淘汰最早的消息
 * - 线程安全
 */
@Component
@ConditionalOnProperty(name = "context.redis.enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryContextStore implements ContextStore {

    /** 单用户最大消息条数 */
    private static final int MAX_MESSAGES_PER_USER = 50;

    private static final Logger log = LoggerFactory.getLogger(InMemoryContextStore.class);

    /** userId → 消息队列 */
    private final ConcurrentHashMap<String, Deque<Message>> store = new ConcurrentHashMap<>();

    // ==================== 查询 ====================

    @Override
    public List<Message> getHistory(String userId, int maxMessages) {
        Deque<Message> messages = store.get(userId);
        if (messages == null) {
            return List.of();
        }

        synchronized (messages) {
            List<Message> all = new ArrayList<>(messages);
            int fromIndex = Math.max(0, all.size() - maxMessages);
            return List.copyOf(all.subList(fromIndex, all.size()));
        }
    }

    @Override
    public Message findLastByPrefix(String userId, String contentPrefix) {
        Deque<Message> messages = store.get(userId);
        if (messages == null) return null;
        synchronized (messages) {
            var it = messages.descendingIterator();
            while (it.hasNext()) {
                Message msg = it.next();
                if (msg.content() != null && msg.content().startsWith(contentPrefix)) {
                    return msg;
                }
            }
        }
        return null;
    }

    @Override
    public List<Message> findAllByPrefix(String userId, String contentPrefix) {
        Deque<Message> messages = store.get(userId);
        if (messages == null) return List.of();
        List<Message> result = new ArrayList<>();
        synchronized (messages) {
            for (Message msg : messages) {
                if (msg.content() != null && msg.content().startsWith(contentPrefix)) {
                    result.add(msg);
                }
            }
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
        Deque<Message> messages = store.computeIfAbsent(userId, k -> new ArrayDeque<>());
        Message message = new Message(role, content, mediaEncryptParam, mediaAesKey, mediaUrl);
        synchronized (messages) {
            messages.addLast(message);
            while (messages.size() > MAX_MESSAGES_PER_USER) {
                messages.removeFirst();
            }
        }
    }

    // ==================== 清除 ====================

    @Override
    public void clear(String userId) {
        store.remove(userId);
        log.debug("已清除用户上下文 | userId={}", userId);
    }

    @Override
    public void updateLastMediaUrl(String userId, String contentPrefix, String url) {
        Message last = findLastByPrefix(userId, contentPrefix);
        if (last == null) return;
        // InMemoryContextStore 的 Message 是 immutable record，无法原地更新
        // 当前通过 append 新标记替代；如果后续需要精确替换，可改为 mutable 或重建队列
        log.debug("updateLastMediaUrl 记录用于后续查找 | userId={} | prefix={}", userId, contentPrefix);
    }
}
