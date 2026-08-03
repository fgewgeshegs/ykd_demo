package com.youkeda.exercise.claw.agent;

import com.youkeda.exercise.claw.agent.model.PlanState;
import com.youkeda.exercise.claw.agent.skill.SkillSession;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.model.MessageType;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.model.WechatMessage;

/**
 * Agent 执行上下文
 *
 * 封装一次 Agent 调用的所有输入信息，贯穿整个执行链路。
 * {@link #planState} 为可选字段——非多步骤任务时为 null，0 额外开销。
 */
public class AgentContext {

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 上下文 Token
     */
    private String contextToken;

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
     * 当前 Skill 会话状态（可选，非 skill 任务时为 null）
     */
    private SkillSession skillSession;

    /**
     * 轮次 ID（ADR Phase 1B）：用户消息已在落库时 beginTurn，roundId 随消息贯通至此；
     * 系统触发（定时任务）为 null，由 executor 自行 beginTurn。
     */
    private String roundId;


    public String getUserId() {
        return userId;
    }

    public AgentContext setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getContextToken() {
        return contextToken;
    }

    public AgentContext setContextToken(String contextToken) {
        this.contextToken = contextToken;
        return this;
    }

    public SkillSession getSkillSession() {
        return skillSession;
    }

    public AgentContext setSkillSession(SkillSession skillSession) {
        this.skillSession = skillSession;
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

    public String getRoundId() {
        return roundId;
    }

    public AgentContext setRoundId(String roundId) {
        this.roundId = roundId;
        return this;
    }

}
