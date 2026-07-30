package com.youkeda.exercise.claw.wechat;

import com.youkeda.exercise.claw.agent.tool.ChatTool;
import com.youkeda.exercise.claw.agent.tool.FileTool;
import com.youkeda.exercise.claw.agent.tool.SimpleReplyTool;
import com.youkeda.exercise.claw.agent.tool.VisionTool;
import com.youkeda.exercise.claw.agent.tool.VoiceFunction;
import com.youkeda.exercise.claw.schedule.CourseImportHandler;
import com.youkeda.exercise.claw.schedule.CourseImportStateManager;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 消息路由器
 *
 * 按消息类型（IMAGE/VOICE/FILE/TEXT）分发到对应的处理器。
 * TEXT 消息由 ChatTool → ReActAgentExecutor 做 LLM tool-calling 循环；
 * 非 TEXT 消息直接分发给专用 Handler（VisionTool/VoiceFunction/FileTool）。
 *
 * 不包含业务逻辑，仅负责路由分发
 */
@Component
public class MessageRouter {

    private static final Logger log = LoggerFactory.getLogger(MessageRouter.class);

    private final ChatTool chatTool;
    private final VisionTool visionTool;
    private final SimpleReplyTool fallbackTool;
    private final VoiceFunction voiceTool;
    private final FileTool fileTool;
    private final CourseImportStateManager courseImportStateManager;
    private final CourseImportHandler courseImportHandler;

    public MessageRouter(ChatTool chatTool,
                         VisionTool visionTool,
                         SimpleReplyTool fallbackTool,
                         VoiceFunction voiceTool,
                         FileTool fileTool,
                         CourseImportStateManager courseImportStateManager,
                         CourseImportHandler courseImportHandler) {
        this.chatTool = chatTool;
        this.visionTool = visionTool;
        this.fallbackTool = fallbackTool;
        this.voiceTool = voiceTool;
        this.fileTool = fileTool;
        this.courseImportStateManager = courseImportStateManager;
        this.courseImportHandler = courseImportHandler;
    }

    /**
     * 路由消息到对应的处理器
     *
     * @param message 微信消息
     * @return 回复内容（WechatReply，包含 TEXT 或 IMAGE 类型）
     */
    public WechatReply route(WechatMessage message) {
        // 图片消息：检查课表导入状态
        if (message.getType() == MessageType.IMAGE) {
            String userId = message.getUserId();
            // 检查是否处于课表导入等待文件状态
            if (courseImportStateManager.getPhase(userId) == CourseImportStateManager.Phase.WAITING_FILE) {
                log.info("路由：图片消息 → CourseImportHandler（课表导入）| from={}", userId);
                WechatReply reply = courseImportHandler.handleImage(message);
                if (reply != null && reply.hasContent()) {
                    return reply;
                }
                // CourseImportHandler 返回空，说明状态异常，降级到 VisionTool
                log.warn("课表导入处理图片失败，降级到 VisionTool | from={}", userId);
            }

            log.info("路由：图片消息 → VisionTool | from={}", userId);
            WechatReply reply = visionTool.handle(message);
            return fallbackIfEmpty(reply, message);
        }

        // 语音消息：ASR → ChatTool（ReActAgentExecutor tool-calling）→ auto TTS
        if (message.getType() == MessageType.VOICE) {
            log.info("路由：语音消息 → ASR → ChatTool + auto TTS | from={}", message.getUserId());

            // 1. ASR 提取文本
            String voiceText = voiceTool.extractText(message);
            if (voiceText == null || voiceText.isEmpty()) {
                log.warn("语音识别失败 | from={}", message.getUserId());
                return fallbackTool.handle(message);
            }

            // 2. 构建文本消息走 ChatTool（ReActAgentExecutor 循环）
            WechatMessage textMsg = new WechatMessage();
            textMsg.setUserId(message.getUserId());
            textMsg.setContextToken(message.getContextToken());
            textMsg.setType(MessageType.TEXT);
            textMsg.setText(voiceText);

            WechatReply textReply = chatTool.handle(textMsg);
            if (textReply != null && textReply.isSilent()) {
                return textReply;
            }
            if (textReply == null || !textReply.hasContent()) {
                return fallbackIfEmpty(null, message);
            }
            // ChatTool 如果返回非文本（如图片），直接返回
            if (textReply.getType() != MessageType.TEXT) {
                return textReply;
            }

            // 3. 自动 TTS 语音回复
            WechatReply voiceReply = voiceTool.synthesizeTextToFile(textReply.getText());
            if (voiceReply != null && voiceReply.hasContent()) {
                return voiceReply;
            }

            // 4. TTS 失败降级为文本回复
            log.warn("TTS 合成失败，降级为文本回复 | from={}", message.getUserId());
            return textReply;
        }

        // 文件消息：检查课表导入状态
        if (message.getType() == MessageType.FILE) {
            String userId = message.getUserId();
            // 检查是否处于课表导入等待文件状态
            if (courseImportStateManager.getPhase(userId) == CourseImportStateManager.Phase.WAITING_FILE) {
                log.info("路由：文件消息 → CourseImportHandler（课表导入）| from={} | fileName={}",
                        userId, message.getFileName());
                WechatReply reply = courseImportHandler.handleFile(message);
                if (reply != null && reply.hasContent()) {
                    return reply;
                }
                // CourseImportHandler 返回空，说明状态异常或不支持的格式，降级到 FileTool
                log.warn("课表导入处理文件失败，降级到 FileTool | from={}", userId);
            }

            log.info("路由：文件消息 → FileTool | from={} | fileName={}", userId, message.getFileName());
            WechatReply reply = fileTool.handle(message);
            return fallbackIfEmpty(reply, message);
        }

        // 文本消息：全部走 ChatTool，由 ReActAgentExecutor 通过 LLM tool-calling 循环自主路由
        if (message.getType() == MessageType.TEXT) {
            log.info("路由：文本消息 → ChatTool | from={}", message.getUserId());
            WechatReply reply = chatTool.handle(message);
            return fallbackIfEmpty(reply, message);
        }

        // 其他类型：兜底
        log.info("路由：未知消息类型 type={} | from={}", message.getType(), message.getUserId());
        return fallbackTool.handle(message);
    }

    /**
     * 如果 Handler 返回空或没有内容，使用兜底回复
     */
    private WechatReply fallbackIfEmpty(WechatReply reply, WechatMessage message) {
        if (reply != null && reply.isSilent()) {
            return reply;
        }
        if (reply != null && reply.hasContent()) {
            return reply;
        }
        log.info("路由：Handler 返回空，使用兜底 | from={}", message.getUserId());
        return fallbackTool.handle(message);
    }
}
