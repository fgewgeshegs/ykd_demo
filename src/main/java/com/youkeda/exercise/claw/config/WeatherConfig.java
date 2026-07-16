package com.youkeda.exercise.claw.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 天气 API 配置
 * 从 application.properties 读取天气服务配置
 */
@Slf4j
@Component
public class WeatherConfig {

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.url}")
    private String apiUrl;

    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isEmpty() || "YOUR_API_KEY_HERE".equals(apiKey)) {
            log.warn("weather.api.key 未配置或为默认值，天气功能将不可用");
        }
        if (apiUrl == null || apiUrl.isEmpty()) {
            log.warn("weather.api.url 未配置，天气功能将不可用");
        }
        log.info("天气配置加载完成");
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }
}
