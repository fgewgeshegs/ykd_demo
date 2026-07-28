package com.youkeda.exercise.claw.agent.memory.longterm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemoryTopicResolverTest {

    @Test
    void llmFailureLeavesTopicUnresolvedForSemanticFallback() {
        LLMClient llmClient = mock(LLMClient.class);
        when(llmClient.chatWithSystemPrompt(anyString(), anyString()))
                .thenThrow(new IllegalStateException("unavailable"));
        MemoryTopicResolver resolver = new MemoryTopicResolver(
                llmClient, new ObjectMapper());

        MemoryTopicResolver.TopicResolution result = resolver.resolve(
                MemoryCategory.PREFERENCE, "用户现在不能吃辣");

        assertEquals("", result.topicKey());
        assertEquals(0.3f, result.confidence());
    }
}
