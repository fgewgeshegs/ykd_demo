package com.youkeda.exercise.claw.tool.file;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.runtime.ToolExecutionContext;
import com.youkeda.exercise.claw.agent.runtime.Tool;
import com.youkeda.exercise.claw.agent.runtime.ToolRegistry;
import com.youkeda.exercise.claw.feature.file.FileService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 文件删除工具
 *
 * <p>LLM Function：{@code file_delete}
 *
 * <p>当用户说「删掉这个文件」「删除笔记」「移除」时，
 * Agent 自主调用此工具软删除用户文件。
 *
 * <p>纯文本入、JSON 字符串出，不走 pending-consumer 模式。
 * 删除为软删除（status = 'deleted'），数据可恢复。
 */
@Component
public class FileDeleteTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(FileDeleteTool.class);

    private final FileService fileService;
    private final ToolRegistry functionRegistry;
    private final ObjectMapper objectMapper;

    public FileDeleteTool(FileService fileService,
                              ToolRegistry functionRegistry,
                              ObjectMapper objectMapper) {
        this.fileService = fileService;
        this.functionRegistry = functionRegistry;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        functionRegistry.register(this);
        log.info("FileDeleteTool 已注册到 ToolRegistry");
    }

    @Override
    public String getName() {
        return "file_delete";
    }

    @Override
    public String getDescription() {
        return "删除用户已保存的文件（软删除）。当用户说「删掉」「删除」「移除」某个文件时调用。"
                + "需要文件 ID。如果不知道 ID，可先用 file_search 搜索获取。";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("type", "object");

        ObjectNode properties = params.putObject("properties");

        ObjectNode fileId = properties.putObject("file_id");
        fileId.put("type", "integer");
        fileId.put("description", "要删除的文件 ID。如果不知道 ID，可先用 file_search 搜索获取后再传入。");

        params.putArray("required").add("file_id");

        return params;
    }

    @Override
    public String execute(String argumentsJson) {
        return "{\"error\": \"缺少用户上下文\"}";
    }

    @Override
    public String execute(String argumentsJson, ToolExecutionContext context) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            String userId = context.userId();

            if (userId == null || userId.isBlank()) {
                return errorJson("缺少用户 ID");
            }

            long fileId = args.path("file_id").asLong(0);

            if (fileId <= 0) {
                return errorJson("请提供要删除的文件 ID（file_id 参数）");
            }

            log.info("file_delete 执行 | userId={} | fileId={}", userId, fileId);

            boolean deleted = fileService.deleteFile(userId, fileId);

            if (!deleted) {
                return errorJson("文件不存在或已被删除");
            }

            ObjectNode result = objectMapper.createObjectNode();
            result.put("status", "success");
            result.put("file_id", fileId);
            result.put("message", "文件已删除");

            log.info("file_delete 完成 | userId={} | fileId={}", userId, fileId);
            return objectMapper.writeValueAsString(result);

        } catch (Exception e) {
            log.error("file_delete 执行失败 | args={}", argumentsJson, e);
            return errorJson("文件删除失败：" + e.getMessage());
        }
    }

    private String errorJson(String message) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("status", "error");
            node.put("message", message);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return "{\"status\":\"error\",\"message\":\"" + message + "\"}";
        }
    }
}