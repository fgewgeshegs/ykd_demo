package com.youkeda.exercise.claw.agent.skill;

import java.time.Instant;

public record SkillSession(String userId, String activeSkill, String previousSkill, Instant activatedAt, Instant lastActivityAt, int inactivityCount) {
}
