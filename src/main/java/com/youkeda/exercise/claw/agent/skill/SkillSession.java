package com.youkeda.exercise.claw.agent.skill;

import java.time.Instant;

public record SkillSession(String userId, String activeSkill, String previousSkill, Instant activatedAt, Instant lastActivityAt, int inactivityCount) {

    public static SkillSession create(String userId) {
        Instant now = Instant.now();
        return new SkillSession(userId, "common", null, now, now, 0);
    }

    public SkillSession withActiveSkill(String newSkill) {
        return new SkillSession(
                this.userId,
                newSkill,
                newSkill.equals(this.activeSkill) ? this.previousSkill : this.activeSkill,
                this.activatedAt,
                Instant.now(),
                0
        );
    }

    public SkillSession withIncrementInactivity() {
        return new SkillSession(
                this.userId,
                this.activeSkill,
                this.previousSkill,
                this.activatedAt,
                this.lastActivityAt,
                this.inactivityCount + 1
        );
    }

    public SkillSession withResetInactivity() {
        return new SkillSession(
                this.userId,
                this.activeSkill,
                this.previousSkill,
                this.activatedAt,
                Instant.now(),
                0
        );
    }
}
