package com.youkeda.exercise.claw.wechat.handler;

import com.youkeda.exercise.claw.ai.image.ImageClient;
import com.youkeda.exercise.claw.ai.image.ImageGenerationService;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 图片生成处理器
 *
 * 职责：接收 IMAGE_GENERATE 意图的消息，委托 ImageGenerationService 完成图片生成，
 *       然后通过 ImageClient 下载图片字节，以 WechatReply.image() 返回给消息服务发送。
 */
@Slf4j
@Component
public class ImageGenerationHandler implements MessageHandler {

    private static final String FALLBACK_REPLY = "抱歉，图片生成失败，请稍后再试。";

    private final ImageGenerationService imageGenerationService;
    private final ImageClient imageClient;

    public ImageGenerationHandler(ImageGenerationService imageGenerationService,
                                  ImageClient imageClient) {
        this.imageGenerationService = imageGenerationService;
        this.imageClient = imageClient;
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

        // 2. 下载图片字节
        byte[] imageBytes = imageClient.downloadImage(imageUrl);
        if (imageBytes == null || imageBytes.length == 0) {
            log.warn("ImageGenerationHandler 图片下载失败 | url={}", imageUrl);
            return WechatReply.text(FALLBACK_REPLY);
        }

        log.info("ImageGenerationHandler 图片生成并下载成功 | size={} bytes", imageBytes.length);
        return WechatReply.image(imageBytes);
    }
}