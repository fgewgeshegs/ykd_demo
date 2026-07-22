package com.youkeda.exercise.claw.agent.tool;

import com.youkeda.exercise.claw.agent.classify.Intent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 工具注册中心
 *
 * 职责：管理所有 Tool 的注册、查找与列表。
 * 当前作为 Spring Bean 使用，未来 Planner 通过它查找可用工具。
 *
 * 线程安全：使用 synchronized 保证并发注册/查找安全。
 */
@Component
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    /**
     * Intent → Tool 映射表（一个 Intent 对应一个 Tool）
     */
    private final Map<Intent, Tool> toolMap = new EnumMap<>(Intent.class);

    /**
     * 所有已注册的工具列表
     */
    private final List<Tool> tools = new ArrayList<>();

    /**
     * 注册工具
     *
     * @param tool 工具实例
     */
    public synchronized void register(Tool tool) {
        tools.add(tool);
        for (Intent intent : tool.supportedIntents()) {
            Tool existing = toolMap.put(intent, tool);
            if (existing != null) {
                log.warn("意图 {} 的原工具 {} 被新工具 {} 覆盖", intent, existing.name(), tool.name());
            }
        }
        log.info("工具已注册: name={}, intents={}", tool.name(), tool.supportedIntents());
    }

    /**
     * 根据意图查找工具
     *
     * @param intent 用户意图
     * @return 匹配的工具，未找到返回 null
     */
    public synchronized Tool findTool(Intent intent) {
        return toolMap.get(intent);
    }

    /**
     * 获取所有已注册工具
     *
     * @return 工具列表（不可变副本）
     */
    public synchronized List<Tool> getTools() {
        return List.copyOf(tools);
    }
}
