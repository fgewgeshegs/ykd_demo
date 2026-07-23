package com.youkeda.exercise.claw.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.youkeda.exercise.claw.ai.llm.ToolDefinition;

/**
 * 可供 LLM 通过 Function Calling 调用的函数
 *
 * <p>Agent 体系的唯一工具接口。所有的工具能力（天气、搜索、时间、地图、预算等）
 * 都以 {@code LLMFunction} 注册到 {@link LLMFunctionRegistry}，
 * 由 {@code ReActAgentExecutor} 在 tool-calling 循环中供 LLM 自主调度。
 *
 * <p>如需将文件/图片/音频等二进制数据传回消息收发层，
 * 实现类可配合暂存-消费模式（如 {@code pendingImage}）进行旁路传递。
 */
public interface LLMFunction {

    /**
     * 函数名（LLM 通过这个名字调用）
     */
    String getName();

    /**
     * 函数描述（LLM 判断何时调用）
     */
    String getDescription();

    /**
     * 参数 JSON Schema
     * <p>返回的 JsonNode 应为 {@code {type: "object", properties: {...}, required: [...]}} 格式。
     */
    JsonNode getParameters();

    /**
     * 执行函数
     *
     * @param argumentsJson LLM 生成的参数字符串（JSON 格式）
     * @return 执行结果字符串（LLM 将拿到此内容组织回答）
     */
    String execute(String argumentsJson);

    /**
     * 带用户上下文执行函数。旧工具默认沿用单参数实现，需要会话状态的新工具可覆盖此方法。
     */
    default String execute(String argumentsJson, FunctionExecutionContext context) {
        return execute(argumentsJson);
    }

    /**
     * 快捷方法：生成发给 LLM 的 {@link ToolDefinition}
     */
    default ToolDefinition toDefinition() {
        return new ToolDefinition(getName(), getDescription(), getParameters());
    }
}
