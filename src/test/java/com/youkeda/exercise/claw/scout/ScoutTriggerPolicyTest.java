package com.youkeda.exercise.claw.scout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoutTriggerPolicyTest {

    @Test
    void shouldRejectTopicStatementsAndShortFollowUpAnswers() {
        assertFalse(ScoutTriggerPolicy.hasExplicitRequest("我经常参加一些计算机类的比赛"));
        assertFalse(ScoutTriggerPolicy.hasExplicitRequest("算法类、应用开发类和综合类都涉及一点"));
        assertFalse(ScoutTriggerPolicy.hasExplicitRequest("都涉及一点"));
        assertFalse(ScoutTriggerPolicy.hasExplicitRequest("最近在做 Agent 项目"));
    }

    @Test
    void shouldRejectNegatedOrDescriptiveMentions() {
        assertFalse(ScoutTriggerPolicy.hasExplicitRequest("不要调用信息猎手"));
        assertFalse(ScoutTriggerPolicy.hasExplicitRequest("不用帮我找资料"));
        assertFalse(ScoutTriggerPolicy.hasExplicitRequest("你刚才为什么调用了信息猎手"));
        assertFalse(ScoutTriggerPolicy.hasExplicitRequest("你会不会调用信息猎手"));
        assertFalse(ScoutTriggerPolicy.hasExplicitRequest("信息猎手是什么"));
    }

    @Test
    void shouldAllowExplicitInformationRequests() {
        assertTrue(ScoutTriggerPolicy.hasExplicitRequest("帮我找找最近的计算机比赛信息"));
        assertTrue(ScoutTriggerPolicy.hasExplicitRequest("搜搜看最近有什么比赛"));
        assertTrue(ScoutTriggerPolicy.hasExplicitRequest("今天有什么值得关注的"));
        assertTrue(ScoutTriggerPolicy.hasExplicitRequest("我需要一些 AI 行业资讯"));
        assertTrue(ScoutTriggerPolicy.hasExplicitRequest("启动信息猎手"));
    }
}
