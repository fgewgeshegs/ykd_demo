package com.youkeda.exercise.claw.agent.memory.longterm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemoryExtractorTest {

    @Test
    void extractionKeepsTopicEvidenceAndConfidence() {
        LLMClient llmClient = mock(LLMClient.class);
        when(llmClient.chatWithSystemPrompt(anyString(), anyString())).thenReturn("""
                [{"topicKey":"diet.spicy","content":"用户现在不能吃辣",\
                "category":"RULE","importance":0.9,"confidence":0.96}]
                """);
        MemoryExtractor extractor = new MemoryExtractor(llmClient, new ObjectMapper());

        List<MemoryItem> result = extractor.extract(
                "u1", "我现在不能吃辣", "知道了");

        assertEquals(1, result.size());
        assertEquals("diet.spicy", result.get(0).topicKey());
        assertEquals("我现在不能吃辣", result.get(0).evidence());
        assertEquals(0.96f, result.get(0).confidence());
    }
}
