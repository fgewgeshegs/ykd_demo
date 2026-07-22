package com.youkeda.exercise.claw.agent.tool;

import com.youkeda.exercise.claw.agent.AgentContext;
import com.youkeda.exercise.claw.agent.ReActAgentExecutor;
import com.youkeda.exercise.claw.agent.classify.Intent;
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
 * <p>所有 TEXT 消息的入口，委托 {@link ReActAgentExecutor} 执行 tool-calling 循环。
 * 同时作为 Tool 和 WechatMessageHandler 暴露。启动时自动注册到 ToolRegistry。
 */
@Component
public class ChatTool implements Tool, WechatMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatTool.class);
    private static final String FALLBACK_REPLY = "抱歉，我现在暂时无法回复，请稍后再试。";

    private final ReActAgentExecutor agentExecutor;
    private final ToolRegistry toolRegistry;

    public ChatTool(ReActAgentExecutor agentExecutor, ToolRegistry toolRegistry) {
        this.agentExecutor = agentExecutor;
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
        return "AI 文本对话，支持多轮聊天和工具调用（天气查询、图片生成等）";
    }

    @Override
    public Intent[] supportedIntents() {
        return new Intent[]{Intent.CHAT};
    }

    @Override
    public String execute(AgentContext context) {
        log.info("ChatTool 执行 | user={} | text={}", context.getUserId(), context.getMessage());
        return agentExecutor.execute(context);
    }

    @Override
    public WechatReply handle(WechatMessage message) {
        if (message.getType() != MessageType.TEXT) {
            return null;
        }

        log.debug("ChatTool.handle 处理消息 | from={} | text={}", message.getUserId(), message.getText());

        String reply = agentExecutor.execute(new AgentContext()
                .setUserId(message.getUserId())
                .setMessage(message.getText())
                .setMessageType(MessageType.TEXT));

        if (reply == null || reply.isEmpty()) {
            log.warn("AI 回复为空，使用降级回复 | from={}", message.getUserId());
            return WechatReply.text(FALLBACK_REPLY);
        }

        return WechatReply.text(reply);
    }
}
