package com.youkeda.exercise.claw.agent;

import com.youkeda.exercise.claw.ai.image.ImageGenerationService;
import com.youkeda.exercise.claw.ai.classifier.Intent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 图片生成工具
 *
 * 封装 ImageGenerationService，以 Tool 接口暴露给 Agent 体系。
 * 启动时自动注册到 ToolRegistry。
 */
@Component
public class ImageGenerationTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationTool.class);

    private final ImageGenerationService imageGenerationService;
    private final ToolRegistry toolRegistry;

    public ImageGenerationTool(ImageGenerationService imageGenerationService,
                               ToolRegistry toolRegistry) {
        this.imageGenerationService = imageGenerationService;
        this.toolRegistry = toolRegistry;
    }

    @PostConstruct
    public void init() {
        toolRegistry.register(this);
    }

    @Override
    public String name() {
        return "image_generate";
    }

    @Override
    public String description() {
        return "根据文字描述生成图片，调用 wanx-v1 模型";
    }

    @Override
    public Intent[] supportedIntents() {
        return new Intent[]{Intent.IMAGE_GENERATE};
    }

    @Override
    public String execute(AgentContext context) {
        log.info("ImageGenerationTool 执行 | user={} | prompt={}",
                context.getUserId(), context.getMessage());
        String imageUrl = imageGenerationService.generate(context.getMessage());
        if (imageUrl == null) {
            return "抱歉，图片生成失败，请稍后再试。";
        }
        return "已为您生成图片：" + imageUrl;
    }
}