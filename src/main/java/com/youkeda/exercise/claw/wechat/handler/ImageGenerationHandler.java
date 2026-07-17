package com.youkeda.exercise.claw.wechat.handler;

import com.youkeda.exercise.claw.ai.image.ImageGenerationService;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 图片生成处理器
 *
 * 职责：接收 IMAGE_GENERATE 意图的消息，委托 ImageGenerationService 完成图片生成
 * 不包含业务逻辑，仅负责请求接收与结果转交
 */
@Slf4j
@Component
public class ImageGenerationHandler implements MessageHandler {

    private final ImageGenerationService imageGenerationService;

    public ImageGenerationHandler(ImageGenerationService imageGenerationService) {
        this.imageGenerationService = imageGenerationService;
    }

    @Override
    public String handle(WechatMessage message) {
        log.info("ImageGenerationHandler 处理消息 | from={} | text={}", message.getUserId(), message.getText());

        return imageGenerationService.generate(message.getText());
    }
}