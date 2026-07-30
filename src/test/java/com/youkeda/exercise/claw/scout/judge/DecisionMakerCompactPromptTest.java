package com.youkeda.exercise.claw.scout.judge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.scout.ScoutProperties;
import com.youkeda.exercise.claw.scout.context.UserProfile;
import com.youkeda.exercise.claw.scout.matcher.MatchedCandidate;
import com.youkeda.exercise.claw.scout.processor.InformationItem;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DecisionMakerCompactPromptTest {

    @Test
    void usesCompactBoundedResponseAndReusesCandidateFields() {
        LLMClient llmClient = mock(LLMClient.class);
        when(llmClient.chatWithSystemPrompt(anyString(), anyString(), eq(1200)))
                .thenReturn("[{\"index\":0,\"relevanceScore\":0.86,"
                        + "\"reason\":\"与AI应用目标相关\",\"suggestion\":\"阅读案例\"}]");
        DecisionMaker maker = new DecisionMaker(
                llmClient, new ScoutProperties(), new ObjectMapper());
        InformationItem item = InformationItem.create(
                "Agentic AI 科研案例", "完整内容", "https://example.com/ai",
                "WEB_SEARCH", "NEWS");
        item.setSummary("已有摘要");
        MatchedCandidate candidate = new MatchedCandidate(item, 0.57f, "匹配AI应用");
        UserProfile profile = new UserProfile(
                List.of("AI应用开发"), List.of(), List.of(), List.of("掌握深度学习"), "");

        List<Recommendation> recommendations = maker.judge(profile, List.of(candidate));

        assertEquals(1, recommendations.size());
        assertEquals("Agentic AI 科研案例", recommendations.get(0).title());
        assertEquals("已有摘要", recommendations.get(0).summary());
        assertEquals(Recommendation.Tier.STRONG, recommendations.get(0).tier());
        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        verify(llmClient).chatWithSystemPrompt(systemPrompt.capture(), anyString(), eq(1200));
        assertFalse(systemPrompt.getValue().contains("\"title\""));
        assertFalse(systemPrompt.getValue().contains("\"summary\""));
    }

    @Test
    void backfillsRankedCandidatesToConfiguredMinimum() {
        LLMClient llmClient = mock(LLMClient.class);
        when(llmClient.chatWithSystemPrompt(anyString(), anyString(), eq(1200)))
                .thenReturn("[{\"index\":0,\"relevanceScore\":0.90,"
                        + "\"reason\":\"强相关\",\"suggestion\":\"立即阅读\"}]");
        ScoutProperties properties = new ScoutProperties();
        properties.setMinRecommendations(3);
        properties.setMaxRecommendations(3);
        DecisionMaker maker = new DecisionMaker(llmClient, properties, new ObjectMapper());
        UserProfile profile = new UserProfile(
                List.of("AI应用开发"), List.of(), List.of(), List.of(), "");

        List<MatchedCandidate> candidates = List.of(
                candidate("第一条", 0.80f),
                candidate("第二条", 0.60f),
                candidate("第三条", 0.50f));

        List<Recommendation> recommendations = maker.judge(profile, candidates);

        assertEquals(3, recommendations.size());
        assertTrue(recommendations.stream().anyMatch(rec ->
                "第二条".equals(rec.title()) && rec.tier() == Recommendation.Tier.DISCOVERY));
        assertTrue(recommendations.stream().anyMatch(rec ->
                "第三条".equals(rec.title()) && rec.tier() == Recommendation.Tier.DISCOVERY));
    }

    @Test
    void fallsBackToRankedCandidatesWhenModelReturnsEmptyText() {
        LLMClient llmClient = mock(LLMClient.class);
        when(llmClient.chatWithSystemPrompt(anyString(), anyString(), eq(1200)))
                .thenReturn("");
        when(llmClient.chatWithSystemPrompt(anyString(), anyString(), eq(1800)))
                .thenReturn("");
        ScoutProperties properties = new ScoutProperties();
        properties.setMinRecommendations(2);
        properties.setMaxRecommendations(2);
        DecisionMaker maker = new DecisionMaker(llmClient, properties, new ObjectMapper());
        UserProfile profile = new UserProfile(
                List.of("AI应用开发"), List.of(), List.of(), List.of(), "");

        List<Recommendation> recommendations = maker.judge(
                profile, List.of(candidate("第一条", 0.70f), candidate("第二条", 0.55f)));

        assertEquals(2, recommendations.size());
        verify(llmClient).chatWithSystemPrompt(anyString(), anyString(), eq(1200));
        verify(llmClient).chatWithSystemPrompt(anyString(), anyString(), eq(1800));
        assertTrue(recommendations.stream()
                .allMatch(rec -> rec.tier() == Recommendation.Tier.DISCOVERY));
    }

    @Test
    void retriesBlankResponseWithCompactPromptAndPromotesModelSelections() {
        LLMClient llmClient = mock(LLMClient.class);
        when(llmClient.chatWithSystemPrompt(anyString(), anyString(), eq(1200)))
                .thenReturn("");
        when(llmClient.chatWithSystemPrompt(anyString(), anyString(), eq(1800)))
                .thenReturn("[{\"index\":1,\"relevanceScore\":0.72,"
                        + "\"reason\":\"实践价值高\",\"suggestion\":\"优先阅读\"}]");
        ScoutProperties properties = new ScoutProperties();
        properties.setMinRecommendations(1);
        properties.setMaxRecommendations(1);
        DecisionMaker maker = new DecisionMaker(llmClient, properties, new ObjectMapper());
        UserProfile profile = new UserProfile(
                List.of("AI应用开发"), List.of(), List.of(), List.of(), "");

        List<Recommendation> recommendations = maker.judge(
                profile, List.of(candidate("第一条", 0.70f), candidate("第二条", 0.55f)));

        assertEquals(1, recommendations.size());
        assertEquals("第二条", recommendations.get(0).title());
        assertEquals(Recommendation.Tier.STRONG, recommendations.get(0).tier());
        ArgumentCaptor<String> retryPrompt = ArgumentCaptor.forClass(String.class);
        verify(llmClient).chatWithSystemPrompt(
                anyString(), retryPrompt.capture(), eq(1800));
        assertTrue(retryPrompt.getValue().contains("请直接返回严格 JSON 数组"));
        assertFalse(retryPrompt.getValue().contains("https://example.com"));
    }

    @Test
    void doesNotRetryAValidEmptyArray() {
        LLMClient llmClient = mock(LLMClient.class);
        when(llmClient.chatWithSystemPrompt(anyString(), anyString(), eq(1200)))
                .thenReturn("[]");
        ScoutProperties properties = new ScoutProperties();
        properties.setMinRecommendations(2);
        properties.setMaxRecommendations(2);
        DecisionMaker maker = new DecisionMaker(llmClient, properties, new ObjectMapper());
        UserProfile profile = new UserProfile(
                List.of("AI应用开发"), List.of(), List.of(), List.of(), "");

        List<Recommendation> recommendations = maker.judge(
                profile, List.of(candidate("第一条", 0.70f), candidate("第二条", 0.55f)));

        assertEquals(2, recommendations.size());
        assertTrue(recommendations.stream()
                .allMatch(rec -> rec.tier() == Recommendation.Tier.DISCOVERY));
        verify(llmClient, never()).chatWithSystemPrompt(
                anyString(), anyString(), eq(1800));
    }

    @Test
    void extractsJsonArraySurroundedByReasoningAndMarkdown() {
        LLMClient llmClient = mock(LLMClient.class);
        when(llmClient.chatWithSystemPrompt(anyString(), anyString(), eq(1200)))
                .thenReturn("""
                        <think>先分析候选信息。</think>
                        以下是最终结果：
                        ```json
                        [{"index":0,"relevanceScore":0.81,
                          "reason":"适合实践","suggestion":"优先阅读"}]
                        ```
                        """);
        ScoutProperties properties = new ScoutProperties();
        properties.setMinRecommendations(1);
        properties.setMaxRecommendations(1);
        DecisionMaker maker = new DecisionMaker(llmClient, properties, new ObjectMapper());

        List<Recommendation> recommendations = maker.judge(
                new UserProfile(List.of("AI应用开发"), List.of(), List.of(), List.of(), ""),
                List.of(candidate("实践案例", 0.60f)));

        assertEquals(1, recommendations.size());
        assertEquals(Recommendation.Tier.STRONG, recommendations.get(0).tier());
        verify(llmClient, never()).chatWithSystemPrompt(
                anyString(), anyString(), eq(1800));
    }

    @Test
    void acceptsRecommendationsWrappedInAnObject() {
        LLMClient llmClient = mock(LLMClient.class);
        when(llmClient.chatWithSystemPrompt(anyString(), anyString(), eq(1200)))
                .thenReturn("""
                        {"recommendations":[
                          {"index":0,"relevanceScore":0.78,
                           "reason":"方向匹配","suggestion":"阅读原文"}
                        ]}
                        """);
        ScoutProperties properties = new ScoutProperties();
        properties.setMinRecommendations(1);
        properties.setMaxRecommendations(1);
        DecisionMaker maker = new DecisionMaker(llmClient, properties, new ObjectMapper());

        List<Recommendation> recommendations = maker.judge(
                new UserProfile(List.of("AI应用开发"), List.of(), List.of(), List.of(), ""),
                List.of(candidate("AI 项目", 0.58f)));

        assertEquals(1, recommendations.size());
        assertEquals(Recommendation.Tier.STRONG, recommendations.get(0).tier());
    }

    private MatchedCandidate candidate(String title, float score) {
        InformationItem item = InformationItem.create(
                title, title + "内容", "https://example.com/" + title,
                "WEB_SEARCH", "NEWS");
        item.setSummary(title + "摘要");
        return new MatchedCandidate(item, score, "匹配用户画像");
    }
}
