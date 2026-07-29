package com.youkeda.exercise.claw.scout.collector;

import com.youkeda.exercise.claw.scout.planner.SearchTask;
import com.youkeda.exercise.claw.scout.processor.InformationItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CollectorRegistryTest {

    @Test
    void routesTasksToMatchingCollectorsAndCollectsRssOnce() {
        Collector web = collector("WEB_SEARCH");
        Collector github = collector("GITHUB");
        Collector rss = collector("RSS");
        when(web.collect(any())).thenReturn(List.of(item("WEB_SEARCH")));
        when(github.collect(any())).thenReturn(List.of(item("GITHUB")));
        when(rss.collect(any())).thenReturn(List.of(item("RSS")));
        CollectorRegistry registry = new CollectorRegistry(List.of(web, github, rss));

        SearchTask news = SearchTask.of("AI news", SearchTask.NEWS, "news", 5);
        SearchTask project = SearchTask.of("agent framework", SearchTask.GITHUB, "code", 4);
        List<InformationItem> result = registry.collectAll(
                List.of(news, project), "owner");

        assertEquals(3, result.size());
        assertEquals(List.of("WEB_SEARCH", "GITHUB", "RSS"),
                result.stream().map(InformationItem::getSourceType).toList());
        result.forEach(item -> assertEquals("owner", item.getUserId()));
        verify(web).collect(news);
        verify(github).collect(project);
        verify(rss, times(1)).collect(any());
    }

    @Test
    void fallsBackToWebSearchWhenGithubCollectorIsUnavailable() {
        Collector web = collector("WEB_SEARCH");
        when(web.collect(any())).thenReturn(List.of(item("WEB_SEARCH")));
        CollectorRegistry registry = new CollectorRegistry(List.of(web));
        SearchTask project = SearchTask.of("agent framework", SearchTask.GITHUB, "code", 4);

        List<InformationItem> result = registry.collectAll(List.of(project), "owner");

        assertEquals(1, result.size());
        verify(web).collect(project);
    }

    private static Collector collector(String type) {
        Collector collector = mock(Collector.class);
        when(collector.getType()).thenReturn(type);
        return collector;
    }

    private static InformationItem item(String sourceType) {
        return InformationItem.create(
                "", sourceType + " title", "content",
                "https://example.com/" + sourceType, sourceType, SearchTask.NEWS);
    }
}
