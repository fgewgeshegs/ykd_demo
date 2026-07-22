package com.youkeda.exercise.claw.ai.llm;

import java.util.List;

/**
 * LLM 响应统一模型
 *
 * 大模型返回两种可能：
 * 1. 直接文本回复（不需要调工具）
 * 2. 工具调用请求（需要先调工具再发第二轮）
 */
public class LLMResponse {

    private final Type type;
    private final String text;
    private final List<ToolCall> toolCalls;

    private LLMResponse(Type type, String text, List<ToolCall> toolCalls) {
        this.type = type;
        this.text = text;
        this.toolCalls = toolCalls;
    }

    public static LLMResponse text(String text) {
        return new LLMResponse(Type.TEXT, text, null);
    }

    public static LLMResponse toolCalls(List<ToolCall> toolCalls) {
        return new LLMResponse(Type.TOOL_CALLS, null, toolCalls);
    }

    public boolean isText() { return type == Type.TEXT; }
    public boolean isToolCalls() { return type == Type.TOOL_CALLS; }

    public String getText() { return text; }
    public List<ToolCall> getToolCalls() { return toolCalls; }

    public enum Type { TEXT, TOOL_CALLS }
}
