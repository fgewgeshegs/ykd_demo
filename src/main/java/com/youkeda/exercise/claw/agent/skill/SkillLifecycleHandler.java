package com.youkeda.exercise.claw.agent.skill;

/** Skill 激活时的领域生命周期扩展点。 */
public interface SkillLifecycleHandler {

    String getSkillName();

    SkillSession onRouting(
            String userMessage,
            SkillRoutingResult routing,
            SkillSession session);

    default SkillSession onRouting(
            String userMessage,
            SkillRoutingResult routing,
            SkillSession session,
            String requestId) {
        return onRouting(userMessage, routing, session);
    }
}
