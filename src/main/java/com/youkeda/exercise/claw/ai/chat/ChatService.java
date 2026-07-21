package com.youkeda.exercise.claw.ai.chat;

import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.context.ContextStore;
import com.youkeda.exercise.claw.context.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 聊天服务
 *
 * 职责：封装多轮对话的业务逻辑，通过 ContextStore 维护上下文，
 * 调用 LLMClient 完成模型交互
 */
@Slf4j
@Service
public class ChatService {

    /** 每次请求携带的最大历史消息条数 */
    private static final int MAX_HISTORY = 20;

    private final LLMClient llmClient;
    private final ContextStore contextStore;

    public ChatService(LLMClient llmClient, ContextStore contextStore) {
        this.llmClient = llmClient;
        this.contextStore = contextStore;
    }

    /**
     * 生成对话回复（带上下文记忆）
     *
     * @param userId  用户标识
     * @param message 用户消息
     * @return 模型回复，失败时返回 null
     */
    public String chat(String userId, String message) {
        log.info("ChatService 开始处理 | user={} | text={}", userId, message);

        try {
            // 1. 获取历史上下文
            List<Message> history = contextStore.getHistory(userId, MAX_HISTORY);
            log.debug("获取历史消息 | user={} | historySize={}", userId, history.size());

            // 2. 调用 LLM（带历史）
            String reply = llmClient.chat(userId, message, history);
            if (reply == null || reply.isEmpty()) {
                log.warn("ChatService 回复为空 | user={}", userId);
                return null;
            }

            // 3. 保存回复到上下文（用户消息已由 WechatMessageService 统一存储）
            contextStore.append(userId, "assistant", reply);

            log.info("ChatService 处理完成 | user={}", userId);
            return reply;
        } catch (Exception e) {
            log.error("ChatService 处理异常 | user={} | error={}", userId, e.getMessage());
            return null;
        }
    }
}
