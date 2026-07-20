package com.youkeda.exercise.claw.agent;

import com.youkeda.exercise.claw.ai.chat.ChatService;
import com.youkeda.exercise.claw.ai.classifier.Intent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 聊天工具
 *
 * 封装 ChatService，以 Tool 接口暴露给 Agent 体系。
 * 启动时自动注册到 ToolRegistry。
 */
@Component
public class ChatTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(ChatTool.class);

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
        return chatService.chat(context.getUserId(), context.getMessage());
    }
}