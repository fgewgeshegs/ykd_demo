package com.youkeda.exercise.claw.wechat.handler;

import com.youkeda.exercise.claw.ai.llm.ImageClient;
import com.youkeda.exercise.claw.ai.vision.VisionService;
import com.youkeda.exercise.claw.context.ContextStore;
import com.youkeda.exercise.claw.wechat.client.WechatILinkClient;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * 图片分析处理器
 *
 * 支持场景：直接发图 / 上下文有最近图片
 */
@Slf4j
@Component
public class VisionHandler implements MessageHandler {

    private static final String FALLBACK_REPLY = "抱歉，我暂时无法分析图片，请稍后再试。";

    private final VisionService visionService;
    private final WechatILinkClient wechatClient;
    private final ContextStore contextStore;
    private final ImageClient imageClient;

    public VisionHandler(VisionService visionService,
                         WechatILinkClient wechatClient,
                         ContextStore contextStore,
                         ImageClient imageClient) {
        this.visionService = visionService;
        this.wechatClient = wechatClient;
        this.contextStore = contextStore;
        this.imageClient = imageClient;
    }

    @Override
    public WechatReply handle(WechatMessage message) {
        if (message.getType() == MessageType.IMAGE) {
            return analyzeImage(message);
        }

        if (message.getType() == MessageType.TEXT
                && (contextStore.getLastImage(message.getUserId()) != null
                    || contextStore.getLastImageUrl(message.getUserId()) != null)) {
            log.info("分析上下文中的最近图片 | user={} | text={}", message.getUserId(), message.getText());
            return analyzeImage(message);
        }

        return null;
    }

    private WechatReply analyzeImage(WechatMessage message) {
        log.info("VisionHandler 分析图片 | user={}", message.getUserId());

        String imageDataUrl = downloadImageAsDataUrl(message);
        if (imageDataUrl == null) {
            log.warn("无法获取图片数据 | from={}", message.getUserId());
            return WechatReply.text(FALLBACK_REPLY);
        }

        String reply = visionService.analyze(imageDataUrl, null);
        if (reply == null || reply.isEmpty()) {
            log.warn("图片分析失败 | from={}", message.getUserId());
            return WechatReply.text(FALLBACK_REPLY);
        }

        saveImageParams(message);

        contextStore.append(message.getUserId(), "user", "[用户发送了一张图片]");
        contextStore.append(message.getUserId(), "assistant", reply);

        return WechatReply.text(reply);
    }

    /**
     * 下载图片：直接 CDN → 上下文 CDN → 上下文 URL → 消息 URL
     */
    private String downloadImageAsDataUrl(WechatMessage message) {
        String encryptParam = message.getEncryptQueryParam();
        String aesKey = message.getAesKey();

        if (isEmpty(encryptParam) || isEmpty(aesKey)) {
            String[] lastImage = contextStore.getLastImage(message.getUserId());
            if (lastImage != null && lastImage.length == 2) {
                encryptParam = lastImage[0];
                aesKey = lastImage[1];
            }
        }

        if (!isEmpty(encryptParam) && !isEmpty(aesKey)) {
            byte[] bytes = wechatClient.downloadMedia(encryptParam, aesKey);
            if (bytes != null && bytes.length > 0) {
                return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes);
            }
        }

        String contextUrl = contextStore.getLastImageUrl(message.getUserId());
        if (contextUrl != null && !contextUrl.isEmpty()) {
            byte[] urlBytes = imageClient.downloadImage(contextUrl);
            if (urlBytes != null && urlBytes.length > 0) {
                return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(urlBytes);
            }
        }

        if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
            return message.getImageUrl();
        }

        return null;
    }

    private void saveImageParams(WechatMessage message) {
        String ep = message.getEncryptQueryParam();
        String ak = message.getAesKey();
        if (!isEmpty(ep) && !isEmpty(ak)) {
            contextStore.setLastImage(message.getUserId(), ep, ak);
        }
    }

    private boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
