package com.youkeda.exercise.claw.wechat.handler;

import com.youkeda.exercise.claw.ai.classifier.Intent;
import com.youkeda.exercise.claw.ai.voice.VoiceService;
import com.youkeda.exercise.claw.ai.voice.VoiceService.VoiceIntentResult;
import com.youkeda.exercise.claw.ai.voice.VoiceService.VoiceSynthesisResult;
import com.youkeda.exercise.claw.context.ContextStore;
import com.youkeda.exercise.claw.wechat.client.WechatILinkClient;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 语音消息处理器
 *
 * 职责：处理 VOICE 类型的微信语音消息
 * 流程：
 * 1. 获取 ASR 文本（优先使用微信服务端识别结果，失败则下载音频用 Paraformer）
 * 2. 调用 LLM 一次完成"有意义判断 + 意图分类"
 * 3. 无意义→回复"没听清"
 * 4. 有意义→按意图路由到对应 Handler（CHAT/VOICE_REPLY→AIChatHandler，IMAGE_GENERATE→ImageGenerationHandler）
 * 5. 根据需要 TTS 语音回复
 */
@Slf4j
@Component
public class VoiceHandler implements MessageHandler {

    private static final String MEANINGLESS_REPLY = "抱歉，我没听清，请再说一遍。";
    private static final String ASR_FAILED_REPLY = "抱歉，语音识别失败，请重试。";
    private static final String FALLBACK_REPLY = "抱歉，处理语音消息时出错，请稍后再试。";

    private final VoiceService voiceService;
    private final WechatILinkClient wechatClient;
    private final AIChatHandler chatHandler;
    private final ImageGenerationHandler imageGenerationHandler;
    private final ContextStore contextStore;

    public VoiceHandler(VoiceService voiceService,
                         WechatILinkClient wechatClient,
                         AIChatHandler chatHandler,
                         ImageGenerationHandler imageGenerationHandler,
                         ContextStore contextStore) {
        this.voiceService = voiceService;
        this.wechatClient = wechatClient;
        this.chatHandler = chatHandler;
        this.imageGenerationHandler = imageGenerationHandler;
        this.contextStore = contextStore;
    }

    @Override
    public WechatReply handle(WechatMessage message) {
        if (message.getType() != MessageType.VOICE) {
            return null;
        }

        log.info("VoiceHandler 处理语音消息 | from={}", message.getUserId());

        // ==================== 1. 获取 ASR 文本 ====================
        String voiceText = getVoiceText(message);
        if (voiceText == null) {
            return WechatReply.text(ASR_FAILED_REPLY);
        }

        // ==================== 2. 分类：有意义判断 + 意图 ====================
        VoiceIntentResult result = voiceService.classifyVoiceIntent(voiceText);
        if (!result.isMeaningful()) {
            log.info("语音无实际含义 | text={}", voiceText);
            return WechatReply.text(MEANINGLESS_REPLY);
        }

        log.info("语音意图分类 | text={} | intent={}", voiceText, result.getIntent());

        // ==================== 4. 按意图路由 ====================
        return routeByIntent(result.getIntent(), voiceText, message);
    }

    /**
     * 获取 ASR 文本：先使用微信服务端识别结果，失败则下载音频用 Paraformer
     */
    private String getVoiceText(WechatMessage message) {
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
     * 根据意图路由到对应 Handler
     */
    private WechatReply routeByIntent(Intent intent, String voiceText, WechatMessage originalMessage) {
        // 构建临时 TEXT 消息供下游 Handler 使用
        WechatMessage textMessage = buildTextMessage(voiceText, originalMessage);

        return switch (intent) {
            case IMAGE_GENERATE -> handleImageGenerate(textMessage);
            case VOICE_REPLY -> handleTextWithFileReply(textMessage);
            case CHAT -> handleChatWithTts(textMessage, voiceService.isTtsEnabled());
            default -> {
                log.warn("未知意图: {}，走兜底", intent);
                yield WechatReply.text(FALLBACK_REPLY);
            }
        };
    }

    /**
     * 处理 IMAGE_GENERATE 意图：调用 ImageGenerationHandler，返回图片或文字兜底
     */
    private WechatReply handleImageGenerate(WechatMessage textMessage) {
        try {
            WechatReply reply = imageGenerationHandler.handle(textMessage);
            if (reply != null && reply.hasContent()) {
                return reply;
            }
        } catch (Exception e) {
            log.error("ImageGenerationHandler 异常", e);
        }
        return WechatReply.text(FALLBACK_REPLY);
    }

    /**
     * 处理 CHAT/VOICE_REPLY 意图：调用 AIChatHandler，可选 TTS
     *
     * @param textMessage  临时文本消息
     * @param ttsEnabled   是否启用 TTS 语音回复
     */
    private WechatReply handleChatWithTts(WechatMessage textMessage, boolean ttsEnabled) {
        // 1. 获取文字回复
        WechatReply textReply;
        try {
            textReply = chatHandler.handle(textMessage);
        } catch (Exception e) {
            log.error("AIChatHandler 异常", e);
            return WechatReply.text(FALLBACK_REPLY);
        }

        if (textReply == null || !textReply.hasContent()) {
            return WechatReply.text(FALLBACK_REPLY);
        }

        // 2. 如果不启用 TTS 或回复不是 TEXT 类型，直接返回
        if (!ttsEnabled || textReply.getType() != MessageType.TEXT) {
            return textReply;
        }

        // 3. TTS 合成语音
        String replyText = textReply.getText();
        VoiceSynthesisResult ttsResult = voiceService.synthesize(replyText);
        if (ttsResult != null) {
            log.info("TTS 合成成功，返回语音回复 | playtime={}ms | sampleRate={}Hz",
                    ttsResult.getPlaytimeMs(), ttsResult.getSampleRate());
            contextStore.append(textMessage.getUserId(), "assistant",
                    "[本条回复以微信语音形式发送]", null, null,
                    ttsResult.getAudioUrl());
            return WechatReply.voice(ttsResult.getAudioBytes(),
                    ttsResult.getPlaytimeMs(), ttsResult.getEncodeType(),
                    ttsResult.getSampleRate(), replyText);
        }

        // TTS 失败，降级为文本回复
        log.warn("TTS 合成失败，降级为文本回复");
        return textReply;
    }

    /**
     * 处理文本消息的语音文件回复（针对 TEXT + VOICE_REPLY 意图的场景）
     *
     * 流程：
     * 1. 调用 AIChatHandler 获取文字回复
     * 2. TTS 合成为 MP3 音频
     * 3. 以 FILE 类型（MP3 文件）返回，而非气泡形式
     * 4. TTS 失败则降级为文字回复
     *
     * @param message 文本消息（TEXT 类型）
     * @return FILE 类型的 WechatReply 或 TEXT 降级
     */
    public WechatReply handleTextWithFileReply(WechatMessage message) {
        if (message.getType() != MessageType.TEXT) {
            return null;
        }

        log.info("VoiceHandler 文字→语音文件回复 | from={} | text={}", message.getUserId(), message.getText());

        // 1. 获取文字回复
        WechatReply textReply;
        try {
            textReply = chatHandler.handle(message);
        } catch (Exception e) {
            log.error("AIChatHandler 异常", e);
            return WechatReply.text(FALLBACK_REPLY);
        }

        if (textReply == null || !textReply.hasContent()) {
            return WechatReply.text(FALLBACK_REPLY);
        }
        if (textReply.getType() != MessageType.TEXT) {
            return textReply;
        }

        // 2. TTS 合成语音
        String replyText = textReply.getText();
        try {
            VoiceSynthesisResult result = voiceService.synthesize(replyText);
            if (result != null && result.getAudioBytes() != null && result.getAudioBytes().length > 0) {
                log.info("TTS 合成成功，返回语音文件回复 | playtime={}ms | size={}bytes",
                        result.getPlaytimeMs(), result.getAudioBytes().length);
                contextStore.append(message.getUserId(), "assistant",
                        "[本条回复以MP3文件形式发送]", null, null,
                        result.getAudioUrl());
                return WechatReply.file(result.getAudioBytes(), "AI语音回复.mp3", null);
            }
        } catch (Exception e) {
            log.error("TTS 合成异常", e);
        }

        // 3. TTS 失败，降级为文本
        log.warn("TTS 合成失败，降级为文本回复");
        return textReply;
    }

    /**
     * 构建临时 TEXT 消息供下游 Handler 使用
     */
    private WechatMessage buildTextMessage(String text, WechatMessage original) {
        WechatMessage msg = new WechatMessage();
        msg.setType(MessageType.TEXT);
        msg.setText(text);
        msg.setUserId(original.getUserId());
        msg.setContextToken(original.getContextToken());
        return msg;
    }
}
