package com.youkeda.exercise.claw.agent.tool;

import com.youkeda.exercise.claw.agent.AgentContext;
import com.youkeda.exercise.claw.agent.classify.Intent;

/**
 * 工具接口
 *
 * Agent 体系中每个可调用的能力都是一个 Tool。
 * 未来 Planner 将通过 ToolRegistry 查找合适的 Tool 并调用。
 *
 * 当前实现：
 * - ChatTool（封装 ChatService）
 * - VisionTool（封装 VisionService）
 * - ImageGenerationTool（封装 ImageGenerationService）
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
