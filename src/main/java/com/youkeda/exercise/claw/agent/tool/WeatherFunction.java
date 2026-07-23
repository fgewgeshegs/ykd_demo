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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

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
        return "查询指定城市当前或出行日期的天气。完整出游方案必须在目的地和日期确定后调用。"
                + "远期日期超出可靠预报范围时返回 UNAVAILABLE，此时可用 web_search 查询同期气候参考，"
                + "但必须提醒用户出发前3至7天复查。";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("type", "object");

        ObjectNode properties = params.putObject("properties");
        ObjectNode city = properties.putObject("city");
        city.put("type", "string");
        city.put("description", "城市名称，如：北京、上海、广州、深圳");

        ObjectNode date = properties.putObject("date");
        date.put("type", "string");
        date.put("description", "可选，出行日期，格式 yyyy-MM-dd；不传则查询当前天气");

        params.putArray("required").add("city");

        return params;
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            JsonNode cityNode = args.has("city") ? args.get("city") : args.get("location");
            if (cityNode == null || cityNode.asText().isBlank()) {
                ObjectNode result = objectMapper.createObjectNode();
                result.put("status", "ERROR");
                result.put("source", "WEATHER_API");
                result.put("error", "缺少必填参数: city");
                result.put("fallback_required", false);
                return result.toString();
            }

            String city = cityNode.asText();
            log.info("WeatherFunction 执行 | city={}", city);

            String dateText = args.path("date").asText("");
            LocalDate targetDate = null;
            if (!dateText.isBlank()) {
                try {
                    targetDate = LocalDate.parse(dateText);
                } catch (DateTimeParseException e) {
                    return unavailable("日期格式必须为 yyyy-MM-dd", false);
                }
                long days = ChronoUnit.DAYS.between(LocalDate.now(), targetDate);
                if (days < 0) return unavailable("不能查询过去日期的天气预报", true);
                if (days > 14) return unavailable("日期超出14天可靠预报范围", true);
            }

            WeatherResponse response = targetDate == null
                    ? weatherTool.queryWeather(city)
                    : weatherTool.queryWeather(city, targetDate);
            ObjectNode result = objectMapper.createObjectNode();
            result.put("status", "SUCCESS");
            result.put("source", "WEATHER_API");
            result.set("data", objectMapper.valueToTree(response));
            result.put("fallback_required", false);
            return objectMapper.writeValueAsString(result);

        } catch (Exception e) {
            log.error("WeatherFunction 执行失败 | args={} | error={}", argumentsJson, e.getMessage());
            ObjectNode result = objectMapper.createObjectNode();
            result.put("status", "ERROR");
            result.put("source", "WEATHER_API");
            result.put("error", e.getMessage());
            result.put("fallback_required", true);
            return result.toString();
        }
    }

    private String unavailable(String reason, boolean webFallback) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("status", "UNAVAILABLE");
        result.put("source", "WEATHER_API");
        result.put("reason", reason);
        result.put("fallback_required", webFallback);
        if (webFallback) {
            result.put("instruction", "可用 web_search 查询当地同期气候作为参考，并提醒出发前3至7天复查天气。不得把同期气候写成准确预报。");
        }
        return result.toString();
    }
}
