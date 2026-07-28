package com.youkeda.exercise.claw.agent.tool;

/**
 * LLM 函数执行上下文。
 *
 * @param currentMessage 当前用户消息
 */
public record FunctionExecutionContext(String currentMessage) {
}
