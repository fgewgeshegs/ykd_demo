package com.youkeda.exercise.claw.profile.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 用户画像条目（profile_summary）
 *
 * <p>在 Memory（事实）之上总结的用户特征。
 * 由 LLM 通过 {@code update_profile} Function 主动总结并保存。
 *
 * <p>Profile 与 Memory 的区别：
 * <ul>
 *   <li>Memory：保存具体事实（"喜欢喝冰美式"、"使用 Java"）</li>
 *   <li>Profile：总结用户特征（"咖啡爱好者"、"Java 开发者"、"AI Agent 方向"）</li>
 * </ul>
 *
 * <p>Profile 类型：
 * <ul>
 *   <li>{@link #PROFILE_TYPE_IDENTITY} — 身份（学生/开发者/设计师）</li>
 *   <li>{@link #PROFILE_TYPE_SKILL} — 技能（Java/AI/产品设计）</li>
 *   <li>{@link #PROFILE_TYPE_GOAL} — 目标（参赛/求职/学习）</li>
 *   <li>{@link #PROFILE_TYPE_INTEREST} — 兴趣方向（AI/开源/创业）</li>
 * </ul>
 */
public class UserProfile {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== Profile 类型常量 ====================

    /** 用户身份 */
    public static final String PROFILE_TYPE_IDENTITY = "IDENTITY";
    /** 技能方向 */
    public static final String PROFILE_TYPE_SKILL = "SKILL";
    /** 当前目标 */
    public static final String PROFILE_TYPE_GOAL = "GOAL";
    /** 兴趣方向 */
    public static final String PROFILE_TYPE_INTEREST = "INTEREST";

    /** 所有 Profile 类型集合 */
    public static final java.util.Set<String> PROFILE_TYPES = java.util.Set.of(
            PROFILE_TYPE_IDENTITY, PROFILE_TYPE_SKILL, PROFILE_TYPE_GOAL, PROFILE_TYPE_INTEREST
    );

    private Long id;
    private String userId;
    private String profileType;
    private String profileKey;
    private String profileValue;
    private LocalDateTime updatedTime;

    public UserProfile() {
    }

    public UserProfile(String userId, String profileType, String profileKey, String profileValue) {
        this.userId = userId;
        this.profileType = profileType;
        this.profileKey = profileKey;
        this.profileValue = profileValue;
        this.updatedTime = LocalDateTime.now();
    }

    // ==================== Getters & Setters ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getProfileType() { return profileType; }
    public void setProfileType(String profileType) { this.profileType = profileType; }

    public String getProfileKey() { return profileKey; }
    public void setProfileKey(String profileKey) { this.profileKey = profileKey; }

    public String getProfileValue() { return profileValue; }
    public void setProfileValue(String profileValue) { this.profileValue = profileValue; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }

    // ==================== 序列化辅助 ====================

    public String getUpdatedTimeAsString() {
        return updatedTime != null ? updatedTime.format(DTF) : null;
    }

    public void setUpdatedTimeFromString(String str) {
        this.updatedTime = (str != null && !str.isEmpty())
                ? LocalDateTime.parse(str, DTF) : null;
    }

    // ==================== 展示辅助 ====================

    public static String profileTypeDisplay(String type) {
        if (type == null) return "";
        return switch (type) {
            case PROFILE_TYPE_IDENTITY -> "身份";
            case PROFILE_TYPE_SKILL -> "技能";
            case PROFILE_TYPE_GOAL -> "目标";
            case PROFILE_TYPE_INTEREST -> "兴趣";
            default -> type;
        };
    }

    public String getProfileTypeDisplay() {
        return profileTypeDisplay(profileType);
    }
}