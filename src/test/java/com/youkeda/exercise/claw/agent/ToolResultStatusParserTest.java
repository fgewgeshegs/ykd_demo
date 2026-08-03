package com.youkeda.exercise.claw.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.model.ResultStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolResultStatusParserTest {

    private final ToolResultStatusParser parser =
            new ToolResultStatusParser(new ObjectMapper());

    @Test
    void treatsStartedBackgroundWorkflowAsSuccessfulToolExecution() {
        assertEquals(ResultStatus.SUCCESS,
                parser.parse("{\"status\":\"started\",\"taskId\":\"task-1\"}"));
    }

    @Test
    void treatsMissingTravelInformationAsPartialExecution() {
        assertEquals(ResultStatus.PARTIAL,
                parser.parse("{\"status\":\"NEED_MORE_INFORMATION\"}"));
    }

    @Test
    void treatsCompletedTravelCollectionAsSuccessfulExecution() {
        assertEquals(ResultStatus.SUCCESS,
                parser.parse("{\"status\":\"ALL_COLLECTED\"}"));
    }

    @Test
    void treatsMalformedToolOutputAsFailedExecution() {
        assertEquals(ResultStatus.FAILED, parser.parse("not-json"));
    }

    @Test
    void treatsMissingStatusAsFailedExecution() {
        assertEquals(ResultStatus.FAILED, parser.parse("{}"));
    }
}
