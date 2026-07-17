package com.youkeda.exercise.claw.router;

import com.youkeda.exercise.claw.intent.Intent;
import com.youkeda.exercise.claw.intent.IntentClassifier;
import com.youkeda.exercise.claw.intent.IntentResult;
import com.youkeda.exercise.claw.wechat.handler.AIChatHandler;
import com.youkeda.exercise.claw.wechat.handler.ImageGenerationHandler;
import com.youkeda.exercise.claw.wechat.handler.MessageHandler;
import com.youkeda.exercise.claw.wechat.handler.SimpleReplyHandler;
import com.youkeda.exercise.claw.wechat.handler.VisionHandler;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
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

    public MessageRouter(IntentClassifier intentClassifier,
                         AIChatHandler chatHandler,
                         VisionHandler visionHandler,
                         ImageGenerationHandler imageGenerationHandler,
                         SimpleReplyHandler fallbackHandler) {
        this.intentClassifier = intentClassifier;
        this.chatHandler = chatHandler;
        this.visionHandler = visionHandler;
        this.imageGenerationHandler = imageGenerationHandler;
        this.fallbackHandler = fallbackHandler;
    }

    /**
     * 路由消息到对应的处理器
     *
     * @param message 微信消息
     * @return 回复内容
     */
    public String route(WechatMessage message) {
        // 图片消息：直接走 VisionHandler（保留已有图片处理流程）
        if (message.getType() == MessageType.IMAGE) {
            log.info("路由：图片消息 → VisionHandler | from={}", message.getUserId());
            String reply = visionHandler.handle(message);
            return fallbackIfEmpty(reply, message);
        }

        // 文本消息：先识别意图，再按意图路由
        if (message.getType() == MessageType.TEXT) {
            IntentResult result = intentClassifier.classify(message.getText());
            Intent intent = result.getIntent();
            log.info("路由：文本消息 intent={} | from={}", intent, message.getUserId());

            MessageHandler targetHandler = selectHandler(intent);
            String reply = targetHandler.handle(message);
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
        };
    }

    /**
     * 如果 Handler 返回空，使用兜底回复
     */
    private String fallbackIfEmpty(String reply, WechatMessage message) {
        if (reply != null && !reply.isEmpty()) {
            return reply;
        }
        log.info("路由：Handler 返回空，使用兜底 | from={}", message.getUserId());
        return fallbackHandler.handle(message);
    }
}