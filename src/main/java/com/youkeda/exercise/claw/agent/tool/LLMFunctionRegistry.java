package com.youkeda.exercise.claw.agent.tool;

import com.youkeda.exercise.claw.ai.llm.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * LLM 函数注册中心
 *
 * <p>管理所有可供 LLM 通过 Function Calling 调用的 {@link LLMFunction}。
 * 与现有 {@link ToolRegistry}（Intent 路由体系）并存。
 *
 * <p>线程安全：使用 {@link ConcurrentHashMap}。
 */
@Component
public class LLMFunctionRegistry {

    private static final Logger log = LoggerFactory.getLogger(LLMFunctionRegistry.class);

    private final Map<String, LLMFunction> functions = new ConcurrentHashMap<>();

    /**
     * 注册函数
     *
     * @param function 函数实例
     */
    public void register(LLMFunction function) {
        functions.put(function.getName(), function);
        log.info("LLM函数已注册: name={}, description={}", function.getName(), function.getDescription());
    }

    /**
     * 根据名称查找函数
     *
     * @param name 函数名
     * @return 匹配的函数，未找到返回 null
     */
    public LLMFunction find(String name) {
        return functions.get(name);
    }

    /**
     * 获取所有已注册的工具定义（用于发给 LLM 的 tools 参数）
     *
     * @return 工具定义列表
     */
    public List<ToolDefinition> getAllDefinitions() {
        return functions.values().stream()
                .map(LLMFunction::toDefinition)
                .collect(Collectors.toList());
    }
}
