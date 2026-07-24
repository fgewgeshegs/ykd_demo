package com.youkeda.exercise.claw.agent.tool;

import com.youkeda.exercise.claw.agent.ReActAgentExecutor;
import com.youkeda.exercise.claw.agent.model.AgentMediaResponse;
import com.youkeda.exercise.claw.map.PlaceImageFunction;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天工具
 *
 * <p>所有 TEXT 消息的入口，委托 {@link ReActAgentExecutor} 执行 tool-calling 循环。
 * 作为 WechatMessageHandler 暴露。
 *
 * <p>回复处理采用两层策略：
 * <ol>
 *   <li><b>stash-consume</b>：工具调用循环中将二进制数据（音频/文件/生成图片）
 *       暂存到对应 Function，ChatTool 消费后发送。适用于 VoiceFunction、
 *       FileGenerationTool、ImageGenerationTool。</li>
 *   <li><b>reply-parse</b>：LLM 在回复文本中嵌入约定的 JSON 格式表达富媒体内容。
 *       通过 {@link AgentMediaResponse#tryParse(String)} 统一解析，
 *       解析成功后拆分为多条 WechatReply：图片优先，文字其次，链接最后。
 *       下载失败降至纯文字。</li>
 * </ol>
 */
@Component
public class ChatTool implements WechatMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatTool.class);
    private static final String FALLBACK_REPLY = "抱歉，我现在暂时无法回复，请稍后再试。";
    private static final String LINK_TITLE_DEFAULT = "查看完整方案";

    private final ReActAgentExecutor agentExecutor;
    private final VoiceFunction voiceTool;
    private final FileGenerationTool fileGenerationTool;
    private final ImageGenerationTool imageGenerationTool;
    private final PlaceImageFunction placeImageFunction;

    public ChatTool(ReActAgentExecutor agentExecutor,
                    VoiceFunction voiceTool, FileGenerationTool fileGenerationTool,
                    ImageGenerationTool imageGenerationTool,
                    PlaceImageFunction placeImageFunction) {
        this.agentExecutor = agentExecutor;
        this.voiceTool = voiceTool;
        this.fileGenerationTool = fileGenerationTool;
        this.imageGenerationTool = imageGenerationTool;
        this.placeImageFunction = placeImageFunction;
    }

    @Override
    public List<WechatReply> handle(WechatMessage message) {
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
            return List.of(WechatReply.text(FALLBACK_REPLY));
        }

        // ========================= stash-consume 层 =========================

        // 地点图片（优先级最高：可能返回多条图片+文字，不丢弃 LLM 文本）
        java.util.List<PlaceImageFunction.PendingPlaceImage> placeImages =
                placeImageFunction.consumePendingPlaceImages();
        if (placeImages != null && !placeImages.isEmpty()) {
            log.info("地点图片待发送 | count={} | from={}",
                    placeImages.size(), message.getUserId());

            List<WechatReply> replies = new ArrayList<>();

            // 先发送所有图片
            for (PlaceImageFunction.PendingPlaceImage img : placeImages) {
                if (img.imageBytes() != null && img.imageBytes().length > 0) {
                    replies.add(WechatReply.image(img.imageBytes()));
                }
            }

            // 再追加 LLM 的文字回复
            if (reply != null && !reply.isBlank()) {
                replies.add(WechatReply.text(reply));
            }

            return replies;
        }

        // TTS 语音
        VoiceFunction.PendingAudio audio = voiceTool.consumePendingAudio();
        if (audio != null && audio.audioBytes() != null && audio.audioBytes().length > 0) {
            log.info("TTS 音频待发送 | size={}bytes | from={}", audio.audioBytes().length, message.getUserId());
            return List.of(WechatReply.file(audio.audioBytes(), "AI语音回复.mp3", audio.text()));
        }

        // 文件生成
        FileGenerationTool.PendingFile file = fileGenerationTool.consumePendingFile();
        if (file != null && file.fileBytes() != null && file.fileBytes().length > 0) {
            log.info("待发送文件 | fileName={} | size={}bytes | from={}",
                    file.fileName(), file.fileBytes().length, message.getUserId());
            return List.of(WechatReply.file(file.fileBytes(), file.fileName(), file.description()));
        }

        // 图片生成
        ImageGenerationTool.PendingImage image = imageGenerationTool.consumePendingImage();
        if (image != null && image.imageBytes() != null && image.imageBytes().length > 0) {
            log.info("待发送图片 | size={}bytes | from={}", image.imageBytes().length, message.getUserId());
            return List.of(WechatReply.image(image.imageBytes()));
        }

        // ========================= reply-parse 层 =========================

        AgentMediaResponse mediaResponse = AgentMediaResponse.tryParse(reply);
        if (mediaResponse != null) {
            log.info("解析到媒体回复 | userId={} | text={} | imageCount={} | hasUrl={}",
                    message.getUserId(),
                    mediaResponse.getText() != null
                            ? mediaResponse.getText().substring(0, Math.min(80, mediaResponse.getText().length()))
                            : "<null>",
                    mediaResponse.getImages().size(),
                    mediaResponse.hasUrl());

            if (mediaResponse.hasImages() || mediaResponse.hasUrl()) {
                return buildMediaReplies(mediaResponse, message.getUserId());
            }

            // 仅有 text，无图片和链接
            String text = mediaResponse.getText();
            return List.of(WechatReply.text(text != null && !text.isEmpty() ? text : reply));
        }

        return List.of(WechatReply.text(reply));
    }

    // ==================== Private helpers ====================

    /**
     * 从 AgentMediaResponse 构建多条 WechatReply。
     *
     * <p>发送顺序：图片 → 文字介绍 → 查看完整方案链接。
     * 下载失败降级为纯文字（含图片 URL）。
     *
     * @return 回复列表，按顺序：IMAGE（如有）、TEXT（如有）、LINK（如有 url）
     */
    private List<WechatReply> buildMediaReplies(AgentMediaResponse mediaResponse, String userId) {
        String text = mediaResponse.getText();
        List<String> imageUrls = mediaResponse.getImages();
        String detailUrl = mediaResponse.getUrl();

        List<WechatReply> replies = new ArrayList<>();

        // 1. 图片（如有）
        if (!imageUrls.isEmpty()) {
            byte[] imageBytes = downloadImageBytes(imageUrls.get(0));
            if (imageBytes != null && imageBytes.length > 0) {
                replies.add(WechatReply.image(imageBytes));
                log.info("媒体回复-图片 | userId={} | size={}bytes", userId, imageBytes.length);
            } else {
                // 图片下载失败：将图片 URL 拼入文字
                log.warn("媒体图片下载失败 | userId={} | url={}", userId, imageUrls.get(0));
                text = appendImageUrlsToText(text, imageUrls);
            }
        }

        // 2. 文字介绍（如有）
        if (text != null && !text.isBlank()) {
            replies.add(WechatReply.text(text));
            log.info("媒体回复-文字 | userId={} | textLen={}", userId, text.length());
        }

        // 3. 链接（如有）
        if (detailUrl != null && !detailUrl.isBlank()) {
            String linkTitle = LINK_TITLE_DEFAULT;
            String linkDesc = "点击查看详情";
            replies.add(WechatReply.link(linkTitle, linkDesc, detailUrl));
            log.info("媒体回复-链接 | userId={} | url={}", userId, detailUrl);
        }

        return replies;
    }

    /**
     * 将图片 URL 拼入文字末尾（图片下载失败降级用）
     */
    private String appendImageUrlsToText(String text, List<String> imageUrls) {
        StringBuilder sb = new StringBuilder();
        if (text != null && !text.isBlank()) {
            sb.append(text).append("\n\n");
        }
        sb.append("📷 地点图片：");
        for (String url : imageUrls) {
            sb.append("\n").append(url);
        }
        return sb.toString();
    }

    /**
     * 下载图片字节（使用 Java 原生 HTTP 客户端）。
     * 自动处理 URL 中的非 ASCII 字符（如中文路径）。
     */
    private byte[] downloadImageBytes(String imageUrl) {
        try {
            String encodedUrl = encodeUrl(imageUrl);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(encodedUrl))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
            log.warn("图片下载 HTTP {} | url={}", response.statusCode(), encodedUrl);
            return null;
        } catch (Exception e) {
            log.warn("图片下载失败 | url={} | error={}", imageUrl, e.getMessage());
            return null;
        }
    }

    /**
     * 对 URL 中的非 ASCII 字符进行编码，避免 URI.create() 异常。
     */
    private String encodeUrl(String url) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c <= 127) {
                sb.append(c);
            } else {
                sb.append(URLEncoder.encode(String.valueOf(c), StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }
}