package com.youkeda.exercise.claw.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.config.WeatherConfig;
import com.youkeda.exercise.claw.exception.ClawException;
import com.youkeda.exercise.claw.model.WeatherResponse;
import com.youkeda.exercise.claw.util.HttpClientUtil;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 天气查询工具
 * 调用外部天气 API 获取天气信息
 */
public class WeatherTool {

    private final WeatherConfig config;
    private final HttpClientUtil httpClient;
    private final ObjectMapper objectMapper;

    public WeatherTool(WeatherConfig config) {
        this.config = config;
        this.httpClient = new HttpClientUtil();
        this.objectMapper = new ObjectMapper();
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
            String url = config.getApiUrl()
                    .replace("{city}", encodedCity)
                    .replace("{key}", config.getApiKey());

            // 2. 发送 HTTP GET 请求
            String responseBody = httpClient.doGet(url);

            // 3. 解析 JSON
            return parseResponse(responseBody, city);

        } catch (ClawException e) {
            throw e;
        } catch (Exception e) {
            throw new ClawException("天气服务暂时不可用", e);
        }
    }

    /**
     * 解析天气 API 返回的 JSON 数据
     * 支持 OpenWeatherMap 格式
     */
    private WeatherResponse parseResponse(String json, String originalCity) throws ClawException {
        try {
            JsonNode root = objectMapper.readTree(json);

            // 检查 API 是否返回错误
            JsonNode codNode = root.get("cod");
            if (codNode != null) {
                String cod = codNode.asText();
                if (!"200".equals(cod)) {
                    if ("404".equals(cod)) {
                        throw new ClawException("城市不存在");
                    }
                    String message = root.has("message") ? root.get("message").asText() : "未知错误";
                    throw new ClawException("天气 API 返回错误: " + message);
                }
            }

            // 解析天气数据
            String cityName = root.has("name") ? root.get("name").asText() : originalCity;
            JsonNode weatherArray = root.get("weather");
            String weatherDesc = weatherArray != null && weatherArray.isArray() && weatherArray.size() > 0
                    ? weatherArray.get(0).get("description").asText()
                    : "未知";

            JsonNode main = root.get("main");
            double temp = main != null && main.has("temp") ? main.get("temp").asDouble() : 0.0;
            int humidity = main != null && main.has("humidity") ? main.get("humidity").asInt() : 0;

            return new WeatherResponse(cityName, weatherDesc, temp, humidity);

        } catch (ClawException e) {
            throw e;
        } catch (Exception e) {
            throw new ClawException("解析天气数据失败", e);
        }
    }
}
