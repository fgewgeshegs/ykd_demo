package com.youkeda.exercise.claw.websearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchServiceTest {

    @Test
    void formattedResponseKeepsPublishedDate() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        SearchService service = new SearchService(new WebSearchConfig(), objectMapper);
        String raw = """
                {"results":[{
                  "title":"标题",
                  "url":"https://example.com",
                  "content":"内容",
                  "score":0.9,
                  "published_date":"2026-07-28"
                }]}
                """;

        JsonNode result = objectMapper.readTree(service.formatResponse(raw, "query"));

        assertEquals("2026-07-28",
                result.path("results").get(0).path("published_date").asText());
    }
}
