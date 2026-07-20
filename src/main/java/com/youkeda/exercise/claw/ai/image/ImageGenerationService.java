package com.youkeda.exercise.claw.ai.image;

import com.youkeda.exercise.claw.ai.llm.ImageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 图片生成服务
 *
 * 职责：封装图片生成的业务逻辑，调用 ImageClient 完成模型交互
 * 当 API 返回侵权或内容安全错误时，直接告知用户无法生成
 */
@Slf4j
@Service
public class ImageGenerationService {

    private static final String FALLBACK_MESSAGE = "抱歉，图片生成失败，请稍后再试。";

    /** 内容安全审查拦截 */
    private static final String DATA_INSPECT_FAIL_MESSAGE = "抱歉，图片内容被安全审查拦截，无法生成。请尝试更换描述。";

    /** 版权侵权嫌疑 */
    private static final String IP_INFRINGEMENT_MESSAGE = "抱歉，您描述的内容涉及版权或IP保护，无法生成。请尝试描述其他内容。";

    private final ImageClient imageClient;

    public ImageGenerationService(ImageClient imageClient) {
        this.imageClient = imageClient;
    }

    /**
     * 根据提示词生成图片
     *
     * @param prompt 图片描述提示词
     * @return 生成结果文本（包含图片 URL 或错误信息）
     */
    public String generate(String prompt) {
        log.info("ImageGenerationService 开始生成 | prompt={}", prompt);

        try {
            String imageUrl = imageClient.generateImage(prompt);
            if (imageUrl == null) {
                log.warn("ImageGenerationService 生成失败");
                return FALLBACK_MESSAGE;
            }
            log.info("ImageGenerationService 生成完成");
            return "已为您生成图片：" + imageUrl;
        } catch (ImageClientException e) {
            String userMessage = errorCodeToUserMessage(e.getErrorCode());
            log.warn("图片生成被拒绝 | errorCode={} | prompt={}", e.getErrorCode(), prompt);
            return userMessage;
        } catch (Exception e) {
            log.error("ImageGenerationService 生成异常 | error={}", e.getMessage());
            return FALLBACK_MESSAGE;
        }
    }

    /**
     * 生成图片并返回原始图片 URL
     *
     * @param prompt 图片描述提示词
     * @return 图片 URL，失败时返回 null
     */
    public String generateImageUrl(String prompt) {
        log.info("ImageGenerationService 生成图片 URL | prompt={}", prompt);
        try {
            return imageClient.generateImage(prompt);
        } catch (Exception e) {
            log.error("ImageGenerationService 生成图片 URL 异常 | error={}", e.getMessage());
            return null;
        }
    }

    /**
     * 将 API 错误码转换为用户可读的提示信息
     */
    private static String errorCodeToUserMessage(String errorCode) {
        if ("DataInspectionFailed".equals(errorCode)) {
            return DATA_INSPECT_FAIL_MESSAGE;
        }
        if ("IPInfringementSuspect".equals(errorCode)) {
            return IP_INFRINGEMENT_MESSAGE;
        }
        return FALLBACK_MESSAGE;
    }
}
