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
}
