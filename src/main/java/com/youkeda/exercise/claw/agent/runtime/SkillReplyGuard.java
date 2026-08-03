package com.youkeda.exercise.claw.agent.runtime;

import com.youkeda.exercise.claw.agent.skill.SkillSession;
import com.youkeda.exercise.claw.agent.model.ResultStatus;

import java.util.Map;
import java.util.Set;

/** Skill 对 LLM 文本结束条件的确定性校验扩展点。 */
public interface SkillReplyGuard {

    String getSkillName();

    GuardResult validate(GuardContext context);

    record GuardContext(
            String userMessage,
            String reply,
            SkillSession session,
            Set<String> executedCalls,
            Map<String, ResultStatus> toolStatuses
    ) {
        public GuardContext(
                String userMessage,
                String reply,
                SkillSession session,
                Set<String> executedCalls) {
            this(userMessage, reply, session, executedCalls,
                    inferSuccessfulStatuses(executedCalls));
        }

        private static Map<String, ResultStatus> inferSuccessfulStatuses(
                Set<String> executedCalls) {
            if (executedCalls == null || executedCalls.isEmpty()) return Map.of();
            java.util.HashMap<String, ResultStatus> statuses = new java.util.HashMap<>();
            for (String call : executedCalls) {
                int separator = call.indexOf('|');
                String toolName = separator >= 0 ? call.substring(0, separator) : call;
                if (!toolName.isBlank()) statuses.put(toolName, ResultStatus.SUCCESS);
            }
            return Map.copyOf(statuses);
        }
    }

    record GuardResult(boolean allowed, String correction) {
        public static GuardResult allow() {
            return new GuardResult(true, null);
        }

        public static GuardResult reject(String correction) {
            return new GuardResult(false, correction);
        }
    }
}
