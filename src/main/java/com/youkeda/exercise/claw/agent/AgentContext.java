package com.youkeda.exercise.claw.agent;

import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;

/**
 * Agent 执行上下文
 *
 * 封装一次 Agent 调用的所有输入信息，贯穿整个执行链路。
 * 未来可扩展：会话历史、用户画像、多轮对话状态等。
 */
public class AgentContext {

    /**
     * 用户标识
     */
    private String userId;

    /**
     * 用户消息文本（TEXT 类型时有效）
     */
    private String message;

    /**
     * 消息类型（TEXT / IMAGE）
     */
    private MessageType messageType;

    /**
     * 原始微信消息（IMAGE 类型时有效，携带 CDN 下载参数）
     */
    private WechatMessage rawMessage;

    // ========== 未来扩展字段 ==========

    /**
     * 会话 ID（预留：多轮对话支持）
     */
    private String conversationId;

    /**
     * 历史消息（预留：上下文记忆）
     */
    private java.util.List<String> history;

    public String getUserId() {
        return userId;
    }

    public AgentContext setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public AgentContext setMessage(String message) {
        this.message = message;
        return this;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public AgentContext setMessageType(MessageType messageType) {
        this.messageType = messageType;
        return this;
    }

    public WechatMessage getRawMessage() {
        return rawMessage;
    }

    public AgentContext setRawMessage(WechatMessage rawMessage) {
        this.rawMessage = rawMessage;
        return this;
    }

    public String getConversationId() {
        return conversationId;
    }

    public AgentContext setConversationId(String conversationId) {
        this.conversationId = conversationId;
        return this;
    }

    public java.util.List<String> getHistory() {
        return history;
    }

    public AgentContext setHistory(java.util.List<String> history) {
        this.history = history;
        return this;
    }
}
