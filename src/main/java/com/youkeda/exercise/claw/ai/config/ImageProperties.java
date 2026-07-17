package com.youkeda.exercise.claw.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 图片生成模型配置属性
 *
 * 从 application.properties 读取 image.* 前缀的配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "image")
public class ImageProperties {

    /**
     * API 密钥（可复用 llm 或 vision 的密钥，也可独立配置）
     */
    private String apiKey;

    /**
     * API 基础地址（默认阿里云 DashScope）
     */
    private String baseUrl = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis";

    /**
     * 图片生成模型名称
     */
    private String model = "wanx-v1";
}