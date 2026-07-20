package com.youkeda.exercise.claw.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存会话上下文存储
 *
 * 数据结构：ConcurrentHashMap<userId, Deque<Message>>
 * - 单用户最多保留 50 条消息，超出自动淘汰最早的消息
 * - 线程安全
 */
@Slf4j
@Component
public class InMemoryContextStore implements ContextStore {

    /** 单用户最大消息条数 */
    private static final int MAX_MESSAGES_PER_USER = 50;

    /** userId → 消息队列 */
    private final ConcurrentHashMap<String, Deque<Message>> store = new ConcurrentHashMap<>();

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
    public void append(String userId, String role, String content) {
        Deque<Message> messages = store.computeIfAbsent(userId, k -> new ArrayDeque<>());

        Message message = new Message(role, content);

        synchronized (messages) {
            messages.addLast(message);
            while (messages.size() > MAX_MESSAGES_PER_USER) {
                messages.removeFirst();
            }
        }
    }

    @Override
    public void clear(String userId) {
        store.remove(userId);
        log.debug("已清除用户上下文 | userId={}", userId);
    }
}
