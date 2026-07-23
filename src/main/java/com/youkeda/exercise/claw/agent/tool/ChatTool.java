package com.youkeda.exercise.claw.agent.tool;

import com.youkeda.exercise.claw.agent.ReActAgentExecutor;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 聊天工具
 *
 * <p>所有 TEXT 消息的入口，委托 {@link ReActAgentExecutor} 执行 tool-calling 循环。
 * 作为 WechatMessageHandler 暴露。
 *
 * <p>TTS 语音合成特殊处理：当工具调用循环中触发了 {@code text_to_speech}，
 * {@link VoiceTool} 会暂存音频数据，{@link #handle(WechatMessage)} 在 executor
 * 返回后优先发送语音文件而非纯文本回复。</p>
 */
@Component
public class ChatTool implements WechatMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatTool.class);
    private static final String FALLBACK_REPLY = "抱歉，我现在暂时无法回复，请稍后再试。";

    private final ReActAgentExecutor agentExecutor;
    private final VoiceTool voiceTool;
    private final FileGenerationTool fileGenerationTool;
    private final ImageGenerationTool imageGenerationTool;

    public ChatTool(ReActAgentExecutor agentExecutor,
                    VoiceTool voiceTool, FileGenerationTool fileGenerationTool,
                    ImageGenerationTool imageGenerationTool) {
        this.agentExecutor = agentExecutor;
        this.voiceTool = voiceTool;
        this.fileGenerationTool = fileGenerationTool;
        this.imageGenerationTool = imageGenerationTool;
    }

    @Override
    public WechatReply handle(WechatMessage message) {
        if (message.getType() != MessageType.TEXT) {
            return null;
        }

        log.debug("ChatTool.handle 处理消息 | from={} | text={}", message.getUserId(), message.getText());

        String reply = agentExecutor.execute(new com.youkeda.exercise.claw.agent.AgentContext()
                .setUserId(message.getUserId())
                .setMessage(message.getText())
                .setMessageType(MessageType.TEXT));

        if (reply == null || reply.isEmpty()) {
            log.warn("AI 回复为空，使用降级回复 | from={}", message.getUserId());
            return WechatReply.text(FALLBACK_REPLY);
        }

        // 检查 TTS 是否生成了待发送的语音（text_to_speech 工具调用的结果）
        VoiceTool.PendingAudio audio = voiceTool.consumePendingAudio();
        if (audio != null && audio.audioBytes() != null && audio.audioBytes().length > 0) {
            log.info("TTS 音频待发送 | size={}bytes | from={}", audio.audioBytes().length, message.getUserId());
            return WechatReply.file(audio.audioBytes(), "AI语音回复.mp3", audio.text());
        }

        // 检查文件生成工具是否产生了待发送的文件（file_generate 工具调用的结果）
        FileGenerationTool.PendingFile file = fileGenerationTool.consumePendingFile();
        if (file != null && file.fileBytes() != null && file.fileBytes().length > 0) {
            log.info("待发送文件 | fileName={} | size={}bytes | from={}",
                    file.fileName(), file.fileBytes().length, message.getUserId());
            return WechatReply.file(file.fileBytes(), file.fileName(), file.description());
        }

        // 检查图片生成工具是否产生了待发送的图片（image_generate 工具调用的结果）
        ImageGenerationTool.PendingImage image = imageGenerationTool.consumePendingImage();
        if (image != null && image.imageBytes() != null && image.imageBytes().length > 0) {
            log.info("待发送图片 | size={}bytes | from={}", image.imageBytes().length, message.getUserId());
            return WechatReply.image(image.imageBytes());
        }

        return WechatReply.text(reply);
    }
}
