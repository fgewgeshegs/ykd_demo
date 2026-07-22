package com.youkeda.exercise.claw.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.classify.Intent;
import com.youkeda.exercise.claw.weather.WeatherResponse;
import com.youkeda.exercise.claw.weather.WeatherTool;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 天气 Function Calling 工具
 *
 * 复用 WeatherTool 的查询逻辑，提供 OpenAI Function Calling 接口。
 * 系统启动时自动注册到 ToolRegistry。
 */
@Slf4j
@Component
public class WeatherFunctionTool implements FunctionTool {

    private static final String TOOL_NAME = "get_weather";

    private static final String TOOL_DEFINITION = """
            {
                "type": "function",
                "function": {
                    "name": "get_weather",
                    "description": "查询指定城市的当前天气信息，包括天气状况、温度、湿度。当用户询问天气相关问题时调用此工具。",
                    "parameters": {
                        "type": "object",
                        "properties": {
                            "city": {
                                "type": "string",
                                "description": "城市名称，如：上海、北京、Tokyo、New York"
                            }
                        },
                        "required": ["city"]
                    }
                }
            }
            """;

    private final WeatherTool weatherTool;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;

    public WeatherFunctionTool(WeatherTool weatherTool, ToolRegistry toolRegistry, ObjectMapper objectMapper) {
        this.weatherTool = weatherTool;
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        toolRegistry.register(this);
        log.info("WeatherFunctionTool 已注册（Function Calling）");
    }

    @Override
    public String getToolDefinition() {
        return TOOL_DEFINITION;
    }

    @Override
    public String executeFunction(JsonNode arguments) {
        try {
            JsonNode cityNode = arguments.get("city");
            if (cityNode == null || cityNode.asText().isEmpty()) {
                return "{\"error\": \"缺少 city 参数\"}";
            }
            String city = cityNode.asText();
            log.info("Function Calling: get_weather | city={}", city);

            WeatherResponse response = weatherTool.queryWeather(city);

            return objectMapper.writeValueAsString(Map.of(
                    "city", response.getCity(),
                    "weather", response.getWeather(),
                    "temperature", response.getTemperature(),
                    "humidity", response.getHumidity()
            ));
        } catch (Exception e) {
            log.error("WeatherFunctionTool 执行失败 | error={}", e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    // ===== Tool 接口实现（保留原有兼容） =====

    @Override
    public String name() {
        return TOOL_NAME;
    }

    @Override
    public String description() {
        return "查询城市天气信息（Function Calling）";
    }

    @Override
    public Intent[] supportedIntents() {
        return new Intent[]{Intent.CHAT};
    }

    @Override
    public String execute(com.youkeda.exercise.claw.agent.AgentContext context) {
        return "请通过 Function Calling 调用此工具";
    }
}
