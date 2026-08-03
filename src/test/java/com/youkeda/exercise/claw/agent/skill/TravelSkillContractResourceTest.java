package com.youkeda.exercise.claw.agent.skill;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelSkillContractResourceTest {

    @Test
    void costCalculatorIsARequiredTravelTool() throws Exception {
        String config = resource("config/skills.yml");
        String travel = section(config, "    travel:", "    transport:");
        String required = section(travel, "      requiredTools:", "      optionalTools:");

        assertTrue(required.contains("travel_calculate_cost"));
    }

    @Test
    void promptRequiresCollectBeforePlanningAndHasOneClarificationPolicy() throws Exception {
        String prompt = resource("prompts/skills/travel.txt");

        assertTrue(prompt.contains("每次收到新的旅游需求或用户补充信息，必须先调用 travel_collect"));
        assertTrue(prompt.contains("需求未齐全前，不得生成完整行程"));
        assertTrue(prompt.contains("一次最多询问两个硬性必填项"));
        assertFalse(prompt.contains("一次只问一个问题"));
    }

    private String resource(String path) throws Exception {
        return new String(
                new ClassPathResource(path).getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
    }

    private String section(String content, String start, String end) {
        int from = content.indexOf(start);
        int to = content.indexOf(end, from + start.length());
        return content.substring(from, to);
    }
}
