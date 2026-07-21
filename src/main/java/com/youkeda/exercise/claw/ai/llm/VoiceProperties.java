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
    private String ttsBaseUrl = "https://{WorkspaceId}.cn-beijing.maas.aliyuncs.com/api/v1/services/audio/tts/SpeechSynthesizer";

    /**
     * TTS 模型名称
     */
    private String ttsModel = "cosyvoice-v2";

    /**
     * TTS 默认音色（cosyvoice-v2 支持：longxiaochun_v2 等）
     */
    private String ttsVoice = "longxiaochun_v2";

    /**
     * 是否启用 TTS 语音回复（关闭时语音消息以文本回复）
     */
    private boolean ttsEnabled = true;
}
