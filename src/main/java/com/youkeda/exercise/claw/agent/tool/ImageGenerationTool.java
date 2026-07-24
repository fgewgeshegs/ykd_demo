package com.youkeda.exercise.claw.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.ai.image.ImageGenerationService;
import com.youkeda.exercise.claw.ai.llm.ImageClient;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 图片生成工具
 *
 * <p>封装 ImageGenerationService，结合 LLM 上下文理解。作为 LLMFunction 暴露，
 * 启动时自动注册到 LLMFunctionRegistry。
 *
 * <p>注意：{@link LLMFunction#execute(String)} 只能返回文本，但图片数据通过
 * {@link #consumePendingImage()} 传递回调用方（{@code ChatTool}），确保图片能被正确发送。</p>
 */
@Component
public class ImageGenerationTool implements LLMFunction {

    private static final Logger log = LoggerFactory.getLogger(ImageGenerationTool.class);

    private final ImageGenerationService imageGenerationService;
    private final ImageClient imageClient;
    private final LLMFunctionRegistry llmFunctionRegistry;
    private final ObjectMapper objectMapper;

    /** 待发送的图片数据（单线程 WeChat 轮询，一次只处理一条消息，用实例字段足够） */
    private volatile PendingImage pendingImage;

    public ImageGenerationTool(ImageGenerationService imageGenerationService,
                                ImageClient imageClient,
                                LLMFunctionRegistry llmFunctionRegistry,
                                ObjectMapper objectMapper) {
        this.imageGenerationService = imageGenerationService;
        this.imageClient = imageClient;
        this.llmFunctionRegistry = llmFunctionRegistry;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        llmFunctionRegistry.register(this);
    }

    /**
     * 消费待发送的图片数据
     * <p>被 {@code ChatTool} 在工具调用循环结束后调用，如果存在则发送图片而非纯文本。</p>
     *
     * @return 待发送的图片数据，没有则返回 null
     */
    public PendingImage consumePendingImage() {
        PendingImage image = pendingImage;
        pendingImage = null;
        return image;
    }

    /** 图片生成结果暂存：图片字节 + 描述文本 */
    public record PendingImage(byte[] imageBytes, String description) {}

    // ==================== LLMFunction ====================

    @Override
    public String getName() {
        return "image_generate";
    }

    @Override
    public String getDescription() {
        return "根据文字描述生成图片，支持各种风格，如写实、卡通、水墨画等。用户要求画图、生成图片时调用此工具";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("type", "object");

        ObjectNode properties = params.putObject("properties");
        ObjectNode prompt = properties.putObject("prompt");
        prompt.put("type", "string");
        prompt.put("description", "图片内容描述，越详细越好，包含主体、场景、风格、色彩等");

        ObjectNode style = properties.putObject("style");
        style.put("type", "string");
        style.put("description", "图片风格，可选：写实、卡通、水墨、油画、素描等");
        style.put("enum", objectMapper.createArrayNode()
                .add("写实").add("卡通").add("水墨").add("油画").add("素描"));

        params.putArray("required").add("prompt");

        return params;
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            JsonNode promptNode = args.get("prompt");
            if (promptNode == null) {
                return "{\"error\": \"缺少必填参数: prompt\"}";
            }

            String prompt = promptNode.asText();
            log.info("ImageGenerationTool LLM调用 | prompt={}", prompt);

            String imageUrl = imageGenerationService.generate(prompt);
            if (imageUrl == null) {
                return "{\"error\": \"图片生成失败\"}";
            }

            // 下载图片字节
            byte[] imageBytes = imageClient.downloadImage(imageUrl);
            if (imageBytes == null || imageBytes.length == 0) {
                log.warn("图片下载失败 | url={}", imageUrl);
                return "{\"error\": \"图片下载失败\"}";
            }

            log.info("图片生成并下载成功 | size={} bytes", imageBytes.length);

            // 暂存图片供 ChatTool 取走发送（execute() 只能返回文本，图片通过此通道传递）
            pendingImage = new PendingImage(imageBytes, prompt);

            return "{\"imageUrl\": \"" + imageUrl
                    + "\", \"size\": " + imageBytes.length
                    + ", \"description\": \"已为您生成图片\"}";

        } catch (Exception e) {
            log.error("ImageGenerationTool LLM执行失败 | args={} | error={}", argumentsJson, e.getMessage());
            return "{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }
}