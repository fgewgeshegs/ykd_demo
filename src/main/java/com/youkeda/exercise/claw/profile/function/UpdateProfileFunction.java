package com.youkeda.exercise.claw.profile.function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.tool.FunctionExecutionContext;
import com.youkeda.exercise.claw.agent.tool.LLMFunction;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import com.youkeda.exercise.claw.profile.model.UserProfile;
import com.youkeda.exercise.claw.profile.service.ProfileService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 更新用户画像 LLM Function
 *
 * <p>注册名称：{@code update_profile}
 *
 * <p>LLM 在与用户对话过程中总结用户特征后调用此函数保存画像。
 * 画像在 Memory（具体事实）之上做更高层级的特征总结。
 *
 * <p>保存场景：
 * <ul>
 *   <li>IDENTITY — 用户身份（如"学生开发者""大厂工程师"）</li>
 *   <li>SKILL — 技术/技能方向（如"Java""AI Agent"）</li>
 *   <li>GOAL — 当前目标（如"参加比赛""求职"）</li>
 *   <li>INTEREST — 兴趣方向（如"开源""创业"）</li>
 * </ul>
 */
@Component
public class UpdateProfileFunction implements LLMFunction {

    private static final Logger log = LoggerFactory.getLogger(UpdateProfileFunction.class);

    private final ObjectMapper objectMapper;
    private final LLMFunctionRegistry functionRegistry;
    private final ProfileService profileService;

    public UpdateProfileFunction(ObjectMapper objectMapper,
                                 LLMFunctionRegistry functionRegistry,
                                 ProfileService profileService) {
        this.objectMapper = objectMapper;
        this.functionRegistry = functionRegistry;
        this.profileService = profileService;
    }

    @PostConstruct
    public void init() {
        functionRegistry.register(this);
        log.info("UpdateProfileFunction 已注册到 LLMFunctionRegistry");
    }

    @Override
    public String getName() {
        return "update_profile";
    }

    @Override
    public String getDescription() {
        return "更新用户画像。\n"
                + "在对话中总结用户特征时调用，保存更高层级的用户画像信息。\n"
                + "画像与记忆（save_memory）不同——记忆保存具体事实，画像保存总结性特征。\n\n"
                + "场景举例：\n"
                + "- 用户介绍了自己的技术栈 → 保存为 SKILL\n"
                + "- 用户说了自己的身份 → 保存为 IDENTITY\n"
                + "- 用户表达了目标 → 保存为 GOAL\n"
                + "- 用户表达了对某些领域的兴趣 → 保存为 INTEREST\n\n"
                + "profile_type 可选值：\n"
                + "- IDENTITY：身份（如「学生开发者」「后端工程师」）\n"
                + "- SKILL：技能方向（如「Java」「AI Agent」「Spring Boot」）\n"
                + "- GOAL：目标（如「参加比赛」「求职」「学习 AI」）\n"
                + "- INTEREST：兴趣（如「开源」「创业」「技术写作」）\n\n"
                + "profile_key 为主题，profile_value 为具体描述。\n"
                + "相同 profile_type+profile_key 会覆盖更新。";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("type", "object");

        ObjectNode properties = params.putObject("properties");

        // profile_type
        ObjectNode type = properties.putObject("profile_type");
        type.put("type", "string");
        type.put("description", "画像类型：IDENTITY=身份, SKILL=技能, GOAL=目标, INTEREST=兴趣");
        type.putArray("enum").add("IDENTITY").add("SKILL").add("GOAL").add("INTEREST");

        // profile_key
        ObjectNode key = properties.putObject("profile_key");
        key.put("type", "string");
        key.put("description", "画像主题。如「role」「programming」「technology」「direction」等。");

        // profile_value
        ObjectNode value = properties.putObject("profile_value");
        value.put("type", "string");
        value.put("description", "画像内容。如「学生开发者」「Java、Spring Boot、AI Agent」「参加比赛」等。");

        params.putArray("required").add("profile_type").add("profile_key").add("profile_value");

        return params;
    }

    @Override
    public String execute(String argumentsJson) {
        return "{\"error\": \"缺少用户上下文，无法更新画像\"}";
    }

    @Override
    public String execute(String argumentsJson, FunctionExecutionContext context) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            String userId = context.userId();

            if (userId == null || userId.isBlank()) {
                return "{\"error\": \"缺少用户ID\"}";
            }

            // 解析参数
            String profileType = args.has("profile_type") ? args.get("profile_type").asText().toUpperCase() : "";
            if (!UserProfile.PROFILE_TYPES.contains(profileType)) {
                return "{\"error\": \"不支持的画像类型: " + profileType + "\"}";
            }

            String profileKey = args.has("profile_key") ? args.get("profile_key").asText().strip() : "";
            if (profileKey.isEmpty()) {
                return "{\"error\": \"缺少 profile_key（画像主题）\"}";
            }

            String profileValue = args.has("profile_value") ? args.get("profile_value").asText().strip() : "";
            if (profileValue.isEmpty()) {
                return "{\"error\": \"缺少 profile_value（画像内容）\"}";
            }

            profileService.saveProfile(userId, profileType, profileKey, profileValue);

            log.info("画像已保存 | userId={} | type={} | key={} | value={}",
                    userId, profileType, profileKey, profileValue);

            ObjectNode result = objectMapper.createObjectNode();
            result.put("status", "saved");
            result.put("profile_type", profileType);
            result.put("profile_key", profileKey);
            result.put("profile_value", profileValue);
            result.put("message", "已更新用户画像: " + UserProfile.profileTypeDisplay(profileType)
                    + " - " + profileKey + "=" + profileValue);

            return result.toString();

        } catch (Exception e) {
            log.error("更新画像失败 | args={} | error={}", argumentsJson, e.getMessage(), e);
            return "{\"error\": \"更新画像失败: " + e.getMessage().replace("\"", "'") + "\"}";
        }
    }
}