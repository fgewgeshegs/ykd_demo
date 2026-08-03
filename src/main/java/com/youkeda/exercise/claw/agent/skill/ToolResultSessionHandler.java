package com.youkeda.exercise.claw.agent.skill;

/**
 * Extends Skill session state from a completed tool result.
 */
public interface ToolResultSessionHandler {

    String skillName();

    String toolName();

    default boolean supports(String activeSkill, String executedToolName) {
        return skillName().equals(activeSkill) && toolName().equals(executedToolName);
    }

    SkillSession afterExecution(
            SkillSession session,
            String toolName,
            String toolResult);
}
