package com.youkeda.exercise.claw.wechat;

import com.youkeda.exercise.claw.ai.classifier.Intent;
import com.youkeda.exercise.claw.ai.classifier.IntentClassifier;
import com.youkeda.exercise.claw.wechat.handler.AIChatHandler;
import com.youkeda.exercise.claw.wechat.handler.ImageGenerationHandler;
import com.youkeda.exercise.claw.wechat.handler.MessageHandler;
import com.youkeda.exercise.claw.wechat.handler.SimpleReplyHandler;
import com.youkeda.exercise.claw.wechat.handler.VisionHandler;
import com.youkeda.exercise.claw.wechat.handler.VoiceHandler;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 消息路由器
 *
 * 职责：
 * 1. 接收消息
 * 2. 调用 IntentClassifier 判断意图
 * 3. 根据 Intent 选择对应的 Handler 处理
 *
 * 不包含业务逻辑，仅负责路由分发
 */
@Slf4j
@Component
public class MessageRouter {

    private final IntentClassifier intentClassifier;
    private final AIChatHandler chatHandler;
    private final VisionHandler visionHandler;
    private final ImageGenerationHandler imageGenerationHandler;
    private final SimpleReplyHandler fallbackHandler;
    private final VoiceHandler voiceHandler;

    public MessageRouter(IntentClassifier intentClassifier,
                         AIChatHandler chatHandler,
                         VisionHandler visionHandler,
                         ImageGenerationHandler imageGenerationHandler,
                         SimpleReplyHandler fallbackHandler,
                         VoiceHandler voiceHandler) {
        this.intentClassifier = intentClassifier;
        this.chatHandler = chatHandler;
        this.visionHandler = visionHandler;
        this.imageGenerationHandler = imageGenerationHandler;
        this.fallbackHandler = fallbackHandler;
        this.voiceHandler = voiceHandler;
    }

    /**
     * 路由消息到对应的处理器
     *
     * @param message 微信消息
     * @return 回复内容（WechatReply，包含 TEXT 或 IMAGE 类型）
     */
    public WechatReply route(WechatMessage message) {
        // 图片消息：直接走 VisionHandler（保留已有图片处理流程）
        if (message.getType() == MessageType.IMAGE) {
            log.info("路由：图片消息 → VisionHandler | from={}", message.getUserId());
            WechatReply reply = visionHandler.handle(message);
            return fallbackIfEmpty(reply, message);
        }

        // 语音消息：直接走 VoiceHandler
        if (message.getType() == MessageType.VOICE) {
            log.info("路由：语音消息 → VoiceHandler | from={}", message.getUserId());
            WechatReply reply = voiceHandler.handle(message);
            return fallbackIfEmpty(reply, message);
        }

        // 文本消息：先识别意图，再按意图路由
        if (message.getType() == MessageType.TEXT) {
            Intent intent = intentClassifier.classify(message.getText());
            log.info("路由：文本消息 intent={} | from={}", intent, message.getUserId());

            // VOICE_REPLY 意图：走语音文件回复路径（文字对话 + TTS → MP3 文件）
            if (intent == Intent.VOICE_REPLY) {
                log.info("路由：VOICE_REPLY 意图 → VoiceHandler.handleTextWithFileReply | from={}", message.getUserId());
                WechatReply reply = voiceHandler.handleTextWithFileReply(message);
                return fallbackIfEmpty(reply, message);
            }

            MessageHandler targetHandler = selectHandler(intent);
            WechatReply reply = targetHandler.handle(message);
            return fallbackIfEmpty(reply, message);
        }

        // 其他类型：兜底
        log.info("路由：未知消息类型 type={} | from={}", message.getType(), message.getUserId());
        return fallbackHandler.handle(message);
    }

    /**
     * 根据意图选择对应的 Handler
     */
    private MessageHandler selectHandler(Intent intent) {
        return switch (intent) {
            case CHAT -> chatHandler;
            case IMAGE_GENERATE -> imageGenerationHandler;
            case IMAGE_ANALYZE -> visionHandler;
            default -> fallbackHandler;
        };
    }

    /**
     * 如果 Handler 返回空或没有内容，使用兜底回复
     */
    private WechatReply fallbackIfEmpty(WechatReply reply, WechatMessage message) {
        if (reply != null && reply.hasContent()) {
            return reply;
        }
        log.info("路由：Handler 返回空，使用兜底 | from={}", message.getUserId());
        return fallbackHandler.handle(message);
    }
}
