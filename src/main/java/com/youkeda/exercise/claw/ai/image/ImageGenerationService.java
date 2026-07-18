package com.youkeda.exercise.claw.ai.image;

import com.youkeda.exercise.claw.ai.llm.ImageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 图片生成服务
 *
 * 职责：封装图片生成的业务逻辑，调用 ImageClient 完成模型交互
 */
@Slf4j
@Service
public class ImageGenerationService {

    private static final String FALLBACK_MESSAGE = "抱歉，图片生成失败，请稍后再试。";

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
        } catch (Exception e) {
            log.error("ImageGenerationService 生成异常 | error={}", e.getMessage());
            return FALLBACK_MESSAGE;
        }
    }
}