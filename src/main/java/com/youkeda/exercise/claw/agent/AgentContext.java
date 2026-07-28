package com.youkeda.exercise.claw.agent;

import com.youkeda.exercise.claw.agent.model.PlanState;
import com.youkeda.exercise.claw.memory.model.UserMemoryContext;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;

/**
 * Agent 执行上下文
 *
 * 封装一次 Agent 调用的所有输入信息，贯穿整个执行链路。
 * {@link #planState} 为可选字段——非多步骤任务时为 null，0 额外开销。
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

    /**
     * 当前会话的计划状态（可选，简单任务时为 null）
     */
    private PlanState planState;

    /**
     * 用户长期记忆上下文（由 MemoryRetriever 在执行前填充）
     */
    private UserMemoryContext userMemoryContext;


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

    public PlanState getPlanState() { return planState; }

    public AgentContext setPlanState(PlanState planState) {
        this.planState = planState;
        return this;
    }

    public UserMemoryContext getUserMemoryContext() {
        return userMemoryContext;
    }

    public AgentContext setUserMemoryContext(UserMemoryContext userMemoryContext) {
        this.userMemoryContext = userMemoryContext;
        return this;
    }

}
