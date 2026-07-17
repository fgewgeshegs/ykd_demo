package com.youkeda.exercise.claw.wechat.handler;

import com.youkeda.exercise.claw.llm.client.LLMClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * AI 聊天处理器
 *
 * 调用大模型生成回复，优先级最高
 * AI 调用失败时返回降级提示
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class AIChatHandler implements MessageHandler {

    private static final String FALLBACK_REPLY = "抱歉，我现在暂时无法回复，请稍后再试。";

    private final LLMClient llmClient;

    @Override
    public String handle(String userId, String text) {
        log.debug("AIChatHandler 处理消息 | from={} | text={}", userId, text);

        String reply = llmClient.chat(userId, text);
        if (reply == null || reply.isEmpty()) {
            log.warn("AI 回复为空，使用降级回复 | from={}", userId);
            return FALLBACK_REPLY;
        }

        return reply;
    }
}
