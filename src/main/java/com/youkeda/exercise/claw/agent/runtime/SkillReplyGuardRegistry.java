package com.youkeda.exercise.claw.agent.runtime;

import com.youkeda.exercise.claw.agent.skill.SkillSession;
import com.youkeda.exercise.claw.agent.model.ResultStatus;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 按当前 Skill 分发文本回复结束条件校验。 */
@Component
public class SkillReplyGuardRegistry {

    private final Map<String, SkillReplyGuard> guards = new LinkedHashMap<>();

    public SkillReplyGuardRegistry(List<SkillReplyGuard> guards) {
        for (SkillReplyGuard guard : guards) {
            SkillReplyGuard previous = this.guards.putIfAbsent(
                    guard.getSkillName(), guard);
            if (previous != null) {
                throw new IllegalStateException(
                        "重复的 SkillReplyGuard: " + guard.getSkillName());
            }
        }
    }

    public SkillReplyGuard.GuardResult validate(
            String activeSkillName,
            String userMessage,
            String reply,
            SkillSession session,
            Set<String> executedCalls) {
        return validate(
                activeSkillName, userMessage, reply, session,
                executedCalls, Map.of());
    }

    public SkillReplyGuard.GuardResult validate(
            String activeSkillName,
            String userMessage,
            String reply,
            SkillSession session,
            Set<String> executedCalls,
            Map<String, ResultStatus> toolStatuses) {
        SkillReplyGuard guard = guards.get(activeSkillName);
        if (guard == null) {
            return SkillReplyGuard.GuardResult.allow();
        }
        return guard.validate(new SkillReplyGuard.GuardContext(
                userMessage, reply, session, Set.copyOf(executedCalls),
                Map.copyOf(toolStatuses)));
    }
}
