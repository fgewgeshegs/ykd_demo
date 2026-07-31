package com.youkeda.exercise.claw.infrastructure.channel.wechat.handler;

import com.youkeda.exercise.claw.agent.ReActAgentExecutor;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.WechatMessageHandler;
import com.youkeda.exercise.claw.tool.map.PlaceImageTool;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.client.WechatILinkClient;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.model.MessageType;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.user.WechatUserManager;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.model.WechatReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 聊天处理器
 *
 * <p>所有 TEXT 消息的入口，委托 {@link ReActAgentExecutor} 执行 tool-calling 循环。
 * 作为 WechatMessageHandler 暴露。
 *
 * <p>TTS 语音合成特殊处理：当工具调用循环中触发了 {@code text_to_speech}，
 * VoiceTool 会暂存音频数据，{@link #handle(WechatMessage)} 在 executor
 * 返回后优先发送语音文件而非纯文本回复。</p>
 */
@Component
public class ChatHandler implements WechatMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatHandler.class);
    private static final String FALLBACK_REPLY = "抱歉，我现在暂时无法回复，请稍后再试。";

    private final ReActAgentExecutor agentExecutor;
    private final com.youkeda.exercise.claw.tool.voice.VoiceTool voiceTool;
    private final com.youkeda.exercise.claw.tool.file.FileGenerationTool fileGenerationTool;
    private final com.youkeda.exercise.claw.tool.image.ImageGenerationTool imageGenerationTool;
    private final PlaceImageTool placeImageFunction;
    private final WechatILinkClient wechatClient;
    private final WechatUserManager wechatUserManager;

    public ChatHandler(ReActAgentExecutor agentExecutor,
                       com.youkeda.exercise.claw.tool.voice.VoiceTool voiceTool,
                       com.youkeda.exercise.claw.tool.file.FileGenerationTool fileGenerationTool,
                       com.youkeda.exercise.claw.tool.image.ImageGenerationTool imageGenerationTool,
                       PlaceImageTool placeImageFunction,
                       WechatILinkClient wechatClient,
                       WechatUserManager wechatUserManager) {
        this.agentExecutor = agentExecutor;
        this.voiceTool = voiceTool;
        this.fileGenerationTool = fileGenerationTool;
        this.imageGenerationTool = imageGenerationTool;
        this.placeImageFunction = placeImageFunction;
        this.wechatClient = wechatClient;
        this.wechatUserManager = wechatUserManager;
    }

    @Override
    public WechatReply handle(WechatMessage message) {
        if (message.getType() != MessageType.TEXT) {
            return null;
        }

        log.debug("ChatHandler.handle 处理消息 | from={} | text={}", message.getUserId(), message.getText());

        String userId = wechatUserManager.getOwnerUserId();
        com.youkeda.exercise.claw.agent.AgentContext context = new com.youkeda.exercise.claw.agent.AgentContext()
                .setUserId(userId)
                .setContextToken(message.getContextToken())
                .setRawMessage(message)
                .setMessage(message.getText())
                .setMessageType(MessageType.TEXT);
        String reply = agentExecutor.execute(context);

        if (ReActAgentExecutor.SILENT_REPLY.equals(reply)) {
            log.info("Agent 请求已处理，本轮无需发送回复 | from={}", message.getUserId());
            return WechatReply.silent();
        }

        if (reply == null || reply.isEmpty()) {
            log.warn("AI 回复为空，使用降级回复 | from={}", message.getUserId());
            return WechatReply.text(FALLBACK_REPLY);
        }

        // 检查 TTS 是否生成了待发送的语音（text_to_speech 工具调用的结果）
        com.youkeda.exercise.claw.tool.voice.VoiceTool.PendingAudio audio = voiceTool.consumePendingAudio();
        if (audio != null && audio.audioBytes() != null && audio.audioBytes().length > 0) {
            log.info("TTS 音频待发送 | size={}bytes | from={}", audio.audioBytes().length, message.getUserId());
            return WechatReply.file(audio.audioBytes(), "AI语音回复.mp3", audio.text());
        }

        // 检查文件生成工具是否产生了待发送的文件（file_generate 工具调用的结果）
        com.youkeda.exercise.claw.tool.file.FileGenerationTool.PendingFile file = fileGenerationTool.consumePendingFile();
        if (file != null && file.fileBytes() != null && file.fileBytes().length > 0) {
            log.info("待发送文件 | fileName={} | size={}bytes | from={}",
                    file.fileName(), file.fileBytes().length, message.getUserId());
            return WechatReply.file(file.fileBytes(), file.fileName(), file.description());
        }

        // 检查地点图片搜索是否产生了待发送的图片（place_image_search 工具调用的结果）
        List<PlaceImageTool.PendingPlaceImage> placeImages = placeImageFunction.consumePendingPlaceImages();
        if (placeImages != null && !placeImages.isEmpty()) {
            PlaceImageTool.PendingPlaceImage first = placeImages.get(0);
            if (first.imageBytes() != null && first.imageBytes().length > 0) {
                log.info("待发送地点图片 | place={} | count={} | size={}bytes | from={}",
                        first.placeName(), placeImages.size(), first.imageBytes().length, message.getUserId());
                // 文本先行发出，再返回图片
                wechatClient.sendTextMessage(message.getUserId(), reply);
                return WechatReply.image(first.imageBytes());
            }
        }

        // 检查图片生成工具是否产生了待发送的图片（image_generate 工具调用的结果）
        com.youkeda.exercise.claw.tool.image.ImageGenerationTool.PendingImage image = imageGenerationTool.consumePendingImage();
        if (image != null && image.imageBytes() != null && image.imageBytes().length > 0) {
            log.info("待发送图片 | size={}bytes | from={}", image.imageBytes().length, message.getUserId());
            wechatClient.sendTextMessage(message.getUserId(), reply);
            return WechatReply.image(image.imageBytes());
        }

        return WechatReply.text(reply);
    }
}
