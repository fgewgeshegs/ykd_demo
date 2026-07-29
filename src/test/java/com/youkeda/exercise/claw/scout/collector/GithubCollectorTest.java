package com.youkeda.exercise.claw.scout.collector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.scout.ScoutProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GithubCollectorTest {

    @Test
    void removesPlannerDateSyntaxBeforeBuildingGithubQuery() {
        GithubCollector collector = new GithubCollector(
                new ScoutProperties(), new ObjectMapper());

        assertEquals(
                "AI application development framework release",
                collector.cleanQuery(
                        "AI application development framework release 2026 after:2026-07-14"));
        assertEquals(
                "AI agent",
                collector.cleanQuery("AI agent published after 2026-07-14"));
    }
}
