package com.youkeda.exercise.claw.infrastructure.channel.wechat;

import com.youkeda.exercise.claw.infrastructure.channel.wechat.handler.ChatHandler;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.handler.FileHandler;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.handler.SimpleReplyHandler;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.handler.VisionHandler;
import com.youkeda.exercise.claw.tool.voice.VoiceTool;
import com.youkeda.exercise.claw.feature.schedule.CourseImportHandler;
import com.youkeda.exercise.claw.feature.schedule.CourseImportStateManager;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.model.MessageType;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.model.WechatReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 消息路由器
 *
 * 按消息类型（IMAGE/VOICE/FILE/TEXT）分发到对应的处理器。
 * TEXT 消息由 ChatHandler → ReActAgentExecutor 做 LLM tool-calling 循环；
 * 非 TEXT 消息直接分发给专用 Handler（VisionHandler/VoiceTool/FileHandler）。
 *
 * 不包含业务逻辑，仅负责路由分发
 */
@Component
public class MessageRouter {

    private static final Logger log = LoggerFactory.getLogger(MessageRouter.class);

    private final ChatHandler chatHandler;
    private final VisionHandler visionHandler;
    private final SimpleReplyHandler fallbackHandler;
    private final VoiceTool voiceTool;
    private final FileHandler fileHandler;
    private final CourseImportStateManager courseImportStateManager;
    private final CourseImportHandler courseImportHandler;

    public MessageRouter(ChatHandler chatHandler,
                         VisionHandler visionHandler,
                         SimpleReplyHandler fallbackHandler,
                         VoiceTool voiceTool,
                         FileHandler fileHandler,
                         CourseImportStateManager courseImportStateManager,
                         CourseImportHandler courseImportHandler) {
        this.chatHandler = chatHandler;
        this.visionHandler = visionHandler;
        this.fallbackHandler = fallbackHandler;
        this.voiceTool = voiceTool;
        this.fileHandler = fileHandler;
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
            // 处于任一导入阶段（等文件/等学期确认/等确认）的图片都按课表导入处理，
            // 避免用户按提示重发图片时被路由到通用 VisionHandler
            if (isImportPhase(courseImportStateManager.getPhase(userId))) {
                log.info("路由：图片消息 → CourseImportHandler（课表导入）| from={}", userId);
                WechatReply reply = courseImportHandler.handleImage(message);
                if (reply != null && reply.hasContent()) {
                    return reply;
                }
                // CourseImportHandler 返回空，说明状态异常，降级到 VisionHandler
                log.warn("课表导入处理图片失败，降级到 VisionHandler | from={}", userId);
            }

            log.info("路由：图片消息 → VisionHandler | from={}", userId);
            WechatReply reply = visionHandler.handle(message);
            return fallbackIfEmpty(reply, message);
        }

        // 语音消息：ASR → ChatHandler（ReActAgentExecutor tool-calling）→ auto TTS
        if (message.getType() == MessageType.VOICE) {
            log.info("路由：语音消息 → ASR → ChatHandler + auto TTS | from={}", message.getUserId());

            // 1. ASR 提取文本
            String voiceText = voiceTool.extractText(message);
            if (voiceText == null || voiceText.isEmpty()) {
                log.warn("语音识别失败 | from={}", message.getUserId());
                return fallbackHandler.handle(message);
            }

            // 2. 构建文本消息走 ChatHandler（ReActAgentExecutor 循环）
            WechatMessage textMsg = new WechatMessage();
            textMsg.setUserId(message.getUserId());
            textMsg.setContextToken(message.getContextToken());
            textMsg.setType(MessageType.TEXT);
            textMsg.setText(voiceText);
            // 继承语音消息的 roundId，使 executor 在同一 Turn 上闭合
            textMsg.setRoundId(message.getRoundId());

            WechatReply textReply = chatHandler.handle(textMsg);
            if (textReply != null && textReply.isSilent()) {
                return textReply;
            }
            if (textReply == null || !textReply.hasContent()) {
                return fallbackIfEmpty(null, message);
            }
            // ChatHandler 如果返回非文本（如图片），直接返回
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
            // 处于任一导入阶段的文件都按课表导入处理
            if (isImportPhase(courseImportStateManager.getPhase(userId))) {
                log.info("路由：文件消息 → CourseImportHandler（课表导入）| from={} | fileName={}",
                        userId, message.getFileName());
                WechatReply reply = courseImportHandler.handleFile(message);
                if (reply != null && reply.hasContent()) {
                    return reply;
                }
                // CourseImportHandler 返回空，说明状态异常或不支持的格式，降级到 FileHandler
                log.warn("课表导入处理文件失败，降级到 FileHandler | from={}", userId);
            }

            log.info("路由：文件消息 → FileHandler | from={} | fileName={}", userId, message.getFileName());
            WechatReply reply = fileHandler.handle(message);
            return fallbackIfEmpty(reply, message);
        }

        // 文本消息：全部走 ChatHandler，由 ReActAgentExecutor 通过 LLM tool-calling 循环自主路由
        if (message.getType() == MessageType.TEXT) {
            log.info("路由：文本消息 → ChatHandler | from={}", message.getUserId());
            WechatReply reply = chatHandler.handle(message);
            return fallbackIfEmpty(reply, message);
        }

        // 其他类型：兜底
        log.info("路由：未知消息类型 type={} | from={}", message.getType(), message.getUserId());
        return fallbackHandler.handle(message);
    }

    /**
     * 是否为课表导入进行中的阶段
     * <p>覆盖 等文件 / 等学期确认 / 等确认 三个导入阶段；NONE 表示无进行中的导入。</p>
     */
    private boolean isImportPhase(CourseImportStateManager.Phase phase) {
        return phase == CourseImportStateManager.Phase.WAITING_FILE
                || phase == CourseImportStateManager.Phase.WAITING_SEMESTER
                || phase == CourseImportStateManager.Phase.WAITING_CONFIRM;
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
        return fallbackHandler.handle(message);
    }
}
