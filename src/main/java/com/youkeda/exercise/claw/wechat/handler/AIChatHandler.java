package com.youkeda.exercise.claw.wechat.handler;

import com.youkeda.exercise.claw.ai.chat.ChatService;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AI 聊天处理器
 *
 * 职责：接收 TEXT 类型的消息，委托 ChatService 完成对话逻辑
 * 不包含业务逻辑，仅负责请求接收与结果转交
 */
@Slf4j
@Component
public class AIChatHandler implements MessageHandler {

    private static final String FALLBACK_REPLY = "抱歉，我现在暂时无法回复，请稍后再试。";

    private final ChatService chatService;

    public AIChatHandler(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public WechatReply handle(WechatMessage message) {
        if (message.getType() != MessageType.TEXT) {
            return null;
        }

        log.debug("AIChatHandler 处理消息 | from={} | text={}", message.getUserId(), message.getText());

        String reply = chatService.chat(message.getUserId(), message.getText());
        if (reply == null || reply.isEmpty()) {
            log.warn("AI 回复为空，使用降级回复 | from={}", message.getUserId());
            return WechatReply.text(FALLBACK_REPLY);
        }

        return WechatReply.text(reply);
    }
}