package com.youkeda.exercise.claw.scout.collector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.scout.ScoutProperties;
import com.youkeda.exercise.claw.scout.planner.SearchTask;
import com.youkeda.exercise.claw.scout.processor.InformationItem;
import com.youkeda.exercise.claw.websearch.SearchService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSearchCollectorTest {

    @Test
    void keepsOnlyResultsWithRecentPublicationDate() {
        SearchService searchService = mock(SearchService.class);
        ScoutProperties props = new ScoutProperties();
        props.setFreshnessDays(14);
        String recent = LocalDate.now().minusDays(2).toString();
        String old = LocalDate.now().minusDays(60).toString();
        String response = """
                {"results":[
                  {"title":"近期信息","url":"https://example.com/new","content":"新","published_date":"%s"},
                  {"title":"过期信息","url":"https://example.com/old","content":"旧","published_date":"%s"},
                  {"title":"日期未知","url":"https://example.com/unknown","content":"未知"}
                ]}
                """.formatted(recent, old);
        when(searchService.searchByDate(anyString(), anyInt(), anyInt()))
                .thenReturn(response);
        WebSearchCollector collector = new WebSearchCollector(
                searchService, props, new ObjectMapper());

        List<InformationItem> items = collector.collect(SearchTask.of(
                "latest", SearchTask.NEWS, "test", 5));

        assertEquals(1, items.size());
        assertEquals("近期信息", items.get(0).getTitle());
    }
}
