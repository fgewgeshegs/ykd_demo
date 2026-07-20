package com.youkeda.exercise.claw.ai.llm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 视觉模型配置属性
 *
 * 从 application.properties 读取 vision.* 前缀的配置
 * 与文本 LLM 配置独立，支持不同的模型提供商
 */
@Data
@Component
@ConfigurationProperties(prefix = "vision")
public class VisionProperties {

    /**
     * API 密钥
     */
    private String apiKey;

    /**
     * API 基础地址，默认硅基流动
     */
    private String baseUrl = "https://api.siliconflow.com/v1";

    /**
     * 视觉模型名称
     */
    private String model = "Qwen/Qwen3-VL-32B-Instruct";
}
