package com.youkeda.exercise.claw.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.weather.WeatherResponse;
import com.youkeda.exercise.claw.weather.WeatherTool;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 天气查询函数（LLM Function Calling 适配器）
 *
 * <p>包装 {@link WeatherTool}，使其可供 LLM 通过工具调用方式使用。
 * LLM 生成 {"city": "北京"} 参数，本函数执行后返回 JSON 格式的天气数据。
 */
@Component
public class WeatherFunction implements LLMFunction {

    private static final Logger log = LoggerFactory.getLogger(WeatherFunction.class);

    private final WeatherTool weatherTool;
    private final ObjectMapper objectMapper;
    private final LLMFunctionRegistry functionRegistry;

    public WeatherFunction(WeatherTool weatherTool,
                            ObjectMapper objectMapper,
                            LLMFunctionRegistry functionRegistry) {
        this.weatherTool = weatherTool;
        this.objectMapper = objectMapper;
        this.functionRegistry = functionRegistry;
    }

    @PostConstruct
    public void init() {
        functionRegistry.register(this);
        log.info("WeatherFunction 已注册到 LLMFunctionRegistry");
    }

    @Override
    public String getName() {
        return "weather_query";
    }

    @Override
    public String getDescription() {
        return "查询指定城市的实时天气信息，返回天气状况、温度、湿度等数据。城市名称支持中文，如：北京、上海、广州、深圳、杭州等";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("type", "object");

        ObjectNode properties = params.putObject("properties");
        ObjectNode city = properties.putObject("city");
        city.put("type", "string");
        city.put("description", "城市名称，如：北京、上海、广州、深圳");

        params.putArray("required").add("city");

        return params;
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            JsonNode cityNode = args.get("city");
            if (cityNode == null) {
                return "{\"error\": \"缺少必填参数: city\"}";
            }

            String city = cityNode.asText();
            log.info("WeatherFunction 执行 | city={}", city);

            WeatherResponse response = weatherTool.queryWeather(city);

            // 将 WeatherResponse 序列化为 JSON 字符串返回给 LLM
            return objectMapper.writeValueAsString(response);

        } catch (Exception e) {
            log.error("WeatherFunction 执行失败 | args={} | error={}", argumentsJson, e.getMessage());
            return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }
}
