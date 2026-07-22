package com.youkeda.exercise.claw.ai.llm;

/**
 * 工具执行结果
 *
 * @param toolCallId 对应的 ToolCall.id
 * @param content    工具返回的 JSON 结果字符串
 */
public record ToolResult(String toolCallId, String content) {
}
