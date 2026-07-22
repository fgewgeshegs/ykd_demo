package com.youkeda.exercise.claw.ai.llm;

/**
 * 大模型返回的工具调用请求
 *
 * @param id           调用 ID（用于第二轮请求中关联 tool 结果）
 * @param functionName 工具名称，如 "get_weather"
 * @param arguments    参数 JSON 字符串，如 "{\"city\":\"上海\"}"
 */
public record ToolCall(String id, String functionName, String arguments) {
}
