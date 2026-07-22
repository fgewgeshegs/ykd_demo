package com.youkeda.exercise.claw.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.ClawException;
import com.youkeda.exercise.claw.common.HttpClientUtil;
import com.youkeda.exercise.claw.weather.WeatherConfig;
import com.youkeda.exercise.claw.weather.WeatherResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 天气查询工具
 * 调用外部天气 API 获取天气信息
 */
@Component
public class WeatherTool {

    private static final Logger log = LoggerFactory.getLogger(WeatherTool.class);

    private final WeatherConfig config;
    private final HttpClientUtil httpClient;
    private final ObjectMapper objectMapper;

    public WeatherTool(WeatherConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.httpClient = new HttpClientUtil();
    }

    /**
     * 查询指定城市的天气
     *
     * @param city 城市名称
     * @return 天气响应数据
     * @throws ClawException 查询失败时抛出
     */
    public WeatherResponse queryWeather(String city) throws ClawException {
        try {
            // 1. 构建请求 URL（替换占位符）
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String url = config.getUrl()
                    .replace("{city}", encodedCity)
                    .replace("{key}", config.getKey());

            // 2. 发送 HTTP GET 请求
            String responseBody = httpClient.doGet(url);

            // 3. 解析 JSON
            return parseResponse(responseBody, city);

        } catch (Exception e) {
            if (e instanceof ClawException ce) throw ce;
            throw new ClawException("天气服务暂时不可用", e);
        }
    }

    /**
     * 解析天气 API 返回的 JSON 数据
     * 支持 WeatherAPI.com 格式
     */
    private WeatherResponse parseResponse(String json, String originalCity) throws ClawException {
        try {
            JsonNode root = objectMapper.readTree(json);

            // 检查 API 是否返回错误
            JsonNode errorNode = root.get("error");
            if (errorNode != null) {
                String message = errorNode.has("message") ? errorNode.get("message").asText() : "未知错误";
                throw new ClawException("天气 API 返回错误: " + message);
            }

            // 解析天气数据
            JsonNode location = root.get("location");
            String cityName = location != null && location.has("name")
                    ? location.get("name").asText()
                    : originalCity;

            JsonNode current = root.get("current");
            String weatherDesc = "未知";
            if (current != null && current.has("condition")) {
                JsonNode condition = current.get("condition");
                weatherDesc = condition.has("text") ? condition.get("text").asText() : "未知";
            }

            double temp = current != null && current.has("temp_c") ? current.get("temp_c").asDouble() : 0.0;
            int humidity = current != null && current.has("humidity") ? current.get("humidity").asInt() : 0;

            return new WeatherResponse(cityName, weatherDesc, temp, humidity);

        } catch (Exception e) {
            if (e instanceof ClawException ce) throw ce;
            throw new ClawException("解析天气数据失败", e);
        }
    }
}
