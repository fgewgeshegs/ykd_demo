package com.youkeda.exercise.claw.agent.tool;

import com.youkeda.exercise.claw.ai.file.FileParseService;
import com.youkeda.exercise.claw.ai.chat.ChatService;
import com.youkeda.exercise.claw.ai.vision.VisionService;
import com.youkeda.exercise.claw.agent.memory.ContextStore;
import com.youkeda.exercise.claw.file.FileService;
import com.youkeda.exercise.claw.wechat.client.WechatILinkClient;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;

/**
 * 文件消息处理器
 *
 * 职责：处理 FILE 类型的微信消息，根据文件内容类型三路分发：
 * - 图片文件（jpg/png/gif/bmp/webp）→ VisionService 多模态分析
 * - 文档文件（PDF/DOCX/PPTX/XLSX/TXT）→ Tika 文本提取 + 内嵌图片分析 → LLM 分析
 * - 不支持的格式 → 返回格式错误提示
 *
 * 实现 WechatMessageHandler 而非 Tool，因 FILE 是消息类型而非意图。
 */
@Component
public class FileTool implements WechatMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(FileTool.class);

    private static final String FALLBACK_REPLY = "抱歉，我暂时无法处理这个文件，请稍后再试。";
    private static final String UNSUPPORTED_FORMAT_REPLY =
            "抱歉，我无法解析这个文件的内容。请确认文件是 PDF、Word 文档、Excel、PPT 或纯文本格式。";

    private final WechatILinkClient wechatClient;
    private final FileParseService fileParseService;
    private final VisionService visionService;
    private final ContextStore contextStore;
    private final ChatService chatService;
    private final FileService fileService;

    public FileTool(WechatILinkClient wechatClient,
                    FileParseService fileParseService,
                    VisionService visionService,
                    ContextStore contextStore,
                    ChatService chatService,
                    FileService fileService) {
        this.wechatClient = wechatClient;
        this.fileParseService = fileParseService;
        this.visionService = visionService;
        this.contextStore = contextStore;
        this.chatService = chatService;
        this.fileService = fileService;
    }

    @Override
    public WechatReply handle(WechatMessage message) {
        if (message.getType() != MessageType.FILE) {
            return null;
        }

        String fileName = message.getFileName() != null ? message.getFileName() : "未知文件";
        log.info("FileTool 处理文件 | from={} | fileName={}", message.getUserId(), fileName);

        // 1. 下载文件字节
        byte[] fileBytes = downloadFile(message);
        if (fileBytes == null || fileBytes.length == 0) {
            log.warn("文件下载失败 | from={} | fileName={}", message.getUserId(), fileName);
            return WechatReply.text(FALLBACK_REPLY);
        }

        log.info("文件下载成功 | fileName={} | size={} bytes", fileName, fileBytes.length);

        // 2. 检测 MIME 类型（基于 magic bytes）
        String mimeType = fileParseService.detectMimeType(fileBytes);
        log.info("文件 MIME 类型 | fileName={} | mimeType={}", fileName, mimeType);

        // 3. 尝试自动保存到用户文件库（保存失败不影响后续分析）
        trySaveFile(message.getUserId(), fileBytes, fileName);

        // 4. 按内容类型分发
        if (mimeType.startsWith("image/")) {
            return handleImageFile(fileBytes, mimeType, fileName);
        } else {
            return handleDocumentFile(fileBytes, fileName);
        }
    }

    /**
     * 分支 A：图片文件 → VisionService 多模态分析
     */
    private WechatReply handleImageFile(byte[] fileBytes, String mimeType, String fileName) {
        log.info("FileTool 图片文件路径 | fileName={}", fileName);

        // 转 base64 data URL
        String base64 = Base64.getEncoder().encodeToString(fileBytes);
        String dataUrl = "data:" + mimeType + ";base64," + base64;

        // 调用视觉模型分析
        String description = visionService.analyze(dataUrl, "请详细描述这张图片的内容");
        if (description == null || description.isEmpty()) {
            log.warn("图片分析失败 | fileName={}", fileName);
            return WechatReply.text("抱歉，我无法分析这张图片的内容。");
        }

        // 保存到上下文（供后续追问参考）
        contextStore.append("user", "[文件图片: " + fileName + "]\n" + description);

        log.info("FileTool 图片分析完成 | fileName={}", fileName);
        return WechatReply.text(description);
    }

    /**
     * 分支 B：文档文件 → Tika 文本提取 + 内嵌图片分析 → LLM 分析
     */
    private WechatReply handleDocumentFile(byte[] fileBytes, String fileName) {
        log.info("FileTool 文档文件路径 | fileName={}", fileName);

        // 解析文件：提取文本和内嵌图片
        FileParseService.FileParseResult result = fileParseService.parse(fileBytes, fileName);
        if (result == null) {
            // 文件过大或无法解析
            if (fileBytes.length > 10 * 1024 * 1024) {
                return WechatReply.text("文件过大，请发送小于 10MB 的文件。");
            }
            return WechatReply.text(UNSUPPORTED_FORMAT_REPLY);
        }

        // 构建上下文内容：文本 + 内嵌图片描述
        StringBuilder contextContent = new StringBuilder();
        contextContent.append("用户发送了文件《").append(fileName).append("》，以下是文件内容：\n");
        contextContent.append(result.text());

        // 处理内嵌图片：前 N 张送 VisionService 分析
        List<FileParseService.EmbeddedImage> images = result.images();
        int analyzedCount = Math.min(images.size(), 3);
        for (int i = 0; i < analyzedCount; i++) {
            FileParseService.EmbeddedImage img = images.get(i);
            String imgBase64 = Base64.getEncoder().encodeToString(img.data());
            String imgDataUrl = "data:" + img.mimeType() + ";base64," + imgBase64;
            String imgDescription = visionService.analyze(imgDataUrl, "请描述这张文档内嵌图片的内容");
            if (imgDescription != null) {
                contextContent.append("\n\n[文档中内嵌图片").append(i + 1).append("]: ").append(imgDescription);
            }
        }

        // 保存到上下文（供后续追问参考）
        contextStore.append("user", contextContent.toString());

        // 调用 ChatService（带历史上下文）分析文件
        String analysis = chatService.chat(
                "请分析用户刚刚发送的文件《" + fileName + "》的内容，给出总结和关键信息。");

        if (analysis == null || analysis.isEmpty()) {
            log.warn("文件分析 LLM 返回空");
            return WechatReply.text(FALLBACK_REPLY);
        }

        log.info("FileTool 文档分析完成 | fileName={}", fileName);
        return WechatReply.text(analysis);
    }

    /**
     * 尝试将文件自动保存到用户文件库
     *
     * <p>保存失败仅记录日志，不抛出异常，不打断已有分析流程。
     * 仅支持白名单内的文件类型（md/txt/pdf/docx）。
     */
    private void trySaveFile(String userId, byte[] fileBytes, String fileName) {
        try {
            fileService.saveFile(userId, fileBytes, fileName);
            log.info("文件已自动保存到知识库 | userId={} | fileName={}", userId, fileName);
        } catch (IllegalArgumentException e) {
            // 不支持的文件类型或超出大小限制——这是预期行为，记录 debug 级别
            log.debug("文件自动保存跳过 | userId={} | fileName={} | reason={}", userId, fileName, e.getMessage());
        } catch (Exception e) {
            // 保存异常——不影响主流程，记录 warn 级别
            log.warn("文件自动保存失败 | userId={} | fileName={} | error={}", userId, fileName, e.getMessage());
        }
    }

    /**
     * 从微信 CDN 下载文件
     */
    private byte[] downloadFile(WechatMessage message) {
        String encryptParam = message.getFileEncryptQueryParam();
        String aesKey = message.getFileAesKey();

        if (encryptParam == null || encryptParam.isEmpty()
                || aesKey == null || aesKey.isEmpty()) {
            log.warn("文件 CDN 参数不完整 | from={}", message.getUserId());
            return null;
        }

        return wechatClient.downloadMedia(encryptParam, aesKey);
    }
}
