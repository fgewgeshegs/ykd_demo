package com.youkeda.exercise.claw.agent.tool;

import com.youkeda.exercise.claw.agent.AgentContext;
import com.youkeda.exercise.claw.agent.classify.Intent;
import com.youkeda.exercise.claw.ai.chat.ChatService;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 聊天工具
 *
 * 封装 ChatService，同时作为 Tool 和 WechatMessageHandler 暴露。
 * 启动时自动注册到 ToolRegistry。
 */
@Component
public class ChatTool implements Tool, WechatMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatTool.class);
    private static final String FALLBACK_REPLY = "抱歉，我现在暂时无法回复，请稍后再试。";

    private final ChatService chatService;
    private final ToolRegistry toolRegistry;

    public ChatTool(ChatService chatService, ToolRegistry toolRegistry) {
        this.chatService = chatService;
        this.toolRegistry = toolRegistry;
    }

    @PostConstruct
    public void init() {
        toolRegistry.register(this);
    }

    @Override
    public String name() {
        return "chat";
    }

    @Override
    public String description() {
        return "AI 文本对话，支持多轮聊天";
    }

    @Override
    public Intent[] supportedIntents() {
        return new Intent[]{Intent.CHAT};
    }

    @Override
    public String execute(AgentContext context) {
        log.info("ChatTool 执行 | user={} | text={}", context.getUserId(), context.getMessage());
        return chatService.chatWithTools(context.getUserId(), context.getMessage());
    }

    @Override
    public WechatReply handle(WechatMessage message) {
        if (message.getType() != MessageType.TEXT) {
            return null;
        }

        log.debug("ChatTool.handle 处理消息 | from={} | text={}", message.getUserId(), message.getText());

        String reply = chatService.chatWithTools(message.getUserId(), message.getText());
        if (reply == null || reply.isEmpty()) {
            log.warn("AI 回复为空，使用降级回复 | from={}", message.getUserId());
            return WechatReply.text(FALLBACK_REPLY);
        }

        return WechatReply.text(reply);
    }
}
