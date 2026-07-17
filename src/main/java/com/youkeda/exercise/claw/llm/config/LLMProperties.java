package com.youkeda.exercise.claw.llm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LLM 配置属性
 *
 * 从 application.properties 读取 llm.* 前缀的配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm")
public class LLMProperties {

    /**
     * API 密钥
     */
    private String apiKey;

    /**
     * API 基础地址
     */
    private String baseUrl = "https://api.deepseek.com";

    /**
     * 模型名称
     */
    private String model = "deepseek-chat";
}
