package com.youkeda.exercise.claw.agent.skill;

import com.youkeda.exercise.claw.skill.SkillDefinition;
import com.youkeda.exercise.claw.skill.SkillsProperties;
import com.youkeda.exercise.claw.skill.SkillExecutionMode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SkillsPropertiesYamlBindingTest {

    @Test
    void bindsBackgroundWorkflowExecutionMetadataFromYaml() throws Exception {
        MutablePropertySources sources = new MutablePropertySources();
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        for (PropertySource<?> source : loader.load(
                "skills", new ClassPathResource("config/skills.yml"))) {
            sources.addLast(source);
        }
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(sources.iterator().next());

        SkillsProperties properties = Binder.get(environment)
                .bind("claw", Bindable.of(SkillsProperties.class))
                .orElseThrow(() -> new IllegalStateException("skills.yml bind failed"));
        SkillDefinition scout = properties.getSkills().get("information-scout");

        assertNotNull(scout);
        assertNotNull(scout.execution());
        assertEquals(SkillExecutionMode.BACKGROUND_WORKFLOW, scout.execution().mode());
        assertEquals("informationScoutSkillExecutor", scout.execution().executorName());
        assertEquals("scoutWorkflow",
                properties.getSkillWorkflowBindings().get("information-scout"));
    }
}
