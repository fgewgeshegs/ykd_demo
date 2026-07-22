package com.youkeda.exercise.claw.ai.llm;

import java.util.Collections;
import java.util.List;

/**
 * LLM 调用返回结果
 *
 * <p>可能是文本回复（{@code content != null}），也可能是工具调用（{@link #isToolCall()} 为 true）。
 * 根据 {@code finishReason} 区分：
 * <ul>
 *   <li>{@code "stop"} — 文本回复，读取 {@link #getContent()}</li>
 *   <li>{@code "tool_calls"} — LLM 要求调用工具，读取 {@link #getToolCalls()}</li>
 * </ul>
 *
 * <p>{@link #getReasoningContent()} 携带 DeepSeek 深度思考模式的中间推理过程，
 * 需要在后续请求中原样回传（见 {@code serializeMessage()}）。</p>
 */
public class LLMResponse {

    private final String content;
    private final List<ToolCall> toolCalls;
    private final String finishReason;
    private final String reasoningContent;

    public LLMResponse(String content, List<ToolCall> toolCalls, String finishReason) {
        this(content, toolCalls, finishReason, null);
    }

    public LLMResponse(String content, List<ToolCall> toolCalls, String finishReason,
                        String reasoningContent) {
        this.content = content;
        this.toolCalls = toolCalls != null ? toolCalls : List.of();
        this.finishReason = finishReason;
        this.reasoningContent = reasoningContent;
    }

    /**
     * LLM 是否要求调用工具
     */
    public boolean isToolCall() {
        return "tool_calls".equals(finishReason) && !toolCalls.isEmpty();
    }

    // ==================== Getters ====================

    public String getContent() {
        return content;
    }

    public List<ToolCall> getToolCalls() {
        return Collections.unmodifiableList(toolCalls);
    }

    public String getFinishReason() {
        return finishReason;
    }

    /**
     * DeepSeek 深度思考模式的中间推理过程
     * <p>此字段需要原样保存并在后续请求中回传给 API，否则 DeepSeek 会拒绝请求。</p>
     */
    public String getReasoningContent() {
        return reasoningContent;
    }

    // ==================== ToolCall ====================

    /**
     * 一次工具调用请求
     *
     * @param id        工具调用 ID，用于关联 tool result
     * @param type      调用类型，固定为 "function"
     * @param name      函数名
     * @param arguments 参数字符串（JSON 格式）
     */
    public record ToolCall(String id, String type, String name, String arguments) {
        public ToolCall(String id, String name, String arguments) {
            this(id, "function", name, arguments);
        }
    }
}
