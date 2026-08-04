package com.youkeda.exercise.claw.infrastructure.channel.wechat.handler;

import com.youkeda.exercise.claw.agent.AgentExecutionPool;
import com.youkeda.exercise.claw.agent.CancellationManager;
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
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

/**
 * 聊天处理器
 *
 * <p>所有 TEXT 消息的入口，委托 {@link ReActAgentExecutor} 执行 tool-calling 循环。
 * 作为 WechatMessageHandler 暴露。
 *
 * <p><b>线程模型（Agent Interrupt 重构后）：</b>
 * <ul>
 *   <li>微信轮询线程仅负责接收消息，TEXT 消息通过 {@link AgentExecutionPool} 异步提交执行</li>
 *   <li>Agent 执行在独立线程池中运行，同一 userId 串行执行</li>
 *   <li>取消命令（"取消"/"停止"/"算了"）同步处理：设置取消标记 + 立即回复</li>
 * </ul>
 *
 * <p>TTS 语音合成特殊处理：当工具调用循环中触发了 {@code text_to_speech}，
 * VoiceTool 会暂存音频数据，在 executor 返回后优先发送语音文件而非纯文本回复。</p>
 */
@Component
public class ChatHandler implements WechatMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatHandler.class);
    private static final String FALLBACK_REPLY = "抱歉，我现在暂时无法回复，请稍后再试。";
    private static final String CANCELLED_REPLY = "👌 已取消当前任务。";

    /** 取消命令关键词集合 */
    private static final Set<String> CANCEL_KEYWORDS = Set.of("取消", "停止", "算了", "cancel", "stop");

    private final ReActAgentExecutor agentExecutor;
    private final com.youkeda.exercise.claw.tool.voice.VoiceTool voiceTool;
    private final com.youkeda.exercise.claw.tool.file.FileGenerationTool fileGenerationTool;
    private final com.youkeda.exercise.claw.tool.image.ImageGenerationTool imageGenerationTool;
    private final PlaceImageTool placeImageFunction;
    private final WechatILinkClient wechatClient;
    private final WechatUserManager wechatUserManager;
    private final AgentExecutionPool agentExecutionPool;
    private final CancellationManager cancellationManager;

    public ChatHandler(ReActAgentExecutor agentExecutor,
                       com.youkeda.exercise.claw.tool.voice.VoiceTool voiceTool,
                       com.youkeda.exercise.claw.tool.file.FileGenerationTool fileGenerationTool,
                       com.youkeda.exercise.claw.tool.image.ImageGenerationTool imageGenerationTool,
                       PlaceImageTool placeImageFunction,
                       WechatILinkClient wechatClient,
                       WechatUserManager wechatUserManager,
                       AgentExecutionPool agentExecutionPool,
                       CancellationManager cancellationManager) {
        this.agentExecutor = agentExecutor;
        this.voiceTool = voiceTool;
        this.fileGenerationTool = fileGenerationTool;
        this.imageGenerationTool = imageGenerationTool;
        this.placeImageFunction = placeImageFunction;
        this.wechatClient = wechatClient;
        this.wechatUserManager = wechatUserManager;
        this.agentExecutionPool = agentExecutionPool;
        this.cancellationManager = cancellationManager;
    }

    @Override
    public WechatReply handle(WechatMessage message) {
        if (message.getType() != MessageType.TEXT) {
            return null;
        }

        log.debug("ChatHandler.handle 处理消息 | from={} | text={}", message.getUserId(), message.getText());

        String userId = wechatUserManager.getOwnerUserId();
        String userText = message.getText();

        // 取消命令检测：同步处理，设置取消标记并立即回复
        if (isCancelCommand(userText)) {
            cancellationManager.cancel(userId);
            log.info("收到取消命令 | userId={} | text={}", userId, userText);
            wechatClient.sendTextMessage(userId, CANCELLED_REPLY);
            return WechatReply.silent();
        }

        com.youkeda.exercise.claw.agent.AgentContext context = new com.youkeda.exercise.claw.agent.AgentContext()
                .setUserId(userId)
                .setContextToken(message.getContextToken())
                .setRawMessage(message)
                .setMessage(userText)
                .setMessageType(MessageType.TEXT)
                .setRoundId(message.getRoundId());

        // 异步提交到 Agent 执行线程池，轮询线程立即返回
        try {
            agentExecutionPool.execute(userId, () -> executeAndSendReply(context, userId));
        } catch (RejectedExecutionException e) {
            log.error("Agent 执行线程池拒绝任务 | userId={}", userId, e);
            wechatClient.sendTextMessage(userId, "系统繁忙，请稍后再试。");
            return WechatReply.silent();
        }

        // 返回 silent 防止 MessageRouter 的 fallbackIfEmpty 误触发兜底回复
        return WechatReply.silent();
    }

    // ==================== 异步执行与回复 ====================

    /**
     * 在 Agent 执行线程中运行：清除取消标记 → 执行 Agent → 发送回复。
     */
    private void executeAndSendReply(
            com.youkeda.exercise.claw.agent.AgentContext context, String userId) {
        try {
            // 新任务开始前清除上一次的取消标记
            cancellationManager.clear(userId);

            String reply = agentExecutor.execute(context);
            sendReplyForUser(userId, reply);
        } catch (Exception e) {
            log.error("Agent 执行异常 | userId={}", userId, e);
            wechatClient.sendTextMessage(userId, FALLBACK_REPLY);
        }
    }

    /**
     * 将 Agent 回复发送给用户，处理 TTS/文件/图片等待发送产物。
     */
    private void sendReplyForUser(String userId, String reply) {
        if (ReActAgentExecutor.SILENT_REPLY.equals(reply)) {
            log.info("Agent 请求已处理，本轮无需发送回复 | from={}", userId);
            return;
        }

        if (reply == null || reply.isEmpty()) {
            log.warn("AI 回复为空，使用降级回复 | from={}", userId);
            wechatClient.sendTextMessage(userId, FALLBACK_REPLY);
            return;
        }

        // 检查 TTS 是否生成了待发送的语音
        com.youkeda.exercise.claw.tool.voice.VoiceTool.PendingAudio audio = voiceTool.consumePendingAudio();
        if (audio != null && audio.audioBytes() != null && audio.audioBytes().length > 0) {
            log.info("TTS 音频待发送 | size={}bytes | from={}", audio.audioBytes().length, userId);
            wechatClient.sendFileMessage(userId, audio.audioBytes(), "AI语音回复.mp3", audio.text());
            return;
        }

        // 检查文件生成工具是否产生了待发送的文件
        com.youkeda.exercise.claw.tool.file.FileGenerationTool.PendingFile file = fileGenerationTool.consumePendingFile();
        if (file != null && file.fileBytes() != null && file.fileBytes().length > 0) {
            log.info("待发送文件 | fileName={} | size={}bytes | from={}",
                    file.fileName(), file.fileBytes().length, userId);
            wechatClient.sendFileMessage(userId, file.fileBytes(), file.fileName(), file.description());
            return;
        }

        // 检查地点图片搜索是否产生了待发送的图片
        List<PlaceImageTool.PendingPlaceImage> placeImages = placeImageFunction.consumePendingPlaceImages();
        if (placeImages != null && !placeImages.isEmpty()) {
            PlaceImageTool.PendingPlaceImage first = placeImages.get(0);
            if (first.imageBytes() != null && first.imageBytes().length > 0) {
                log.info("待发送地点图片 | place={} | count={} | size={}bytes | from={}",
                        first.placeName(), placeImages.size(), first.imageBytes().length, userId);
                wechatClient.sendTextMessage(userId, reply);
                wechatClient.sendImageMessage(userId, first.imageBytes(), "抱歉，图片发送失败，请稍后再试。");
                return;
            }
        }

        // 检查图片生成工具是否产生了待发送的图片
        com.youkeda.exercise.claw.tool.image.ImageGenerationTool.PendingImage image = imageGenerationTool.consumePendingImage();
        if (image != null && image.imageBytes() != null && image.imageBytes().length > 0) {
            log.info("待发送图片 | size={}bytes | from={}", image.imageBytes().length, userId);
            wechatClient.sendTextMessage(userId, reply);
            wechatClient.sendImageMessage(userId, image.imageBytes(), "抱歉，图片发送失败，请稍后再试。");
            return;
        }

        wechatClient.sendTextMessage(userId, reply);
    }

    // ==================== 取消命令检测 ====================

    /**
     * 判断用户消息是否为取消命令。
     *
     * <p>匹配规则：trim 后以取消关键词开头（忽略大小写）。
     * 例如 "算了，太慢了" 命中 "算了"，"停止生成" 命中 "停止"，
     * 而 "如何取消订单" 不命中（关键词不在开头）。
     */
    private boolean isCancelCommand(String text) {
        if (text == null) return false;
        String trimmed = text.trim().toLowerCase();
        for (String keyword : CANCEL_KEYWORDS) {
            if (trimmed.startsWith(keyword)) {
                return true;
            }
        }
        return false;
    }
}