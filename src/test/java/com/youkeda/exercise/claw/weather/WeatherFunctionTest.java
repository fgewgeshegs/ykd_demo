package com.youkeda.exercise.claw.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class WeatherFunctionTest {

    @Test
    void shouldNotUseCurrentWeatherForFarFutureTrip() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        WeatherTool weatherTool = mock(WeatherTool.class);
        WeatherFunction function = new WeatherFunction(
                weatherTool, objectMapper, new LLMFunctionRegistry());
        String date = LocalDate.now().plusDays(15).toString();

        JsonNode result = objectMapper.readTree(function.execute(
                "{\"city\":\"杭州\",\"date\":\"" + date + "\"}"));

        assertEquals("UNAVAILABLE", result.path("status").asText());
        assertTrue(result.path("instruction").asText().contains("复查天气"));
        verifyNoInteractions(weatherTool);
    }
}
