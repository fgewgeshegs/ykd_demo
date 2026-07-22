package com.youkeda.exercise.claw.agent.tool;

import com.youkeda.exercise.claw.agent.AgentContext;
import com.youkeda.exercise.claw.agent.classify.Intent;
import com.youkeda.exercise.claw.agent.memory.ContextStore;
import com.youkeda.exercise.claw.ai.file.FileGenerationService;
import com.youkeda.exercise.claw.ai.file.FileGenerationService.FileGenerationResult;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 文件生成工具
 *
 * 封装 FileGenerationService，根据用户请求生成 PDF 或 Word 文档文件。
 * 同时作为 Tool 和 WechatMessageHandler 暴露。
 * 启动时自动注册到 ToolRegistry。
 */
@Component
public class FileGenerationTool implements Tool, WechatMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(FileGenerationTool.class);
    private static final String FALLBACK_REPLY = "抱歉，文件生成失败，请稍后再试。";

    private final FileGenerationService fileGenerationService;
    private final ContextStore contextStore;
    private final ToolRegistry toolRegistry;

    public FileGenerationTool(FileGenerationService fileGenerationService,
                              ContextStore contextStore,
                              ToolRegistry toolRegistry) {
        this.fileGenerationService = fileGenerationService;
        this.contextStore = contextStore;
        this.toolRegistry = toolRegistry;
    }

    @PostConstruct
    public void init() {
        toolRegistry.register(this);
    }

    @Override
    public String name() {
        return "file_generate";
    }

    @Override
    public String description() {
        return "根据文字描述生成 PDF 或 Word 文档文件，支持总结对话、生成报告等";
    }

    @Override
    public Intent[] supportedIntents() {
        return new Intent[]{Intent.FILE_GENERATE};
    }

    @Override
    public String execute(AgentContext context) {
        log.info("FileGenerationTool 执行 | user={} | text={}", context.getUserId(), context.getMessage());

        FileGenerationResult result = fileGenerationService.generate(
                context.getUserId(), context.getMessage());
        if (result == null) {
            return FALLBACK_REPLY;
        }

        contextStore.append(context.getUserId(), "assistant",
                "[已生成文件: " + result.fileName() + "]");
        return "已为您生成文件：" + result.fileName();
    }

    @Override
    public WechatReply handle(WechatMessage message) {
        if (message.getType() != MessageType.TEXT) {
            return null;
        }

        log.info("FileGenerationTool.handle 处理消息 | from={} | text={}",
                message.getUserId(), message.getText());

        FileGenerationResult result = fileGenerationService.generate(
                message.getUserId(), message.getText());
        if (result == null) {
            log.warn("文件生成失败 | from={}", message.getUserId());
            return WechatReply.text(FALLBACK_REPLY);
        }

        contextStore.append(message.getUserId(), "assistant",
                "[已生成文件: " + result.fileName() + "]");

        log.info("文件生成成功 | fileName={} | size={} bytes",
                result.fileName(), result.fileBytes().length);

        return WechatReply.file(result.fileBytes(), result.fileName(), result.description());
    }
}
