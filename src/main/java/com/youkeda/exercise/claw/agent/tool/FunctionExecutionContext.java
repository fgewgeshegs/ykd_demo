package com.youkeda.exercise.claw.agent.tool;

import com.youkeda.exercise.claw.agent.skill.SkillSession;

/**
 * LLM 函数执行上下文。
 *
 * @param currentMessage 当前用户消息
 * @param skillSession 当前 Skill 会话，可为空
 */
public record FunctionExecutionContext(String currentMessage, SkillSession skillSession) {

    public FunctionExecutionContext(String currentMessage) {
        this(currentMessage, null);
    }
}
