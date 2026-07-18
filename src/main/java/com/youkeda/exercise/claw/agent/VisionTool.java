package com.youkeda.exercise.claw.agent;

import com.youkeda.exercise.claw.ai.vision.VisionService;
import com.youkeda.exercise.claw.ai.classifier.Intent;
import com.youkeda.exercise.claw.wechat.client.WechatILinkClient;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * 视觉理解工具
 *
 * 封装 VisionService + 微信图片下载，以 Tool 接口暴露给 Agent 体系。
 * 启动时自动注册到 ToolRegistry。
 */
@Component
public class VisionTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(VisionTool.class);

    private final VisionService visionService;
    private final WechatILinkClient wechatClient;
    private final ToolRegistry toolRegistry;

    public VisionTool(VisionService visionService,
                      WechatILinkClient wechatClient,
                      ToolRegistry toolRegistry) {
        this.visionService = visionService;
        this.wechatClient = wechatClient;
        this.toolRegistry = toolRegistry;
    }

    @PostConstruct
    public void init() {
        toolRegistry.register(this);
    }

    @Override
    public String name() {
        return "vision";
    }

    @Override
    public String description() {
        return "图片理解与分析，支持多模态视觉模型";
    }

    @Override
    public Intent[] supportedIntents() {
        return new Intent[]{Intent.IMAGE_ANALYZE};
    }

    @Override
    public String execute(AgentContext context) {
        log.info("VisionTool 执行 | user={}", context.getUserId());

        String imageDataUrl = resolveImage(context);
        if (imageDataUrl == null) {
            return "抱歉，无法获取图片数据。";
        }

        String question = context.getMessage();
        return visionService.analyze(imageDataUrl, question);
    }

    /**
     * 从上下文中获取图片数据（优先 base64 下载，降级为原始 URL）
     */
    private String resolveImage(AgentContext context) {
        WechatMessage raw = context.getRawMessage();
        if (raw == null || raw.getType() != MessageType.IMAGE) {
            return null;
        }

        // 尝试 CDN 下载
        String encryptQueryParam = raw.getEncryptQueryParam();
        String aesKey = raw.getAesKey();

        if (encryptQueryParam != null && !encryptQueryParam.isEmpty()
                && aesKey != null && !aesKey.isEmpty()) {
            byte[] imageBytes = wechatClient.downloadMedia(encryptQueryParam, aesKey);
            if (imageBytes != null && imageBytes.length > 0) {
                return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageBytes);
            }
        }

        // 降级：使用原始 URL
        return raw.getImageUrl();
    }
}