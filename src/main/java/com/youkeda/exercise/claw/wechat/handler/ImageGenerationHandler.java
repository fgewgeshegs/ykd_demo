package com.youkeda.exercise.claw.wechat.handler;

import com.lth.wechat.ilink.ILinkClient;
import com.youkeda.exercise.claw.ai.image.ImageGenerationService;
import com.youkeda.exercise.claw.common.HttpClientUtil;
import com.youkeda.exercise.claw.wechat.client.WechatILinkClient;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 图片生成处理器
 *
 * 职责：接收 IMAGE_GENERATE 意图的消息，生成图片并直接以图片消息发送回微信
 * 流程：生成 URL → 下载图片 bytes → 上传微信 CDN → 发送图片消息
 * 异常时逐级降级：成功发图片 → 降级返回 URL 文本 → 返回错误提示
 */
@Slf4j
@Component
public class ImageGenerationHandler implements MessageHandler {

    private static final String GENERATE_FAIL = "抱歉，图片生成失败，请稍后再试。";
    private static final String GENERATED_FILE_NAME = "generated.png";

    private final ImageGenerationService imageGenerationService;
    private final WechatILinkClient wechatClient;
    private final HttpClientUtil httpClientUtil;

    public ImageGenerationHandler(ImageGenerationService imageGenerationService,
                                  WechatILinkClient wechatClient) {
        this.imageGenerationService = imageGenerationService;
        this.wechatClient = wechatClient;
        this.httpClientUtil = new HttpClientUtil();
    }

    @Override
    public String handle(WechatMessage message) {
        String userId = message.getUserId();
        String contextToken = message.getContextToken();
        String prompt = message.getText();

        log.info("ImageGenerationHandler 处理消息 | from={} | text={}", userId, prompt);

        // 1. 生成图片获取 URL
        String imageUrl = imageGenerationService.generateImageUrl(prompt);
        if (imageUrl == null) {
            log.warn("图片生成失败 | from={}", userId);
            return GENERATE_FAIL;
        }

        // 2. 从 URL 下载图片二进制
        byte[] imageBytes;
        try {
            imageBytes = httpClientUtil.doGetBytes(imageUrl);
            log.info("图片下载成功 | size={} bytes", imageBytes.length);
        } catch (Exception e) {
            log.error("下载图片失败 | url={} | error={}", imageUrl, e.getMessage());
            return "已为您生成图片：" + imageUrl;
        }

        // 3. 上传到微信 CDN（使用真实用户 ID）
        ILinkClient.MediaInfo mediaInfo = wechatClient.uploadMedia(imageBytes, GENERATED_FILE_NAME, userId);
        if (mediaInfo == null) {
            log.warn("微信媒体上传失败，降级返回 URL | from={}", userId);
            return "已为您生成图片：" + imageUrl;
        }

        // 4. 发送图片消息
        wechatClient.sendImageMessage(userId, contextToken, mediaInfo);

        // 图片已通过微信消息发送，不返回文本回复
        return null;
    }
}