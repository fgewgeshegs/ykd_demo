package com.youkeda.exercise.claw.scout.matcher;

import com.youkeda.exercise.claw.agent.memory.longterm.EmbeddingClient;
import com.youkeda.exercise.claw.scout.ScoutProperties;
import com.youkeda.exercise.claw.scout.context.UserProfile;
import com.youkeda.exercise.claw.scout.processor.InformationItem;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CandidateMatcherTest {

    @Test
    void oldAndUndatedItemsCannotBecomeCandidates() {
        ScoutProperties props = new ScoutProperties();
        props.setFreshnessDays(14);
        CandidateMatcher matcher = new CandidateMatcher(
                mock(EmbeddingClient.class), props);
        UserProfile emptyProfile = new UserProfile(
                "owner", List.of(), List.of(), List.of(), List.of(), "");
        InformationItem recent = item("近期", Instant.now().minus(2, ChronoUnit.DAYS).toEpochMilli());
        InformationItem old = item("旧闻", Instant.now().minus(60, ChronoUnit.DAYS).toEpochMilli());
        InformationItem unknown = item("未知日期", 0L);

        List<MatchedCandidate> result = matcher.match(
                "owner", emptyProfile, List.of(recent, old, unknown));

        assertEquals(1, result.size());
        assertEquals("近期", result.get(0).item().getTitle());
    }

    @Test
    void matchesAgainstBestIndividualProfileFacet() {
        ScoutProperties props = new ScoutProperties();
        props.setMinMatchScore(0.80f);
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        when(embeddingClient.embedBatch(List.of("兴趣：考研", "兴趣：AI应用开发")))
                .thenReturn(List.of(
                        new float[]{1f, 0f},
                        new float[]{0f, 1f}));
        CandidateMatcher matcher = new CandidateMatcher(embeddingClient, props);
        UserProfile profile = new UserProfile(
                "owner",
                List.of("考研", "AI应用开发"),
                List.of(), List.of(), List.of(), "");
        InformationItem aiNews = item(
                "AI Agent 框架发布",
                Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli());
        aiNews.setVector(new float[]{0f, 1f});

        List<MatchedCandidate> result = matcher.match(
                "owner", profile, List.of(aiNews));

        assertEquals(1, result.size());
        assertEquals(1f, result.get(0).semanticScore());
        assertEquals("匹配画像维度：兴趣：AI应用开发", result.get(0).matchReason());
        verify(embeddingClient).embedBatch(List.of("兴趣：考研", "兴趣：AI应用开发"));
    }

    @Test
    void usesLimitedFallbackCandidatesWhenStrictThresholdHasNoHits() {
        ScoutProperties props = new ScoutProperties();
        props.setMinMatchScore(0.60f);
        props.setFallbackMatchScore(0.30f);
        props.setFallbackCandidateCount(2);
        EmbeddingClient embeddingClient = mock(EmbeddingClient.class);
        when(embeddingClient.embedBatch(List.of("兴趣：AI应用开发")))
                .thenReturn(List.of(new float[]{1f, 0f}));
        CandidateMatcher matcher = new CandidateMatcher(embeddingClient, props);
        UserProfile profile = new UserProfile(
                "owner", List.of("AI应用开发"),
                List.of(), List.of(), List.of(), "");
        InformationItem candidate = item(
                "Agent 工具更新",
                Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli());
        candidate.setVector(new float[]{0.40f, 0.9165f});

        List<MatchedCandidate> result = matcher.match(
                "owner", profile, List.of(candidate));

        assertEquals(1, result.size());
        assertEquals(0.40f, result.get(0).semanticScore(), 0.001f);
    }

    private InformationItem item(String title, long publishedAt) {
        InformationItem item = InformationItem.create(
                "owner", title, "内容", "https://example.com/" + title,
                "WEB_SEARCH", "NEWS");
        item.setPublishedAt(publishedAt);
        item.setVector(new float[]{1f, 0f});
        return item;
    }
}
