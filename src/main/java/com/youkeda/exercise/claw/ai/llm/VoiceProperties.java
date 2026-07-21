package com.youkeda.exercise.claw.ai.llm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 语音（ASR + TTS）模型配置属性
 *
 * 从 application.properties 读取 voice.* 前缀的配置
 * ASR 使用 DashScope Paraformer，TTS 使用 DashScope CosyVoice
 */
@Data
@Component
@ConfigurationProperties(prefix = "voice")
public class VoiceProperties {

    /**
     * API 密钥（与 vision/image 共用 DashScope 密钥）
     */
    private String apiKey;

    /**
     * ASR（语音识别）API 基础地址（默认阿里云 DashScope）
     */
    private String asrBaseUrl = "https://dashscope.aliyuncs.com/api/v1/services/audio/transcription/transcription";

    /**
     * ASR 模型名称
     */
    private String asrModel = "paraformer-v2";

    /**
     * TTS（语音合成）API 基础地址
     */
    private String ttsBaseUrl = "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";

    /**
     * TTS 模型名称（Qwen-TTS，与生图共用 multimodal-generation 端点）
     */
    private String ttsModel = "qwen3-tts-flash";

    /**
     * TTS 默认音色（Qwen-TTS 内置音色：Cherry、Pony、Harry 等）
     */
    private String ttsVoice = "Cherry";

    /**
     * 是否启用 TTS 语音回复（关闭时语音消息以文本回复）
     */
    private boolean ttsEnabled = true;
}
