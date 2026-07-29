package com.youkeda.exercise.claw.scout.planner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.scout.ScoutProperties;
import com.youkeda.exercise.claw.scout.context.UserProfile;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SearchPlannerSourcePolicyTest {

    @Test
    void dropsUnsupportedCategoriesAndFillsExecutableSearchAngles() {
        LLMClient llmClient = mock(LLMClient.class);
        when(llmClient.chatWithSystemPrompt(anyString(), anyString())).thenReturn("""
                [
                  {"query":"AI jobs latest","category":"JOB","reason":"jobs","priority":3},
                  {"query":"AI competition latest","category":"COMPETITION","reason":"contest","priority":3},
                  {"query":"AI framework latest","category":"NEWS","reason":"news","priority":5}
                ]
                """);
        SearchPlanner planner = new SearchPlanner(
                llmClient, new ScoutProperties(), new ObjectMapper());
        UserProfile profile = new UserProfile(
                List.of("AI"), List.of(), List.of(), List.of(), "");

        List<SearchTask> tasks = planner.plan(profile);

        assertEquals(5, tasks.size());
        assertTrue(tasks.stream().allMatch(task ->
                List.of(SearchTask.NEWS, SearchTask.BLOG, SearchTask.GITHUB)
                        .contains(task.category())));
        assertTrue(tasks.stream().noneMatch(task ->
                SearchTask.JOB.equals(task.category())
                        || SearchTask.COMPETITION.equals(task.category())));
        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        verify(llmClient).chatWithSystemPrompt(systemPrompt.capture(), anyString());
        assertTrue(systemPrompt.getValue().contains("NEWS、BLOG、GITHUB"));
    }
}
