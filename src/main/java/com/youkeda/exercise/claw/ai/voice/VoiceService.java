package com.youkeda.exercise.claw.ai.voice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.classify.Intent;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.ai.llm.VoiceClient;
import com.youkeda.exercise.claw.ai.llm.VoiceProperties;
import com.youkeda.exercise.claw.common.PromptLoader;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 语音服务
 *
 * 封装语音业务的逻辑：
 * - ASR（语音识别）：调用 VoiceClient 将音频转为文字，含重试
 * - TTS（语音合成）：调用 VoiceClient 将文字转为语音，含重试
 * - 语音意图分类：调用 LLM 一次完成"有意义判断 + 意图分类"
 */
@Service
public class VoiceService {

    private static final Logger log = LoggerFactory.getLogger(VoiceService.class);

    private static final int MAX_RETRIES = 3;
    private static final String VOICE_PROMPT_PATH = "prompts/voice-system-prompt.txt";
    private static final String DEFAULT_VOICE_PROMPT =
            "你是一个语音消息分类器。请判断用户语音是否有实际含义，如果有请返回意图。";

    private final VoiceClient voiceClient;
    private final LLMClient llmClient;
    private final VoiceProperties voiceProperties;
    private final ObjectMapper objectMapper;
    private final PromptLoader promptLoader;

    private String voiceIntentPrompt;

    public VoiceService(VoiceClient voiceClient,
                         LLMClient llmClient,
                         VoiceProperties voiceProperties,
                         ObjectMapper objectMapper,
                         PromptLoader promptLoader) {
        this.voiceClient = voiceClient;
        this.llmClient = llmClient;
        this.voiceProperties = voiceProperties;
        this.objectMapper = objectMapper;
        this.promptLoader = promptLoader;
    }

    @PostConstruct
    public void init() {
        this.voiceIntentPrompt = promptLoader.load(VOICE_PROMPT_PATH, DEFAULT_VOICE_PROMPT);
    }

    /**
     * 语音识别：音频转文字（含重试）
     *
     * @param audioBytes 音频字节数据
     * @param encodeType 音频编码类型
     * @return 识别文本，失败时返回 null
     */
    public String transcribe(byte[] audioBytes, int encodeType) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                String text = voiceClient.asr(audioBytes, encodeType);
                if (text != null && !text.trim().isEmpty()) {
                    log.info("ASR 成功 | text={}", text);
                    return text.trim();
                }
                log.warn("ASR 返回空文本");
                return null;
            } catch (VoiceClientException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    log.error("ASR 失败，已达最大重试次数 {} | errorCode={}",
                            MAX_RETRIES, e.getErrorCode());
                    return null;
                }
                log.warn("ASR 失败 (第{}/{}) | errorCode={}，{}ms 后重试",
                        attempt, MAX_RETRIES, e.getErrorCode(), 2000L * attempt);
                try {
                    Thread.sleep(2000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            } catch (Exception e) {
                log.error("ASR 异常", e);
                return null;
            }
        }
        return null;
    }

    /**
     * 语音合成：文字转语音（含重试）
     *
     * @param text 待合成的文字
     * @return 语音合成结果（含音频字节和时长），失败时返回 null
     */
    public VoiceSynthesisResult synthesize(String text) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                byte[] audioBytes = voiceClient.tts(text);
                if (audioBytes != null && audioBytes.length > 0) {
                    int playtimeMs = voiceClient.parsePlaytime(audioBytes, text);
                    int sampleRate = voiceClient.parseSampleRate(audioBytes);
                    String audioUrl = voiceClient.getLastTtsUrl();
                    log.info("TTS 成功 | text={} | size={} | playtime={}ms | sampleRate={}Hz | url={}",
                            text, audioBytes.length, playtimeMs, sampleRate, audioUrl);
                    return new VoiceSynthesisResult(audioBytes, playtimeMs, 4, sampleRate, audioUrl);
                }
                log.warn("TTS 返回空音频");
                return null;
            } catch (VoiceClientException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    log.error("TTS 失败，已达最大重试次数 {} | errorCode={}",
                            MAX_RETRIES, e.getErrorCode());
                    return null;
                }
                log.warn("TTS 失败 (第{}/{}) | errorCode={}，{}ms 后重试",
                        attempt, MAX_RETRIES, e.getErrorCode(), 2000L * attempt);
                try {
                    Thread.sleep(2000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            } catch (Exception e) {
                log.error("TTS 异常", e);
                return null;
            }
        }
        return null;
    }

    /**
     * 语音意图分类：一次 LLM 调用完成"有意义判断 + 意图分类"
     *
     * @param voiceText ASR 后的语音文本
     * @return 分类结果（是否有含义、意图）
     */
    public VoiceIntentResult classifyVoiceIntent(String voiceText) {
        try {
            String rawResult = llmClient.chatWithSystemPrompt(voiceIntentPrompt, voiceText);
            if (rawResult == null || rawResult.trim().isEmpty()) {
                log.warn("意图分类 LLM 返回空，默认有意义+CHAT");
                return new VoiceIntentResult(true, Intent.CHAT);
            }
            return parseVoiceIntentResult(rawResult.trim());
        } catch (Exception e) {
            log.error("意图分类异常，默认有意义+CHAT | error={}", e.getMessage());
            return new VoiceIntentResult(true, Intent.CHAT);
        }
    }

    /**
     * 是否启用 TTS
     */
    public boolean isTtsEnabled() {
        return voiceProperties.isTtsEnabled();
    }

    /**
     * 解析 LLM 返回的 JSON 结果
     */
    private VoiceIntentResult parseVoiceIntentResult(String json) {
        try {
            // 从回复中提取 JSON 对象
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start == -1 || end == -1) {
                log.warn("意图分类响应无 JSON，默认有意义+CHAT | response={}", json);
                return new VoiceIntentResult(true, Intent.CHAT);
            }

            String jsonStr = json.substring(start, end + 1);
            JsonNode root = objectMapper.readTree(jsonStr);

            // 判断是否有含义
            JsonNode meaningfulNode = root.get("meaningful");
            boolean meaningful = meaningfulNode != null && meaningfulNode.asBoolean();

            if (!meaningful) {
                return new VoiceIntentResult(false, null);
            }

            // 解析意图
            JsonNode intentNode = root.get("intent");
            if (intentNode != null && !intentNode.isNull()) {
                String intentStr = intentNode.asText();
                try {
                    Intent intent = Intent.valueOf(intentStr);
                    return new VoiceIntentResult(true, intent);
                } catch (IllegalArgumentException e) {
                    log.warn("未知意图: {}，默认 CHAT", intentStr);
                    return new VoiceIntentResult(true, Intent.CHAT);
                }
            }

            return new VoiceIntentResult(true, Intent.CHAT);

        } catch (Exception e) {
            log.warn("意图分类解析失败，默认有意义+CHAT | json={} | error={}", json, e.getMessage());
            return new VoiceIntentResult(true, Intent.CHAT);
        }
    }

    /**
     * 语音意图分类结果
     */
    public static class VoiceIntentResult {
        /** 是否有实际含义 */
        private boolean meaningful;
        /** 意图（仅在 meaningful 为 true 时有意义） */
        private Intent intent;

        public VoiceIntentResult(boolean meaningful, Intent intent) {
            this.meaningful = meaningful;
            this.intent = intent;
        }

        public boolean isMeaningful() {
            return meaningful;
        }

        public void setMeaningful(boolean meaningful) {
            this.meaningful = meaningful;
        }

        public Intent getIntent() {
            return intent;
        }

        public void setIntent(Intent intent) {
            this.intent = intent;
        }
    }

    /**
     * 语音合成结果
     */
    public static class VoiceSynthesisResult {
        /** 合成音频字节数据 */
        private byte[] audioBytes;
        /** 音频时长（毫秒） */
        private int playtimeMs;
        /** 音频编码类型 */
        private int encodeType;
        /** 音频采样率（Hz） */
        private int sampleRate;
        /** 音频 URL（OSS 下载地址，可存上下文复用） */
        private String audioUrl;

        public VoiceSynthesisResult(byte[] audioBytes, int playtimeMs, int encodeType, int sampleRate, String audioUrl) {
            this.audioBytes = audioBytes;
            this.playtimeMs = playtimeMs;
            this.encodeType = encodeType;
            this.sampleRate = sampleRate;
            this.audioUrl = audioUrl;
        }

        public byte[] getAudioBytes() {
            return audioBytes;
        }

        public void setAudioBytes(byte[] audioBytes) {
            this.audioBytes = audioBytes;
        }

        public int getPlaytimeMs() {
            return playtimeMs;
        }

        public void setPlaytimeMs(int playtimeMs) {
            this.playtimeMs = playtimeMs;
        }

        public int getEncodeType() {
            return encodeType;
        }

        public void setEncodeType(int encodeType) {
            this.encodeType = encodeType;
        }

        public int getSampleRate() {
            return sampleRate;
        }

        public void setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
        }

        public String getAudioUrl() {
            return audioUrl;
        }

        public void setAudioUrl(String audioUrl) {
            this.audioUrl = audioUrl;
        }
    }
}
