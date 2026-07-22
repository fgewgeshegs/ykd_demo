package com.youkeda.exercise.claw.agent.tool;

import com.youkeda.exercise.claw.agent.classify.Intent;
import com.youkeda.exercise.claw.agent.memory.ContextStore;
import com.youkeda.exercise.claw.ai.voice.VoiceService;
import com.youkeda.exercise.claw.ai.voice.VoiceService.VoiceIntentResult;
import com.youkeda.exercise.claw.ai.voice.VoiceService.VoiceSynthesisResult;
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
 * 4. 有意义→按意图路由到对应 Tool（CHAT→ChatTool，IMAGE_GENERATE→ImageGenerationTool）
 * 5. 根据需要 TTS 语音回复
 */
@Slf4j
@Component
public class VoiceTool implements WechatMessageHandler {

    private static final String MEANINGLESS_REPLY = "抱歉，我没听清，请再说一遍。";
    private static final String ASR_FAILED_REPLY = "抱歉，语音识别失败，请重试。";
    private static final String FALLBACK_REPLY = "抱歉，处理语音消息时出错，请稍后再试。";

    private final VoiceService voiceService;
    private final WechatILinkClient wechatClient;
    private final ChatTool chatTool;
    private final ImageGenerationTool imageGenerationTool;
    private final ContextStore contextStore;

    public VoiceTool(VoiceService voiceService,
                     WechatILinkClient wechatClient,
                     ChatTool chatTool,
                     ImageGenerationTool imageGenerationTool,
                     ContextStore contextStore) {
        this.voiceService = voiceService;
        this.wechatClient = wechatClient;
        this.chatTool = chatTool;
        this.imageGenerationTool = imageGenerationTool;
        this.contextStore = contextStore;
    }

    @Override
    public WechatReply handle(WechatMessage message) {
        if (message.getType() != MessageType.VOICE) {
            return null;
        }

        log.info("VoiceTool 处理语音消息 | from={}", message.getUserId());

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

        // ==================== 3. 按意图路由 ====================
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
     * 根据意图路由到对应 Tool
     */
    private WechatReply routeByIntent(Intent intent, String voiceText, WechatMessage originalMessage) {
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

    private WechatReply handleImageGenerate(WechatMessage textMessage) {
        try {
            WechatReply reply = imageGenerationTool.handle(textMessage);
            if (reply != null && reply.hasContent()) {
                return reply;
            }
        } catch (Exception e) {
            log.error("ImageGenerationTool 异常", e);
        }
        return WechatReply.text(FALLBACK_REPLY);
    }

    private WechatReply handleChatWithTts(WechatMessage textMessage, boolean ttsEnabled) {
        WechatReply textReply;
        try {
            textReply = chatTool.handle(textMessage);
        } catch (Exception e) {
            log.error("ChatTool 异常", e);
            return WechatReply.text(FALLBACK_REPLY);
        }

        if (textReply == null || !textReply.hasContent()) {
            return WechatReply.text(FALLBACK_REPLY);
        }

        if (!ttsEnabled || textReply.getType() != MessageType.TEXT) {
            return textReply;
        }

        String replyText = textReply.getText();
        VoiceSynthesisResult ttsResult = voiceService.synthesize(replyText);
        if (ttsResult != null) {
            log.info("TTS 合成成功，返回语音文件回复 | playtime={}ms | size={}bytes",
                    ttsResult.getPlaytimeMs(), ttsResult.getAudioBytes().length);
            contextStore.append(textMessage.getUserId(), "assistant",
                    "[本条回复以MP3文件形式发送]");
            return WechatReply.file(ttsResult.getAudioBytes(), "AI语音回复.mp3", replyText);
        }

        log.warn("TTS 合成失败，降级为文本回复");
        return textReply;
    }

    public WechatReply handleTextWithFileReply(WechatMessage message) {
        if (message.getType() != MessageType.TEXT) {
            return null;
        }

        log.info("VoiceTool 文字→语音文件回复 | from={} | text={}", message.getUserId(), message.getText());

        WechatReply textReply;
        try {
            textReply = chatTool.handle(message);
        } catch (Exception e) {
            log.error("ChatTool 异常", e);
            return WechatReply.text(FALLBACK_REPLY);
        }

        if (textReply == null || !textReply.hasContent()) {
            return WechatReply.text(FALLBACK_REPLY);
        }
        if (textReply.getType() != MessageType.TEXT) {
            return textReply;
        }

        String replyText = textReply.getText();
        try {
            VoiceSynthesisResult result = voiceService.synthesize(replyText);
            if (result != null && result.getAudioBytes() != null && result.getAudioBytes().length > 0) {
                log.info("TTS 合成成功，返回语音文件回复 | playtime={}ms | size={}bytes",
                        result.getPlaytimeMs(), result.getAudioBytes().length);
                contextStore.append(message.getUserId(), "assistant",
                        "[本条回复以MP3文件形式发送]");
                return WechatReply.file(result.getAudioBytes(), "AI语音回复.mp3", null);
            }
        } catch (Exception e) {
            log.error("TTS 合成异常", e);
        }

        log.warn("TTS 合成失败，降级为文本回复");
        return textReply;
    }

    private WechatMessage buildTextMessage(String text, WechatMessage original) {
        WechatMessage msg = new WechatMessage();
        msg.setType(MessageType.TEXT);
        msg.setText(text);
        msg.setUserId(original.getUserId());
        msg.setContextToken(original.getContextToken());
        return msg;
    }
}
