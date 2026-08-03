package com.youkeda.exercise.claw.agent.skill;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 将 Skill 路由事件分发给领域生命周期处理器。 */
@Component
public class SkillLifecycleCoordinator {

    private final Map<String, SkillLifecycleHandler> handlers = new LinkedHashMap<>();

    public SkillLifecycleCoordinator(List<SkillLifecycleHandler> handlers) {
        for (SkillLifecycleHandler handler : handlers) {
            SkillLifecycleHandler previous = this.handlers.putIfAbsent(
                    handler.getSkillName(), handler);
            if (previous != null) {
                throw new IllegalStateException(
                        "重复的 SkillLifecycleHandler: " + handler.getSkillName());
            }
        }
    }

    public SkillSession onRouting(
            String userMessage,
            SkillRoutingResult routingResult,
            SkillSession session) {
        if (routingResult == null || routingResult.primarySkill() == null) {
            return session;
        }
        SkillLifecycleHandler handler = handlers.get(routingResult.primarySkill());
        if (handler == null) return session;
        SkillSession updated = handler.onRouting(
                userMessage, routingResult, session);
        return updated != null ? updated : session;
    }

    public SkillSession onRouting(
            String userMessage,
            SkillRoutingResult routing,
            SkillSession session,
            String requestId) {
        if (routing == null || routing.primarySkill() == null) return session;
        SkillLifecycleHandler handler = handlers.get(routing.primarySkill());
        if (handler == null) return session;
        SkillSession updated = handler.onRouting(
                userMessage, routing, session, requestId);
        return updated != null ? updated : session;
    }
}
