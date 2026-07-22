package com.youkeda.exercise.claw.agent.tool;

import com.youkeda.exercise.claw.agent.AgentContext;
import com.youkeda.exercise.claw.agent.classify.Intent;
import com.youkeda.exercise.claw.agent.memory.ContextStore;
import com.youkeda.exercise.claw.agent.memory.Message;
import com.youkeda.exercise.claw.ai.image.ImageGenerationService;
import com.youkeda.exercise.claw.ai.llm.ImageClient;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 图片生成工具
 *
 * 封装 ImageGenerationService，结合 LLM 上下文理解，同时作为 Tool 和 WechatMessageHandler 暴露。
 * 启动时自动注册到 ToolRegistry。
 */
@Component
public class ImageGenerationTool implements Tool, WechatMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationTool.class);
    private static final String FALLBACK_REPLY = "抱歉，图片生成失败，请稍后再试。";
    private static final int MAX_HISTORY = 20;

    /** 上下文扩写提示词：结合历史将简短请求展开为完整的图片描述 */
    private static final String ENRICH_PROMPT =
            "你是一个图片描述助手。结合对话历史，将用户最新的需求扩写成一个完整、详细、" +
            "适合图片生成模型（如 DALL-E、Qwen-Image）的图片描述。" +
            "保留用户指定的风格、元素、颜色等细节。" +
            "直接输出扩写后的描述，不要任何解释。";

    private final ImageGenerationService imageGenerationService;
    private final ImageClient imageClient;
    private final ContextStore contextStore;
    private final LLMClient llmClient;
    private final ToolRegistry toolRegistry;

    public ImageGenerationTool(ImageGenerationService imageGenerationService,
                                ImageClient imageClient,
                                ContextStore contextStore,
                                LLMClient llmClient,
                                ToolRegistry toolRegistry) {
        this.imageGenerationService = imageGenerationService;
        this.imageClient = imageClient;
        this.contextStore = contextStore;
        this.llmClient = llmClient;
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
        return "根据文字描述生成图片，调用 qwen-image-2.0 模型";
    }

    @Override
    public Intent[] supportedIntents() {
        return new Intent[]{Intent.IMAGE_GENERATE};
    }

    @Override
    public String execute(AgentContext context) {
        log.info("ImageGenerationTool 执行 | user={} | prompt={}",
                context.getUserId(), context.getMessage());

        String fullPrompt = enrichPrompt(context.getUserId(), context.getMessage());
        String imageUrl = imageGenerationService.generate(fullPrompt);
        if (imageUrl == null) {
            return FALLBACK_REPLY;
        }

        contextStore.append(context.getUserId(), "assistant", "[图片]", null, null, imageUrl);
        return "已为您生成图片：" + imageUrl;
    }

    @Override
    public WechatReply handle(WechatMessage message) {
        log.info("ImageGenerationTool.handle 处理消息 | from={} | text={}", message.getUserId(), message.getText());

        // 1. 用 LLM（带历史）将简短请求展开为完整图片描述
        String fullPrompt = enrichPrompt(message.getUserId(), message.getText());

        // 2. 生成图片，拿到 URL
        String imageUrl = imageGenerationService.generate(fullPrompt);
        if (imageUrl == null) {
            log.warn("图片生成失败");
            return WechatReply.text(FALLBACK_REPLY);
        }

        // 3. 记住 AI 图片 URL（后续描述/编辑可从此下载）
        contextStore.updateLastMediaUrl(message.getUserId(), "[图片]", imageUrl);

        // 4. 下载图片字节
        byte[] imageBytes = imageClient.downloadImage(imageUrl);
        if (imageBytes == null || imageBytes.length == 0) {
            log.warn("图片下载失败 | url={}", imageUrl);
            return WechatReply.text(FALLBACK_REPLY);
        }

        log.info("图片生成并下载成功 | size={} bytes", imageBytes.length);

        // 5. 保存 [图片] 到上下文
        contextStore.append(message.getUserId(), "assistant", "[图片]", null, null, imageUrl);

        return WechatReply.image(imageBytes);
    }

    /**
     * 用 LLM（带对话历史）将用户请求扩写为完整的图片描述
     */
    private String enrichPrompt(String userId, String userText) {
        if (userText.length() >= 15) {
            return userText;
        }

        List<Message> history = contextStore.getHistory(userId, MAX_HISTORY);
        if (history == null || history.isEmpty()) {
            return userText;
        }

        log.info("扩写图片提示词 | user={} | historySize={}", userId, history.size());
        String enriched = llmClient.chatWithSystemPrompt(ENRICH_PROMPT, userText, history);
        if (enriched == null || enriched.trim().isEmpty()) {
            log.warn("上下文扩写失败，使用原始文本");
            return userText;
        }

        log.info("图片提示词扩写完成 | original={} | enriched={}", userText, enriched);
        return enriched.trim();
    }
}
