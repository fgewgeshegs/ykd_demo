package com.youkeda.exercise.claw.wechat;

import com.youkeda.exercise.claw.agent.classify.Intent;
import com.youkeda.exercise.claw.agent.classify.IntentClassifier;
import com.youkeda.exercise.claw.agent.tool.ChatTool;
import com.youkeda.exercise.claw.agent.tool.FileGenerationTool;
import com.youkeda.exercise.claw.agent.tool.FileTool;
import com.youkeda.exercise.claw.agent.tool.ImageGenerationTool;
import com.youkeda.exercise.claw.agent.tool.SimpleReplyTool;
import com.youkeda.exercise.claw.agent.tool.VisionTool;
import com.youkeda.exercise.claw.agent.tool.VoiceTool;
import com.youkeda.exercise.claw.agent.tool.WechatMessageHandler;
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
    private final ChatTool chatTool;
    private final VisionTool visionTool;
    private final ImageGenerationTool imageGenerationTool;
    private final SimpleReplyTool fallbackTool;
    private final VoiceTool voiceTool;
    private final FileTool fileTool;
    private final FileGenerationTool fileGenerationTool;

    public MessageRouter(IntentClassifier intentClassifier,
                         ChatTool chatTool,
                         VisionTool visionTool,
                         ImageGenerationTool imageGenerationTool,
                         SimpleReplyTool fallbackTool,
                         VoiceTool voiceTool,
                         FileTool fileTool,
                         FileGenerationTool fileGenerationTool) {
        this.intentClassifier = intentClassifier;
        this.chatTool = chatTool;
        this.visionTool = visionTool;
        this.imageGenerationTool = imageGenerationTool;
        this.fallbackTool = fallbackTool;
        this.voiceTool = voiceTool;
        this.fileTool = fileTool;
        this.fileGenerationTool = fileGenerationTool;
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
            log.info("路由：图片消息 → VisionTool | from={}", message.getUserId());
            WechatReply reply = visionTool.handle(message);
            return fallbackIfEmpty(reply, message);
        }

        // 语音消息：直接走 VoiceTool
        if (message.getType() == MessageType.VOICE) {
            log.info("路由：语音消息 → VoiceTool | from={}", message.getUserId());
            WechatReply reply = voiceTool.handle(message);
            return fallbackIfEmpty(reply, message);
        }

        // 文件消息：直接走 FileTool（根据文件内容类型分发：图片→视觉模型，文档→文本提取+LLM）
        if (message.getType() == MessageType.FILE) {
            log.info("路由：文件消息 → FileTool | from={} | fileName={}", message.getUserId(), message.getFileName());
            WechatReply reply = fileTool.handle(message);
            return fallbackIfEmpty(reply, message);
        }

        // 文本消息：先识别意图，再按意图路由
        if (message.getType() == MessageType.TEXT) {
            Intent intent = intentClassifier.classify(message.getText());
            log.info("路由：文本消息 intent={} | from={}", intent, message.getUserId());

            // VOICE_REPLY 意图：走语音文件回复路径（文字对话 + TTS → MP3 文件）
            if (intent == Intent.VOICE_REPLY) {
                log.info("路由：VOICE_REPLY 意图 → VoiceTool.handleTextWithFileReply | from={}", message.getUserId());
                WechatReply reply = voiceTool.handleTextWithFileReply(message);
                return fallbackIfEmpty(reply, message);
            }

            WechatMessageHandler targetHandler = selectHandler(intent);
            WechatReply reply = targetHandler.handle(message);
            return fallbackIfEmpty(reply, message);
        }

        // 其他类型：兜底
        log.info("路由：未知消息类型 type={} | from={}", message.getType(), message.getUserId());
        return fallbackTool.handle(message);
    }

    /**
     * 根据意图选择对应的 Handler
     */
    private WechatMessageHandler selectHandler(Intent intent) {
        return switch (intent) {
            case CHAT -> chatTool;
            case IMAGE_GENERATE -> imageGenerationTool;
            case IMAGE_ANALYZE -> visionTool;
            case FILE_GENERATE -> fileGenerationTool;
            default -> fallbackTool;
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
        return fallbackTool.handle(message);
    }
}
