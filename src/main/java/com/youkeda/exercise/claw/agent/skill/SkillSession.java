package com.youkeda.exercise.claw.agent.skill;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public record SkillSession(
        String userId,
        String activeSkill,
        String previousSkill,
        Instant activatedAt,
        Instant lastActivityAt,
        int inactivityCount,
        Map<String, String> context
) {

    private static final String PENDING_ACTION = "pendingAction";
    private static final String PENDING_SLOT = "pendingSlot";
    private static final String SUSPENDED_PREFIX = "suspended.";

    public SkillSession(String userId, String activeSkill, String previousSkill,
                        Instant activatedAt, Instant lastActivityAt, int inactivityCount) {
        this(userId, activeSkill, previousSkill, activatedAt, lastActivityAt,
                inactivityCount, Map.of());
    }

    public SkillSession {
        context = context == null ? Map.of() : Map.copyOf(context);
    }

    public static SkillSession create(String userId) {
        Instant now = Instant.now();
        return new SkillSession(userId, "common", null, now, now, 0, Map.of());
    }

    public SkillSession withActiveSkill(String newSkill) {
        boolean sameSkill = newSkill.equals(this.activeSkill);
        Map<String, String> updatedContext = sameSkill
                ? this.context
                : switchContext(this.activeSkill, newSkill);
        return new SkillSession(
                this.userId,
                newSkill,
                sameSkill ? this.previousSkill : this.activeSkill,
                this.activatedAt,
                Instant.now(),
                0,
                updatedContext
        );
    }

    private Map<String, String> switchContext(String oldSkill, String newSkill) {
        Map<String, String> updated = new HashMap<>();
        context.forEach((key, value) -> {
            if (key.startsWith(SUSPENDED_PREFIX)) {
                updated.put(key, value);
            } else {
                updated.put(suspendedKey(oldSkill, key), value);
            }
        });
        String restorePrefix = SUSPENDED_PREFIX + newSkill + ".";
        Map<String, String> snapshot = new HashMap<>(updated);
        snapshot.forEach((key, value) -> {
            if (key.startsWith(restorePrefix)) {
                updated.remove(key);
                updated.put(key.substring(restorePrefix.length()), value);
            }
        });
        return updated;
    }

    private static String suspendedKey(String skill, String key) {
        return SUSPENDED_PREFIX + skill + "." + key;
    }

    public SkillSession withIncrementInactivity() {
        return new SkillSession(
                this.userId,
                this.activeSkill,
                this.previousSkill,
                this.activatedAt,
                this.lastActivityAt,
                this.inactivityCount + 1,
                this.context
        );
    }

    public SkillSession withResetInactivity() {
        return new SkillSession(
                this.userId,
                this.activeSkill,
                this.previousSkill,
                this.activatedAt,
                Instant.now(),
                0,
                this.context
        );
    }

    public SkillSession withPendingAction(String action, String slot) {
        Map<String, String> updated = new HashMap<>(context);
        updated.put(PENDING_ACTION, action);
        if (slot == null || slot.isBlank()) {
            updated.remove(PENDING_SLOT);
        } else {
            updated.put(PENDING_SLOT, slot);
        }
        return new SkillSession(userId, activeSkill, previousSkill, activatedAt,
                Instant.now(), inactivityCount, updated);
    }

    public boolean hasPendingAction(String action) {
        return action != null && action.equals(context.get(PENDING_ACTION));
    }

    public boolean hasSuspendedPendingAction(String skill, String action) {
        return action != null && action.equals(
                context.get(suspendedKey(skill, PENDING_ACTION)));
    }

    public String suspendedPendingSlot(String skill) {
        return context.get(suspendedKey(skill, PENDING_SLOT));
    }

    public String pendingSlot() {
        return context.get(PENDING_SLOT);
    }

    public SkillSession clearPendingAction() {
        if (!context.containsKey(PENDING_ACTION) && !context.containsKey(PENDING_SLOT)) {
            return this;
        }
        Map<String, String> updated = new HashMap<>(context);
        updated.remove(PENDING_ACTION);
        updated.remove(PENDING_SLOT);
        return new SkillSession(userId, activeSkill, previousSkill, activatedAt,
                Instant.now(), inactivityCount, updated);
    }

    public SkillSession withContextValue(String key, String value) {
        Map<String, String> updated = new HashMap<>(context);
        updated.put(key, value);
        return new SkillSession(userId, activeSkill, previousSkill, activatedAt,
                Instant.now(), inactivityCount, updated);
    }

    public SkillSession withoutContextValue(String key) {
        if (!context.containsKey(key)) return this;
        Map<String, String> updated = new HashMap<>(context);
        updated.remove(key);
        return new SkillSession(userId, activeSkill, previousSkill, activatedAt,
                Instant.now(), inactivityCount, updated);
    }
}
