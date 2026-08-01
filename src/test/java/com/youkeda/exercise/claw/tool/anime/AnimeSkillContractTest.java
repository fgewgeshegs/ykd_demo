package com.youkeda.exercise.claw.tool.anime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.skill.SkillDefinition;
import com.youkeda.exercise.claw.skill.SkillsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * anime 订阅契约测试：
 * <ul>
 *   <li>「订阅X」必须能路由到 anime skill 且 anime_subscribe 在白名单中、注册名一致（否则 LLM 看不到/调不到工具）；</li>
 *   <li>anime 提示词必须强制订阅调用工具、禁止凭推荐结果确认订阅。</li>
 * </ul>
 */
class AnimeSkillContractTest {

    @Test
    void animeSkillExposesSubscribeTool() throws Exception {
        SkillsProperties properties = bindSkillsProperties();
        SkillDefinition anime = properties.getSkills().get("anime");

        assertNotNull(anime,
                "anime 技能必须在 skills.yml 中定义，否则「订阅无职转生」无法路由到 anime_subscribe");

        Set<String> tools = anime.allowedTools();
        assertTrue(tools.contains("anime_subscribe"),
                "anime skill 必须声明 anime_subscribe，否则 LLM 看不到该工具");
        assertTrue(tools.contains("anime_recommend"));

        // 注册名必须与白名单一致，否则 ToolRegistry.find("anime_subscribe") 返回 null
        //（回归背景：didi_taxi 与 didi_ride 名字不一致导致工具从未暴露）
        AnimeSubscribeTool tool = new AnimeSubscribeTool(null, null, null, new ObjectMapper());
        assertEquals("anime_subscribe", tool.getName(),
                "AnimeSubscribeTool 注册名必须与 skills.yml anime.optionalTools 声明一致");
    }

    @Test
    void promptRequiresSubscribeCallAndForbidsConfirmingFromRecommendation() throws Exception {
        String prompt = Files.readString(Path.of(
                "src/main/resources/prompts/skills/anime.txt"), StandardCharsets.UTF_8);

        assertTrue(prompt.contains("必须调用 anime_subscribe"),
                "提示词必须要求：订阅必须调用 anime_subscribe");
        assertTrue(prompt.contains("不得仅凭推荐结果声称已订阅"),
                "提示词必须禁止凭推荐结果确认订阅");
        assertTrue(prompt.contains("不得回复\"订阅成功\""),
                "提示词必须禁止在工具返回 ERROR 时回复订阅成功");
    }

    private SkillsProperties bindSkillsProperties() throws Exception {
        MutablePropertySources sources = new MutablePropertySources();
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        for (PropertySource<?> source : loader.load(
                "skills", new ClassPathResource("config/skills.yml"))) {
            sources.addLast(source);
        }
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(sources.iterator().next());
        return Binder.get(environment)
                .bind("claw", Bindable.of(SkillsProperties.class))
                .orElseThrow(() -> new IllegalStateException("skills.yml bind failed"));
    }
}