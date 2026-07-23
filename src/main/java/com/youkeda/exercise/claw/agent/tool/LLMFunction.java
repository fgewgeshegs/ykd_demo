package com.youkeda.exercise.claw.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.youkeda.exercise.claw.ai.llm.ToolDefinition;

/**
 * 可供 LLM 通过 Function Calling 调用的函数
 *
 * <p>与现有 {@link Tool} 接口并存互不冲突：
 * <ul>
 *   <li>{@link Tool} — Agent 路由体系（Intent → Tool 映射）</li>
 *   <li>{@code LLMFunction} — LLM 自主 tool-calling 循环</li>
 * </ul>
 *
 * 一个实现类可以同时实现两个接口，注册到各自的 Registry。
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
