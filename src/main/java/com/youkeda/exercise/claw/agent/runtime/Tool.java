package com.youkeda.exercise.claw.agent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.youkeda.exercise.claw.ai.llm.ToolDefinition;

/**
 * Agent 工具接口（Tool 合约）。
 *
 * <p>所有工具实现（天气、搜索、时间、地图、预算等）以 {@code Tool}
 * 注册到 {@link ToolRegistry}，由 {@code AgentRuntime} 在 tool-calling
 * 循环中供 LLM 自主调度。
 *
 * <p>如需将文件/图片/音频等二进制数据传回消息收发层，
 * 实现类可配合暂存-消费模式进行旁路传递。
 */
public interface Tool {

    /**
     * 工具名（LLM 通过这个名字调用）
     */
    String getName();

    /**
     * 工具描述（LLM 判断何时调用）
     */
    String getDescription();

    /**
     * 参数 JSON Schema
     * <p>返回的 JsonNode 应为 {@code {type: "object", properties: {...}, required: [...]}} 格式。
     */
    JsonNode getParameters();

    /**
     * 执行工具
     *
     * @param argumentsJson LLM 生成的参数字符串（JSON 格式）
     * @return 执行结果字符串（LLM 将拿到此内容组织回答）
     */
    String execute(String argumentsJson);

    /**
     * 带用户上下文执行工具。旧工具默认沿用单参数实现，需要会话状态的新工具可覆盖此方法。
     */
    default String execute(String argumentsJson, ToolExecutionContext context) {
        return execute(argumentsJson);
    }

    /**
     * 判断当前用户消息是否允许暴露并执行该工具。
     *
     * <p>默认所有消息均可使用。具有严格触发条件的工具应覆盖此方法；
     * 执行器会同时在工具定义暴露阶段和实际执行阶段进行校验。
     */
    default boolean isAvailable(ToolExecutionContext context) {
        return true;
    }

    /**
     * 当前消息不允许使用该工具时返回给模型的原因。
     */
    default String getUnavailableReason(ToolExecutionContext context) {
        return "当前用户消息未明确请求使用该工具。";
    }

    /**
     * 快捷方法：生成发给 LLM 的 {@link ToolDefinition}
     */
    default ToolDefinition toDefinition() {
        return new ToolDefinition(getName(), getDescription(), getParameters());
    }
}
