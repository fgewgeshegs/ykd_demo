package com.youkeda.exercise.claw.profile.service;

import com.youkeda.exercise.claw.profile.model.UserProfile;
import com.youkeda.exercise.claw.profile.repository.ProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户画像业务服务
 *
 * <p>封装 {@link ProfileRepository} 的 CRUD 操作。
 * 提供保存、查询、构建画像上下文的业务语义。
 */
@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private final ProfileRepository repository;

    public ProfileService(ProfileRepository repository) {
        this.repository = repository;
    }

    /**
     * 保存画像条目（同 type+key 覆盖更新）
     */
    public void saveProfile(String userId, String profileType, String profileKey, String profileValue) {
        log.info("保存用户画像 | userId={} | type={} | key={} | value={}",
                userId, profileType, profileKey, profileValue);
        UserProfile profile = new UserProfile(userId, profileType, profileKey, profileValue);
        repository.saveProfile(profile);
    }

    /**
     * 查询用户全部画像
     */
    public List<UserProfile> getProfiles(String userId) {
        return repository.findProfiles(userId);
    }

    /**
     * 按类型查询画像
     */
    public List<UserProfile> getProfilesByType(String userId, String profileType) {
        return repository.findProfilesByType(userId, profileType);
    }

    /**
     * 删除画像
     */
    public boolean deleteProfile(Long id, String userId) {
        log.info("删除用户画像 | id={} | userId={}", id, userId);
        return repository.deleteProfile(id, userId);
    }

    /**
     * 构建用户画像上下文字符串，供 Agent 注入
     *
     * <p>按 profile_type 分组输出，格式：
     * 【用户画像】
     *
     * 身份:
     * 学生开发者
     *
     * 技能:
     * Java、Spring Boot、AI Agent
     *
     * 目标:
     * 参加比赛
     */
    public String buildProfileContext(String userId) {
        List<UserProfile> profiles = repository.findProfiles(userId);
        if (profiles.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【用户画像】\n");

        String currentType = null;
        boolean firstGroup = true;
        for (UserProfile p : profiles) {
            String type = p.getProfileTypeDisplay();
            if (!type.equals(currentType)) {
                if (!firstGroup) {
                    sb.append("\n");
                }
                currentType = type;
                sb.append("\n").append(type).append(":\n");
                firstGroup = false;
            }
            sb.append(p.getProfileValue()).append("\n");
        }

        return sb.toString();
    }
}