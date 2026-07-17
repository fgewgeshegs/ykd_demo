package com.youkeda.exercise.claw.ai.chat;

import com.youkeda.exercise.claw.llm.client.LLMClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 聊天服务
 *
 * 职责：封装文本对话的业务逻辑，调用 LLMClient 完成模型交互
 */
@Slf4j
@Service
public class ChatService {

    private final LLMClient llmClient;

    public ChatService(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    /**
     * 生成对话回复
     *
     * @param userId  用户标识
     * @param message 用户消息
     * @return 模型回复，失败时返回 null
     */
    public String chat(String userId, String message) {
        log.info("ChatService 开始处理 | user={} | text={}", userId, message);

        try {
            String reply = llmClient.chat(userId, message);
            if (reply == null || reply.isEmpty()) {
                log.warn("ChatService 回复为空 | user={}", userId);
                return null;
            }
            log.info("ChatService 处理完成 | user={}", userId);
            return reply;
        } catch (Exception e) {
            log.error("ChatService 处理异常 | user={} | error={}", userId, e.getMessage());
            return null;
        }
    }
}