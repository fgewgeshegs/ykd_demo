package com.youkeda.exercise.claw.agent.tool;

import com.youkeda.exercise.claw.agent.AgentContext;
import com.youkeda.exercise.claw.agent.classify.Intent;

/**
 * 工具接口（旧 Agent 路由体系）
 *
 * Agent 体系中每个可调用的能力都是一个 Tool。
 * 与 {@link LLMFunction} 接口并存：
 * <ul>
 *   <li>{@link Tool} — 非 TEXT 消息类型（IMAGE/VOICE/FILE）通过 ToolRegistry 按 Intent 路由</li>
 *   <li>{@link LLMFunction} — TEXT 消息由 LLM 通过 Function Calling 自主调用</li>
 * </ul>
 * 一个实现类可以同时实现两个接口，注册到各自的 Registry。
 */
public interface Tool {

    /**
     * 工具名称（唯一标识）
     *
     * @return 工具名，如 "chat"、"vision"、"image_generate"
     */
    String name();

    /**
     * 工具描述（供 Planner 决策使用）
     *
     * @return 工具功能说明
     */
    String description();

    /**
     * 该工具支持的意图列表
     *
     * @return 支持的 Intent 数组
     */
    Intent[] supportedIntents();

    /**
     * 执行工具
     *
     * @param context Agent 执行上下文
     * @return 执行结果文本
     */
    String execute(AgentContext context);
}
