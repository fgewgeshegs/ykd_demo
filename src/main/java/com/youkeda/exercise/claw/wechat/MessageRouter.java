package com.youkeda.exercise.claw.wechat;

import com.youkeda.exercise.claw.agent.tool.ChatTool;
import com.youkeda.exercise.claw.agent.tool.FileTool;
import com.youkeda.exercise.claw.agent.tool.SimpleReplyTool;
import com.youkeda.exercise.claw.agent.tool.VisionTool;
import com.youkeda.exercise.claw.agent.tool.VoiceFunction;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

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

    public MessageRouter(ChatTool chatTool,
                         VisionTool visionTool,
                         SimpleReplyTool fallbackTool,
                         VoiceFunction voiceTool,
                         FileTool fileTool) {
        this.chatTool = chatTool;
        this.visionTool = visionTool;
        this.fallbackTool = fallbackTool;
        this.voiceTool = voiceTool;
        this.fileTool = fileTool;
    }

    /**
     * 路由消息到对应的处理器，返回回复列表。
     *
     * @param message 微信消息
     * @return 回复列表，null 或空列表表示不回复
     */
    public List<WechatReply> route(WechatMessage message) {
        // 图片消息：直接走 VisionHandler（保留已有图片处理流程）
        if (message.getType() == MessageType.IMAGE) {
            log.info("路由：图片消息 → VisionTool | from={}", message.getUserId());
            return fallbackIfEmpty(visionTool.handle(message), message);
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

            List<WechatReply> replies = chatTool.handle(textMsg);
            if (replies == null || replies.isEmpty() || !hasContent(replies)) {
                return fallbackIfEmpty(null, message);
            }

            // 3. 取最后一条文本回复做 TTS，替换为语音文件
            WechatReply lastTextReply = null;
            int lastTextIdx = -1;
            for (int i = replies.size() - 1; i >= 0; i--) {
                if (replies.get(i).getType() == MessageType.TEXT) {
                    lastTextReply = replies.get(i);
                    lastTextIdx = i;
                    break;
                }
            }

            if (lastTextReply != null) {
                WechatReply voiceReply = voiceTool.synthesizeTextToFile(lastTextReply.getText());
                if (voiceReply != null && voiceReply.hasContent()) {
                    // TTS 成功：替换最后一条文本为语音，其余不变
                    List<WechatReply> mixed = new java.util.ArrayList<>(replies);
                    mixed.set(lastTextIdx, voiceReply);
                    return mixed;
                }
                // TTS 失败：返回原始列表
                log.warn("TTS 合成失败，保留文本回复 | from={}", message.getUserId());
            }

            return replies;
        }

        // 文件消息：直接走 FileTool
        if (message.getType() == MessageType.FILE) {
            log.info("路由：文件消息 → FileTool | from={} | fileName={}", message.getUserId(), message.getFileName());
            return fallbackIfEmpty(fileTool.handle(message), message);
        }

        // 文本消息：全部走 ChatTool
        if (message.getType() == MessageType.TEXT) {
            log.info("路由：文本消息 → ChatTool | from={}", message.getUserId());
            return fallbackIfEmpty(chatTool.handle(message), message);
        }

        // 其他类型：兜底
        log.info("路由：未知消息类型 type={} | from={}", message.getType(), message.getUserId());
        return fallbackTool.handle(message);
    }

    /**
     * 如果 Handler 返回空或没有内容，使用兜底回复
     */
    private List<WechatReply> fallbackIfEmpty(List<WechatReply> replies, WechatMessage message) {
        if (replies != null && !replies.isEmpty() && hasContent(replies)) {
            return replies;
        }
        log.info("路由：Handler 返回空，使用兜底 | from={}", message.getUserId());
        return fallbackTool.handle(message);
    }

    private boolean hasContent(List<WechatReply> replies) {
        for (WechatReply r : replies) {
            if (r != null && r.hasContent()) {
                return true;
            }
        }
        return false;
    }
}