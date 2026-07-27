package com.youkeda.exercise.claw.agent.memory.longterm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemoryConsolidatorTest {

    @Test
    void parsesUpdateDecision() {
        LLMClient llmClient = mock(LLMClient.class);
        when(llmClient.chatWithSystemPrompt(anyString(), anyString())).thenReturn(
                "{\"action\":\"UPDATE\",\"content\":\"用户现在不能吃辣\"}");
        MemoryConsolidator consolidator = new MemoryConsolidator(
                llmClient, new ObjectMapper());
        MemoryItem existing = MemoryItem.ofAuto(
                "u1", MemoryCategory.PREFERENCE, "diet.spicy",
                "用户喜欢吃辣", "我喜欢吃辣", 0.8f, 0.9f);
        MemoryItem incoming = MemoryItem.ofAuto(
                "u1", MemoryCategory.RULE, "diet.spicy",
                "用户现在不能吃辣", "我现在不能吃辣", 0.9f, 0.95f);

        MemoryMergeDecision decision = consolidator.decide(existing, incoming);

        assertEquals(MemoryMergeAction.UPDATE, decision.action());
        assertEquals("用户现在不能吃辣", decision.content());
    }

    @Test
    void sameTopicFallsBackToLatestValueWhenLlmFails() {
        MemoryConsolidator consolidator = failingConsolidator();
        MemoryItem existing = MemoryItem.ofAuto(
                "u1", MemoryCategory.PREFERENCE, "diet.spicy",
                "用户喜欢吃辣", "我喜欢吃辣", 0.8f, 0.9f);
        MemoryItem incoming = MemoryItem.ofAuto(
                "u1", MemoryCategory.PREFERENCE, "diet.spicy",
                "用户偏好微辣", "我现在偏好微辣", 0.8f, 0.9f);

        MemoryMergeDecision decision = consolidator.decide(existing, incoming);

        assertEquals(MemoryMergeAction.UPDATE, decision.action());
        assertEquals(incoming.content(), decision.content());
    }

    @Test
    void unresolvedTopicOnlyUpdatesWhenIncomingMessageIsExplicitCorrection() {
        MemoryConsolidator consolidator = failingConsolidator();
        MemoryItem existing = MemoryItem.ofAuto(
                "u1", MemoryCategory.PREFERENCE, "",
                "用户喜欢吃辣", "我喜欢吃辣", 0.8f, 0.9f);
        MemoryItem correction = MemoryItem.ofAuto(
                "u1", MemoryCategory.RULE, "",
                "用户现在不能吃辣", "我现在不能吃辣", 0.9f, 0.9f);
        MemoryItem additive = MemoryItem.ofAuto(
                "u1", MemoryCategory.PREFERENCE, "",
                "用户喜欢吃湘菜", "我也喜欢湘菜", 0.8f, 0.9f);

        assertEquals(MemoryMergeAction.UPDATE,
                consolidator.decide(existing, correction).action());
        assertEquals(MemoryMergeAction.ADD,
                consolidator.decide(existing, additive).action());
    }

    private MemoryConsolidator failingConsolidator() {
        LLMClient llmClient = mock(LLMClient.class);
        when(llmClient.chatWithSystemPrompt(anyString(), anyString()))
                .thenThrow(new IllegalStateException("unavailable"));
        return new MemoryConsolidator(llmClient, new ObjectMapper());
    }
}
