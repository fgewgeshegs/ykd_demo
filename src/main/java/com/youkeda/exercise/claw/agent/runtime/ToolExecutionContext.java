package com.youkeda.exercise.claw.agent.runtime;

import com.youkeda.exercise.claw.agent.skill.SkillSession;

/**
 * 工具执行上下文。
 *
 * @param currentMessage 当前用户消息
 * @param skillSession 当前 Skill 会话，可为空
 * @param userId 当前用户标识，可为空
 */
public record ToolExecutionContext(String currentMessage, SkillSession skillSession, String userId) {

    /**
     * 空上下文。无 {@link ToolExecutor} 场景（如直接单参数调用工具）时的占位，永不为 null。
     */
    public static final ToolExecutionContext EMPTY = new ToolExecutionContext("");

    public ToolExecutionContext(String currentMessage) {
        this(currentMessage, null, "");
    }

    public ToolExecutionContext(String currentMessage, SkillSession skillSession) {
        this(currentMessage, skillSession, "");
    }
}
