package com.youkeda.exercise.claw.scout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoutPropertiesMatchThresholdTest {

    @Test
    void usesBroaderRecallAndOutputDefaults() {
        ScoutProperties properties = new ScoutProperties();

        assertEquals(0.45f, properties.getMinMatchScore(), 0.0001f);
        assertEquals(0.25f, properties.getFallbackMatchScore(), 0.0001f);
        assertEquals(8, properties.getFallbackCandidateCount());
        assertEquals(14, properties.getFreshnessDays());
        assertEquals(20, properties.getMaxCandidates());
        assertEquals(8, properties.getMinRecommendations());
        assertEquals(10, properties.getMaxRecommendations());
        assertEquals(10, properties.getMaxResultsPerTask());
        assertEquals(7, properties.getRss().getFreshnessDays());
    }
}
