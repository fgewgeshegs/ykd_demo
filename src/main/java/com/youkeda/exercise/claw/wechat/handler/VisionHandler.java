package com.youkeda.exercise.claw.wechat.handler;

import com.youkeda.exercise.claw.ai.vision.VisionService;
import com.youkeda.exercise.claw.wechat.client.WechatILinkClient;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * 图片消息处理器
 *
 * 职责：接收 IMAGE 类型的消息，委托 VisionService 完成图片分析
 * 不包含模型调用逻辑，仅负责图片下载与结果转交
 */
@Slf4j
@Component
public class VisionHandler implements MessageHandler {

    private static final String FALLBACK_REPLY = "抱歉，我暂时无法分析图片，请稍后再试。";

    private final VisionService visionService;
    private final WechatILinkClient wechatClient;

    public VisionHandler(VisionService visionService, WechatILinkClient wechatClient) {
        this.visionService = visionService;
        this.wechatClient = wechatClient;
    }

    @Override
    public String handle(WechatMessage message) {
        if (message.getType() != MessageType.IMAGE) {
            return null;
        }

        log.info("收到图片消息 user={}", message.getUserId());

        // 1. 尝试从微信 CDN 下载图片并转为 base64 data URL
        String imageDataUrl = downloadImageAsDataUrl(message);
        if (imageDataUrl == null) {
            // 降级：使用原始 URL（部分模型/提供商可能支持）
            imageDataUrl = message.getImageUrl();
        }

        if (imageDataUrl == null) {
            log.warn("无法获取图片数据，使用降级回复 | from={}", message.getUserId());
            return FALLBACK_REPLY;
        }

        // 2. 委托 VisionService 分析图片
        String reply = visionService.analyze(imageDataUrl, null);

        if (reply == null || reply.isEmpty()) {
            log.warn("图片分析失败，使用降级回复 | from={}", message.getUserId());
            return FALLBACK_REPLY;
        }

        return reply;
    }

    /**
     * 通过微信 CDN 下载图片并转换为 base64 data URL
     */
    private String downloadImageAsDataUrl(WechatMessage message) {
        String encryptQueryParam = message.getEncryptQueryParam();
        String aesKey = message.getAesKey();

        if (encryptQueryParam == null || encryptQueryParam.isEmpty()
                || aesKey == null || aesKey.isEmpty()) {
            log.warn("图片加密参数不完整，无法下载 | encryptQueryParam={}", encryptQueryParam);
            return null;
        }

        byte[] imageBytes = wechatClient.downloadMedia(encryptQueryParam, aesKey);
        if (imageBytes == null || imageBytes.length == 0) {
            log.warn("图片下载失败，返回空数据");
            return null;
        }

        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        String dataUrl = "data:image/jpeg;base64," + base64;
        log.info("图片已转换为 base64 data URL，大小={} bytes", imageBytes.length);
        return dataUrl;
    }
}