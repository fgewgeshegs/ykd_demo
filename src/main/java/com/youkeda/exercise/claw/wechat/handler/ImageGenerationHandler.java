package com.youkeda.exercise.claw.wechat.handler;

import com.youkeda.exercise.claw.ai.image.ImageGenerationService;
import com.youkeda.exercise.claw.ai.llm.ImageClient;
import com.youkeda.exercise.claw.context.ContextStore;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 图片生成处理器
 *
 * 职责：接收 IMAGE_GENERATE 意图的消息，生成图片并以下载的字节数据返回，
 *       由上游 WechatMessageService 负责上传 CDN 和发送图片消息。
 *       流程：生成 URL → ImageClient 下载字节 → 返回 WechatReply.image
 */
@Slf4j
@Component
public class ImageGenerationHandler implements MessageHandler {

    private static final String FALLBACK_REPLY = "抱歉，图片生成失败，请稍后再试。";

    private final ImageGenerationService imageGenerationService;
    private final ImageClient imageClient;
    private final ContextStore contextStore;

    public ImageGenerationHandler(ImageGenerationService imageGenerationService,
                                  ImageClient imageClient,
                                  ContextStore contextStore) {
        this.imageGenerationService = imageGenerationService;
        this.imageClient = imageClient;
        this.contextStore = contextStore;
    }

    @Override
    public WechatReply handle(WechatMessage message) {
        log.info("ImageGenerationHandler 处理消息 | from={} | text={}", message.getUserId(), message.getText());

        // 1. 生成图片，拿到 URL
        String imageUrl = imageGenerationService.generate(message.getText());
        if (imageUrl == null) {
            log.warn("ImageGenerationHandler 图片生成失败");
            return WechatReply.text(FALLBACK_REPLY);
        }

        // 2. 记住 AI 图片 URL（后续描述/编辑可从此下载）
        contextStore.setLastImageUrl(message.getUserId(), imageUrl);

        // 3. 下载图片字节
        byte[] imageBytes = imageClient.downloadImage(imageUrl);
        if (imageBytes == null || imageBytes.length == 0) {
            log.warn("ImageGenerationHandler 图片下载失败 | url={}", imageUrl);
            return WechatReply.text(FALLBACK_REPLY);
        }

        log.info("ImageGenerationHandler 图片生成并下载成功 | size={} bytes", imageBytes.length);
        return WechatReply.image(imageBytes);
    }
}