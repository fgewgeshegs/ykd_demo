package com.youkeda.exercise.claw.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 通用工具调用安全检查（与业务无关）。
 *
 * <p>负责 CanExecute 阶段的检查——调用是否合法、参数是否合理。
 * 不涉及业务领域判断（阶段、顺序等）。
 */
@Component
public class SafetyPolicy {

    private static final Logger log = LoggerFactory.getLogger(SafetyPolicy.class);

    /**
     * 不可被 LLM 调用的工具黑名单。
     */
    private static final Set<String> BLACKLISTED_TOOLS = Set.of();

    /**
     * @return null 表示允许；非空字符串表示阻止原因
     */
    public String canExecute(String toolName, String argumentsJson) {
        if (toolName == null || toolName.isBlank()) {
            return "工具名称为空";
        }
        if (BLACKLISTED_TOOLS.contains(toolName)) {
            return "工具 " + toolName + " 被安全策略禁止调用";
        }
        // 用量限制、参数校验等可以在此扩展
        return null;
    }
}
