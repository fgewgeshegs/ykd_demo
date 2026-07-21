package com.youkeda.exercise.claw.wechat.handler;

import com.youkeda.exercise.claw.ai.image.ImageGenerationService;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.ai.llm.ImageClient;
import com.youkeda.exercise.claw.context.ContextStore;
import com.youkeda.exercise.claw.context.Message;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 图片生成处理器
 *
 * 职责：接收 IMAGE_GENERATE 意图的消息，生成图片并以下载的字节数据返回，
 *       由上游 WechatMessageService 负责上传 CDN 和发送图片消息。
 *       流程：生成 URL → ImageClient 下载字节 → 返回 WechatReply.image
 */
@Slf4j
@Component
public class ImageGenerationHandler implements MessageHandler {

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

    public ImageGenerationHandler(ImageGenerationService imageGenerationService,
                                  ImageClient imageClient,
                                  ContextStore contextStore,
                                  LLMClient llmClient) {
        this.imageGenerationService = imageGenerationService;
        this.imageClient = imageClient;
        this.contextStore = contextStore;
        this.llmClient = llmClient;
    }

    @Override
    public WechatReply handle(WechatMessage message) {
        log.info("ImageGenerationHandler 处理消息 | from={} | text={}", message.getUserId(), message.getText());

        // 1. 用 LLM（带历史）将简短请求展开为完整图片描述
        String fullPrompt = enrichPrompt(message.getUserId(), message.getText());

        // 2. 生成图片，拿到 URL
        String imageUrl = imageGenerationService.generate(fullPrompt);
        if (imageUrl == null) {
            log.warn("ImageGenerationHandler 图片生成失败");
            return WechatReply.text(FALLBACK_REPLY);
        }

        // 2. 记住 AI 图片 URL（存入上下文，后续可下载分析）
        contextStore.append(message.getUserId(), "assistant", "[AI生成图片]", null, null, imageUrl);

        // 4. 下载图片字节
        byte[] imageBytes = imageClient.downloadImage(imageUrl);
        if (imageBytes == null || imageBytes.length == 0) {
            log.warn("ImageGenerationHandler 图片下载失败 | url={}", imageUrl);
            return WechatReply.text(FALLBACK_REPLY);
        }

        log.info("ImageGenerationHandler 图片生成并下载成功 | size={} bytes", imageBytes.length);

        // 5. 保存 [图片] 到上下文，供后续 VisionHandler 分析 / 再次编辑时查找
        contextStore.append(message.getUserId(), "assistant", "[图片]", null, null, imageUrl);

        return WechatReply.image(imageBytes);
    }

    /**
     * 用 LLM（带对话历史）将用户请求扩写为完整的图片描述
     *
     * 当用户只说"换个卡通风"这类简短指令时，LLM 结合历史能理解要改的是上一张图，
     * 输出完整的图片描述，确保图片生成模型不会丢失前文信息。
     */
    private String enrichPrompt(String userId, String userText) {
        // 用户输入足够详细时直接使用
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
