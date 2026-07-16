package com.youkeda.exercise.claw.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 天气 API 配置
 * 从 application.properties 加载天气服务配置
 */
public class WeatherConfig {

    private final String apiKey;
    private final String apiUrl;

    private static final String PROPERTIES_FILE = "application.properties";

    public WeatherConfig() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (is == null) {
                throw new RuntimeException("找不到配置文件: " + PROPERTIES_FILE);
            }
            props.load(is);

            this.apiKey = props.getProperty("weather.api.key");
            this.apiUrl = props.getProperty("weather.api.url");

            if (apiKey == null || apiKey.isEmpty() || "YOUR_API_KEY_HERE".equals(apiKey)) {
                throw new RuntimeException("请在 application.properties 中配置 weather.api.key");
            }
            if (apiUrl == null || apiUrl.isEmpty()) {
                throw new RuntimeException("请在 application.properties 中配置 weather.api.url");
            }
        } catch (IOException e) {
            throw new RuntimeException("读取配置文件失败", e);
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }
}
