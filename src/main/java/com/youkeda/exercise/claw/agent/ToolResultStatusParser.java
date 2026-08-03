package com.youkeda.exercise.claw.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.model.ResultStatus;
import org.springframework.stereotype.Component;

@Component
public class ToolResultStatusParser {

    private final ObjectMapper objectMapper;

    public ToolResultStatusParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ResultStatus parse(String resultJson) {
        if (resultJson == null || resultJson.isBlank()) return ResultStatus.FAILED;
        try {
            JsonNode node = objectMapper.readTree(resultJson);
            if (node.has("error")) return ResultStatus.FAILED;
            String status = node.path("status").asText("").toUpperCase();
            return switch (status) {
                case "SUCCESS", "STARTED", "ALL_COLLECTED" -> ResultStatus.SUCCESS;
                case "PARTIAL", "NEED_MORE_INFORMATION" -> ResultStatus.PARTIAL;
                case "BLOCKED" -> ResultStatus.BLOCKED;
                default -> ResultStatus.FAILED;
            };
        } catch (Exception e) {
            return ResultStatus.FAILED;
        }
    }
}
