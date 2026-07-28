package com.youkeda.exercise.claw.profile.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.tool.FunctionExecutionContext;
import com.youkeda.exercise.claw.memory.model.UserMemoryContext;
import com.youkeda.exercise.claw.memory.retriever.MemoryRetrieverImpl;
import com.youkeda.exercise.claw.memory.service.MemoryService;
import com.youkeda.exercise.claw.memory.repository.MemoryRepository;
import com.youkeda.exercise.claw.profile.function.UpdateProfileFunction;
import com.youkeda.exercise.claw.profile.model.UserProfile;
import com.youkeda.exercise.claw.profile.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户画像（Profile Layer）单元测试
 *
 * <p>覆盖：
 * 1. 保存用户画像
 * 2. 查询画像
 * 3. 不同用户隔离
 * 4. Agent 执行加载画像（通过 MemoryRetrieverImpl）
 * 5. update_profile Function
 * 6. SQLite 持久化
 */
class ProfileTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String USER_A = "user_a";
    private static final String USER_B = "user_b";

    private ProfileRepository profileRepository;
    private ProfileService profileService;
    private UpdateProfileFunction updateFunction;

    // 用于 Agent 注入测试
    private MemoryRepository memoryRepository;
    private MemoryService memoryService;
    private MemoryRetrieverImpl retriever;

    @TempDir
    File tempDir;

    @BeforeEach
    void setUp() {
        // Profile 层
        profileRepository = new ProfileRepository();
        setField(profileRepository, "dbPath", new File(tempDir, "test-profiles.db").getAbsolutePath());
        profileRepository.init();
        profileService = new ProfileService(profileRepository);
        updateFunction = new UpdateProfileFunction(MAPPER, null, profileService);

        // Memory 层（用于 Agent 注入测试）
        memoryRepository = new MemoryRepository();
        setField(memoryRepository, "dbPath", new File(tempDir, "test-memory.db").getAbsolutePath());
        memoryRepository.init();
        memoryService = new MemoryService(memoryRepository);
        retriever = new MemoryRetrieverImpl(memoryService, profileService);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== 1. 保存用户画像 ====================

    @Test
    void shouldSaveUserProfile() {
        profileService.saveProfile(USER_A, UserProfile.PROFILE_TYPE_IDENTITY, "role", "学生开发者");

        List<UserProfile> profiles = profileService.getProfiles(USER_A);
        assertEquals(1, profiles.size(), "应保存 1 条画像");
        assertEquals("role", profiles.get(0).getProfileKey());
        assertEquals("学生开发者", profiles.get(0).getProfileValue());
        assertEquals(UserProfile.PROFILE_TYPE_IDENTITY, profiles.get(0).getProfileType());
    }

    @Test
    void shouldSaveMultipleProfileTypes() {
        profileService.saveProfile(USER_A, UserProfile.PROFILE_TYPE_IDENTITY, "role", "后端开发者");
        profileService.saveProfile(USER_A, UserProfile.PROFILE_TYPE_SKILL, "programming", "Java");
        profileService.saveProfile(USER_A, UserProfile.PROFILE_TYPE_SKILL, "ai", "AI Agent");
        profileService.saveProfile(USER_A, UserProfile.PROFILE_TYPE_GOAL, "contest", "参加比赛");

        List<UserProfile> profiles = profileService.getProfiles(USER_A);
        assertEquals(4, profiles.size(), "应保存 4 条画像");
    }

    // ==================== 2. 查询画像 ====================

    @Test
    void shouldQueryProfilesByType() {
        profileService.saveProfile(USER_A, UserProfile.PROFILE_TYPE_IDENTITY, "role", "开发者");
        profileService.saveProfile(USER_A, UserProfile.PROFILE_TYPE_SKILL, "programming", "Java");

        List<UserProfile> identities = profileService.getProfilesByType(USER_A, UserProfile.PROFILE_TYPE_IDENTITY);
        assertEquals(1, identities.size(), "应查到 1 条身份画像");
        assertEquals("开发者", identities.get(0).getProfileValue());

        List<UserProfile> skills = profileService.getProfilesByType(USER_A, UserProfile.PROFILE_TYPE_SKILL);
        assertEquals(1, skills.size());

        List<UserProfile> goals = profileService.getProfilesByType(USER_A, UserProfile.PROFILE_TYPE_GOAL);
        assertTrue(goals.isEmpty(), "没有目标画像应返回空列表");
    }

    @Test
    void shouldReturnEmptyListForUserWithoutProfiles() {
        List<UserProfile> profiles = profileService.getProfiles("unknown_user");
        assertTrue(profiles.isEmpty(), "没有画像的用户应返回空列表");
    }

    // ==================== 3. 不同用户隔离 ====================

    @Test
    void shouldNotSeeOtherUsersProfiles() {
        profileService.saveProfile(USER_A, UserProfile.PROFILE_TYPE_IDENTITY, "role", "Java开发者");
        profileService.saveProfile(USER_B, UserProfile.PROFILE_TYPE_IDENTITY, "role", "设计师");

        List<UserProfile> userAProfiles = profileService.getProfiles(USER_A);
        assertEquals(1, userAProfiles.size(), "用户 A 只能看到自己的画像");
        assertEquals("Java开发者", userAProfiles.get(0).getProfileValue());

        List<UserProfile> userBProfiles = profileService.getProfiles(USER_B);
        assertEquals(1, userBProfiles.size(), "用户 B 只能看到自己的画像");
        assertEquals("设计师", userBProfiles.get(0).getProfileValue());
    }

    // ==================== 4. Agent 执行加载画像 ====================

    @Test
    void shouldLoadProfileContextForAgent() {
        // 保存画像
        profileService.saveProfile(USER_A, UserProfile.PROFILE_TYPE_IDENTITY, "role", "Java开发者");
        profileService.saveProfile(USER_A, UserProfile.PROFILE_TYPE_SKILL, "technology", "Spring Boot AI Agent");

        // 通过 MemoryRetrieverImpl 加载（模拟 Agent 执行流程）
        UserMemoryContext context = retriever.retrieve(USER_A, "");

        assertTrue(context.hasContent(), "上下文应包含内容");
        assertEquals(2, context.getProfileSummaries().size(), "应包含 2 条画像");

        // 验证格式化输出包含画像内容
        String formatted = context.formatForPrompt();
        assertTrue(formatted.contains("用户画像"), "格式化的 prompt 应包含【用户画像】");
        assertTrue(formatted.contains("Java开发者"), "应包含画像内容");
    }

    @Test
    void shouldReturnEmptyContextForUserWithoutProfiles() {
        UserMemoryContext context = retriever.retrieve("new_user", "");
        assertNotNull(context);
        assertFalse(context.hasContent(), "无数据的用户应返回空上下文");
    }

    @Test
    void profileContextShouldFormatCorrectly() {
        profileService.saveProfile(USER_A, UserProfile.PROFILE_TYPE_IDENTITY, "role", "学生开发者");
        profileService.saveProfile(USER_A, UserProfile.PROFILE_TYPE_SKILL, "programming", "Java");
        profileService.saveProfile(USER_A, UserProfile.PROFILE_TYPE_SKILL, "ai", "AI Agent");

        UserMemoryContext context = retriever.retrieve(USER_A, "");
        String formatted = context.formatForPrompt();

        assertTrue(formatted.contains("用户画像"));
        assertTrue(formatted.contains("身份"));
        assertTrue(formatted.contains("技能"));
        assertTrue(formatted.contains("学生开发者"));
        assertTrue(formatted.contains("Java"));
        assertTrue(formatted.contains("AI Agent"));
    }

    // ==================== 5. update_profile Function ====================

    @Test
    void updateProfileFunctionShouldSaveProfile() {
        String arguments = """
                {
                    "profile_type": "IDENTITY",
                    "profile_key": "role",
                    "profile_value": "Java开发者"
                }
                """;

        String result = updateFunction.execute(arguments, new FunctionExecutionContext(USER_A, ""));

        assertTrue(result.contains("saved"), "应返回保存成功");
        assertTrue(result.contains("画像"), "应包含画像描述");

        List<UserProfile> profiles = profileService.getProfiles(USER_A);
        assertEquals(1, profiles.size());
        assertEquals("Java开发者", profiles.get(0).getProfileValue());
    }

    @Test
    void updateProfileFunctionShouldRejectInvalidType() {
        String arguments = """
                {
                    "profile_type": "INVALID",
                    "profile_key": "role",
                    "profile_value": "test"
                }
                """;

        String result = updateFunction.execute(arguments, new FunctionExecutionContext(USER_A, ""));
        assertTrue(result.contains("不支持的画像类型"), "应拒绝无效类型");

        assertTrue(profileService.getProfiles(USER_A).isEmpty(), "不应保存任何数据");
    }

    @Test
    void updateProfileFunctionShouldRejectMissingKey() {
        String arguments = """
                {
                    "profile_type": "IDENTITY",
                    "profile_value": "developer"
                }
                """;

        String result = updateFunction.execute(arguments, new FunctionExecutionContext(USER_A, ""));
        assertTrue(result.contains("缺少 profile_key"), "应提示缺少 profile_key");
    }

    @Test
    void updateProfileFunctionShouldRejectMissingUserId() {
        String arguments = """
                {
                    "profile_type": "IDENTITY",
                    "profile_key": "role",
                    "profile_value": "developer"
                }
                """;

        String result = updateFunction.execute(arguments, new FunctionExecutionContext("", ""));
        assertTrue(result.contains("缺少用户ID"), "应提示缺少用户ID");
    }

    @Test
    void updateProfileFunctionShouldSupportAllTypes() {
        // IDENTITY
        updateFunction.execute("""
                {"profile_type": "IDENTITY", "profile_key": "role", "profile_value": "开发者"}
                """, new FunctionExecutionContext(USER_A, ""));
        assertEquals(1, profileService.getProfiles(USER_A).size());

        // SKILL
        updateFunction.execute("""
                {"profile_type": "SKILL", "profile_key": "programming", "profile_value": "Java"}
                """, new FunctionExecutionContext(USER_A, ""));
        assertEquals(2, profileService.getProfiles(USER_A).size());

        // GOAL
        updateFunction.execute("""
                {"profile_type": "GOAL", "profile_key": "contest", "profile_value": "参加黑客松"}
                """, new FunctionExecutionContext(USER_A, ""));
        assertEquals(3, profileService.getProfiles(USER_A).size());

        // INTEREST
        updateFunction.execute("""
                {"profile_type": "INTEREST", "profile_key": "open_source", "profile_value": "开源项目"}
                """, new FunctionExecutionContext(USER_A, ""));
        assertEquals(4, profileService.getProfiles(USER_A).size());
    }

    @Test
    void updateProfileFunctionShouldOverwriteExistingKey() {
        // 第一次保存
        updateFunction.execute("""
                {"profile_type": "SKILL", "profile_key": "programming", "profile_value": "Java"}
                """, new FunctionExecutionContext(USER_A, ""));
        assertEquals(1, profileService.getProfiles(USER_A).size());

        // 同 type+key，覆盖值
        updateFunction.execute("""
                {"profile_type": "SKILL", "profile_key": "programming", "profile_value": "Java Spring Boot"}
                """, new FunctionExecutionContext(USER_A, ""));

        // 应该只有 1 条（覆盖而非追加）
        List<UserProfile> profiles = profileService.getProfiles(USER_A);
        assertEquals(1, profiles.size(), "相同 type+key 应覆盖而非追加");
        assertEquals("Java Spring Boot", profiles.get(0).getProfileValue());
    }

    // ==================== 6. SQLite 持久化 ====================

    @Test
    void profileShouldBePersistedInSqlite() {
        profileService.saveProfile(USER_A, UserProfile.PROFILE_TYPE_IDENTITY, "role", "Java开发者");

        // 新建 Repository 实例验证持久化
        ProfileRepository freshRepo = new ProfileRepository();
        setField(freshRepo, "dbPath", new File(tempDir, "test-profiles.db").getAbsolutePath());
        freshRepo.init();

        ProfileService freshService = new ProfileService(freshRepo);
        List<UserProfile> profiles = freshService.getProfiles(USER_A);

        assertEquals(1, profiles.size(), "新建实例应读到持久化的画像");
        assertEquals("Java开发者", profiles.get(0).getProfileValue());
    }

    @Test
    void deleteProfileShouldWork() {
        profileService.saveProfile(USER_A, UserProfile.PROFILE_TYPE_IDENTITY, "role", "开发者");
        assertEquals(1, profileService.getProfiles(USER_A).size());

        List<UserProfile> profiles = profileService.getProfiles(USER_A);
        Long id = profiles.get(0).getId();

        boolean deleted = profileService.deleteProfile(id, USER_A);
        assertTrue(deleted, "删除应成功");
        assertTrue(profileService.getProfiles(USER_A).isEmpty(), "删除后应无画像");
    }

    @Test
    void shouldNotDeleteOtherUsersProfile() {
        profileService.saveProfile(USER_A, UserProfile.PROFILE_TYPE_IDENTITY, "role", "开发者");
        List<UserProfile> profiles = profileService.getProfiles(USER_A);
        Long id = profiles.get(0).getId();

        boolean deleted = profileService.deleteProfile(id, USER_B);
        assertFalse(deleted, "不能删除别人的画像");

        assertEquals(1, profileService.getProfiles(USER_A).size(), "USER_A 的画像还在");
    }
}