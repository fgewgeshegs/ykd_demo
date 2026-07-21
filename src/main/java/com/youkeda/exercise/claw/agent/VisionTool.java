package com.youkeda.exercise.claw.agent;

import com.youkeda.exercise.claw.agent.classify.Intent;
import com.youkeda.exercise.claw.agent.memory.ContextStore;
import com.youkeda.exercise.claw.agent.memory.Message;
import com.youkeda.exercise.claw.agent.tool.MessageHandler;
import com.youkeda.exercise.claw.ai.llm.ImageClient;
import com.youkeda.exercise.claw.ai.vision.VisionService;
import com.youkeda.exercise.claw.wechat.client.WechatILinkClient;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * 视觉理解工具
 *
 * 封装 VisionService + 微信图片下载，同时作为 Tool 和 MessageHandler 暴露。
 * 支持场景：直接发图 / 上下文有最近图片 / Agent 调用。
 * 启动时自动注册到 ToolRegistry。
 */
@Component
public class VisionTool implements Tool, MessageHandler {

    private static final Logger log = LoggerFactory.getLogger(VisionTool.class);
    private static final String FALLBACK_REPLY = "抱歉，我暂时无法分析图片，请稍后再试。";

    private final VisionService visionService;
    private final WechatILinkClient wechatClient;
    private final ContextStore contextStore;
    private final ImageClient imageClient;
    private final ToolRegistry toolRegistry;

    public VisionTool(VisionService visionService,
                      WechatILinkClient wechatClient,
                      ContextStore contextStore,
                      ImageClient imageClient,
                      ToolRegistry toolRegistry) {
        this.visionService = visionService;
        this.wechatClient = wechatClient;
        this.contextStore = contextStore;
        this.imageClient = imageClient;
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

    @Override
    public WechatReply handle(WechatMessage message) {
        if (message.getType() == MessageType.IMAGE) {
            return analyzeImage(message);
        }

        if (message.getType() == MessageType.TEXT
                && contextStore.findLastByPrefix(message.getUserId(), "[图片]") != null) {
            log.info("分析上下文中的最近图片 | user={} | text={}", message.getUserId(), message.getText());
            return analyzeImage(message);
        }

        return null;
    }

    private WechatReply analyzeImage(WechatMessage message) {
        log.info("VisionTool 分析图片 | user={}", message.getUserId());

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

        contextStore.append(message.getUserId(), "user", "[用户发送了一张图片]");
        contextStore.append(message.getUserId(), "assistant", reply);

        return WechatReply.text(reply);
    }

    /**
     * 下载图片：消息 CDN → 上下文 CDN → 上下文 URL → 消息 URL
     */
    private String downloadImageAsDataUrl(WechatMessage message) {
        String encryptParam = message.getEncryptQueryParam();
        String aesKey = message.getAesKey();

        if (isEmpty(encryptParam) || isEmpty(aesKey)) {
            Message lastImage = contextStore.findLastByPrefix(message.getUserId(), "[图片]");
            if (lastImage != null && lastImage.hasMedia()) {
                encryptParam = lastImage.mediaEncryptParam();
                aesKey = lastImage.mediaAesKey();
            }
        }

        if (!isEmpty(encryptParam) && !isEmpty(aesKey)) {
            byte[] bytes = wechatClient.downloadMedia(encryptParam, aesKey);
            if (bytes != null && bytes.length > 0) {
                return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes);
            }
        }

        Message lastImage = contextStore.findLastByPrefix(message.getUserId(), "[图片]");
        if (lastImage != null && lastImage.mediaUrl() != null && !lastImage.mediaUrl().isEmpty()) {
            byte[] urlBytes = imageClient.downloadImage(lastImage.mediaUrl());
            if (urlBytes != null && urlBytes.length > 0) {
                return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(urlBytes);
            }
        }

        if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
            return message.getImageUrl();
        }

        return null;
    }

    /**
     * 从 AgentContext 中获取图片数据（供 Agent 调用）
     */
    private String resolveImage(AgentContext context) {
        WechatMessage raw = context.getRawMessage();
        if (raw == null || raw.getType() != MessageType.IMAGE) {
            return null;
        }
        return downloadImageAsDataUrl(raw);
    }

    private boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
