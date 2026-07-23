package com.youkeda.exercise.claw.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.ai.voice.VoiceService;
import com.youkeda.exercise.claw.ai.voice.VoiceService.VoiceSynthesisResult;
import com.youkeda.exercise.claw.wechat.client.WechatILinkClient;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 语音函数
 *
 * <p>职责：
 * <ul>
 *   <li>ASR 语音识别（提取语音消息文本）</li>
 *   <li>TTS 语音合成（将文本合成为音频文件）</li>
 *   <li>作为 {@link LLMFunction} 提供 {@code text_to_speech} 工具供 LLM 调用</li>
 * </ul>
 *
 * <p>注意：{@link LLMFunction#execute(String)} 只能返回文本，但 TTS 产生的音频数据通过
 * {@link #consumePendingAudio()} 传递回调用方（{@code ChatTool}），确保语音文件能被正确发送。</p>
 */
@Component
public class VoiceFunction implements LLMFunction {

    private static final Logger log = LoggerFactory.getLogger(VoiceFunction.class);

    private static final String ASR_FAILED_REPLY = "抱歉，语音识别失败，请重试。";

    private final VoiceService voiceService;
    private final WechatILinkClient wechatClient;
    private final LLMFunctionRegistry functionRegistry;
    private final ObjectMapper objectMapper;

    /** 待发送的音频数据（单线程 WeChat 轮询，一次只处理一条消息，用实例字段足够） */
    private volatile PendingAudio pendingAudio;

    public VoiceFunction(VoiceService voiceService,
                      WechatILinkClient wechatClient,
                      LLMFunctionRegistry functionRegistry,
                      ObjectMapper objectMapper) {
        this.voiceService = voiceService;
        this.wechatClient = wechatClient;
        this.functionRegistry = functionRegistry;
        this.objectMapper = objectMapper;
    }

    /**
     * 消费待发送的音频数据
     * <p>被 {@code ChatTool} 在工具调用循环结束后调用，如果存在则发送语音文件而非纯文本。</p>
     *
     * @return 待发送的音频数据，没有则返回 null
     */
    public PendingAudio consumePendingAudio() {
        PendingAudio audio = pendingAudio;
        pendingAudio = null;
        return audio;
    }

    /** TTS 结果暂存：音频文件 + 原始文本 */
    public record PendingAudio(byte[] audioBytes, String text) {}

    @PostConstruct
    public void init() {
        functionRegistry.register(this);
        log.info("VoiceFunction 已注册到 LLMFunctionRegistry（text_to_speech）");
    }

    // ==================== LLMFunction（text_to_speech） ====================

    @Override
    public String getName() {
        return "text_to_speech";
    }

    @Override
    public String getDescription() {
        return "将文本合成为语音音频文件。当用户说「用语音回复」「读给我听」「说给我听」时调用此工具，也适合语音输入场景下对回复内容进行语音播报";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("type", "object");

        ObjectNode properties = params.putObject("properties");
        ObjectNode text = properties.putObject("text");
        text.put("type", "string");
        text.put("description", "需要合成语音的文本内容");

        params.putArray("required").add("text");

        return params;
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            JsonNode textNode = args.get("text");
            if (textNode == null) {
                return "{\"error\": \"缺少必填参数: text\"}";
            }

            String text = textNode.asText();
            log.info("text_to_speech 执行 | text={}", text);

            VoiceSynthesisResult result = voiceService.synthesize(text);
            if (result == null) {
                return "{\"error\": \"语音合成失败\"}";
            }

            log.info("语音合成成功 | size={}bytes | playtime={}ms",
                    result.getAudioBytes().length, result.getPlaytimeMs());

            // 暂存音频供 ChatTool 取走发送（execute() 只能返回文本，音频通过此通道传递）
            pendingAudio = new PendingAudio(result.getAudioBytes(), text);

            return "{\"success\": true, \"playtimeMs\": " + result.getPlaytimeMs()
                    + ", \"size\": " + result.getAudioBytes().length + "}";

        } catch (Exception e) {
            log.error("text_to_speech 执行失败 | args={} | error={}", argumentsJson, e.getMessage());
            return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    // ==================== 公共工具方法 ====================

    /**
     * 从语音消息中提取 ASR 文本
     *
     * <p>优先使用微信服务端 ASR 结果，失败则下载音频调用 Paraformer。
     *
     * @param message 微信语音消息
     * @return ASR 文本，提取失败返回 null
     */
    public String extractText(WechatMessage message) {
        if (message.getType() != MessageType.VOICE) {
            return null;
        }

        // 优先使用微信服务端 ASR 结果
        if (message.getVoiceText() != null && !message.getVoiceText().trim().isEmpty()) {
            log.info("使用微信服务端 ASR | text={}", message.getVoiceText());
            return message.getVoiceText().trim();
        }

        // 降级：下载音频调用 Paraformer
        log.info("微信服务端 ASR 为空，下载音频调用 Paraformer");
        byte[] audioBytes = wechatClient.downloadMedia(
                message.getVoiceEncryptQueryParam(),
                message.getVoiceAesKey()
        );

        if (audioBytes == null || audioBytes.length == 0) {
            log.warn("音频下载失败");
            return null;
        }

        int encodeType = message.getEncodeType() != null ? message.getEncodeType() : 0;
        String text = voiceService.transcribe(audioBytes, encodeType);

        if (text != null) {
            log.info("Paraformer ASR 成功 | text={}", text);
        } else {
            log.warn("Paraformer ASR 失败");
        }
        return text;
    }

    /**
     * 将文本合成为语音并返回文件类型的回复
     *
     * @param text 待合成的文本
     * @return 语音文件回复，合成失败返回 null
     */
    public WechatReply synthesizeTextToFile(String text) {
        try {
            VoiceSynthesisResult result = voiceService.synthesize(text);
            if (result != null && result.getAudioBytes() != null && result.getAudioBytes().length > 0) {
                log.info("TTS 合成成功 | playtime={}ms | size={}bytes",
                        result.getPlaytimeMs(), result.getAudioBytes().length);
                return WechatReply.file(result.getAudioBytes(), "AI语音回复.mp3", text);
            }
        } catch (Exception e) {
            log.error("TTS 合成异常", e);
        }
        log.warn("TTS 合成失败");
        return null;
    }
}
