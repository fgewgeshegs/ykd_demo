package com.youkeda.exercise.claw.feature.scout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoutTriggerPolicyTest {

    @Test
    void acceptsExplicitNewsDiscoveryAndTrackingRequests() {
        assertTrue(ScoutTriggerPolicy.hasExplicitRequest("帮我看看 AI Agent 最近有什么动态"));
        assertTrue(ScoutTriggerPolicy.hasExplicitRequest("跟踪 Claude Code 的版本更新"));
        assertTrue(ScoutTriggerPolicy.hasExplicitRequest("以后关注考研政策变化"));
        assertTrue(ScoutTriggerPolicy.hasExplicitRequest("启动信息猎手"));
    }

    @Test
    void rejectsInterestStatementsExplanationsAndOrdinaryFindRequests() {
        assertFalse(ScoutTriggerPolicy.hasExplicitRequest("我喜欢 AI Agent"));
        assertFalse(ScoutTriggerPolicy.hasExplicitRequest("信息猎手是什么"));
        assertFalse(ScoutTriggerPolicy.hasExplicitRequest("帮我找一下本地文件"));
        assertFalse(ScoutTriggerPolicy.hasExplicitRequest("查一下这个 bug 的原因"));
        assertFalse(ScoutTriggerPolicy.hasExplicitRequest("我需要修改一下个人信息"));
        assertFalse(ScoutTriggerPolicy.hasExplicitRequest("我需要更新账户资料"));
        assertFalse(ScoutTriggerPolicy.hasExplicitRequest("不要帮我查 AI 动态"));
        assertFalse(ScoutTriggerPolicy.hasExplicitRequest("我不想启动信息猎手"));
        assertFalse(ScoutTriggerPolicy.hasExplicitRequest("我不希望你帮我查一下最近的 AI 动态"));
    }
}
