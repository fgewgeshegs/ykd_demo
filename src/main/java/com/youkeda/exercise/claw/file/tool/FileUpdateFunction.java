package com.youkeda.exercise.claw.file.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.tool.FunctionExecutionContext;
import com.youkeda.exercise.claw.agent.tool.LLMFunction;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import com.youkeda.exercise.claw.file.FileService;
import com.youkeda.exercise.claw.file.entity.FileMetadata;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 文件更新工具
 *
 * <p>LLM Function：{@code file_update}
 *
 * <p>当用户说「重命名」「改个名字」「把 XX 改成 YY」时，
 * Agent 自主调用此工具修改文件名。
 *
 * <p>纯文本入、JSON 字符串出，不走 pending-consumer 模式。
 */
@Component
public class FileUpdateFunction implements LLMFunction {

    private static final Logger log = LoggerFactory.getLogger(FileUpdateFunction.class);

    private final FileService fileService;
    private final LLMFunctionRegistry functionRegistry;
    private final ObjectMapper objectMapper;

    public FileUpdateFunction(FileService fileService,
                              LLMFunctionRegistry functionRegistry,
                              ObjectMapper objectMapper) {
        this.fileService = fileService;
        this.functionRegistry = functionRegistry;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        functionRegistry.register(this);
        log.info("FileUpdateFunction 已注册到 LLMFunctionRegistry");
    }

    @Override
    public String getName() {
        return "file_update";
    }

    @Override
    public String getDescription() {
        return "更新已保存文件的名称。当用户说「重命名」「改个名字」「把 XX 改成 YY」时调用。"
                + "需要文件 ID 和新文件名。仅修改显示名，不影响存储文件。";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("type", "object");

        ObjectNode properties = params.putObject("properties");

        ObjectNode fileId = properties.putObject("file_id");
        fileId.put("type", "integer");
        fileId.put("description", "要修改的文件 ID。如果不知道 ID，可先用 file_search 搜索获取。");

        ObjectNode filename = properties.putObject("filename");
        filename.put("type", "string");
        filename.put("description", "新的文件名，需包含扩展名，如「Java面试题整理 v2.md」");

        params.putArray("required").add("file_id").add("filename");

        return params;
    }

    @Override
    public String execute(String argumentsJson) {
        return "{\"error\": \"缺少用户上下文\"}";
    }

    @Override
    public String execute(String argumentsJson, FunctionExecutionContext context) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            String userId = context.userId();

            if (userId == null || userId.isBlank()) {
                return errorJson("缺少用户 ID");
            }

            long fileId = args.path("file_id").asLong(0);
            String newFilename = args.path("filename").asText("");

            if (fileId <= 0) {
                return errorJson("请提供要修改的文件 ID（file_id 参数）");
            }
            if (newFilename.isBlank()) {
                return errorJson("请提供新的文件名（filename 参数）");
            }

            log.info("file_update 执行 | userId={} | fileId={} | newFilename={}",
                    userId, fileId, newFilename);

            FileMetadata updated = fileService.updateFileName(userId, fileId, newFilename);
            if (updated == null) {
                return errorJson("文件不存在或已被删除");
            }

            ObjectNode result = objectMapper.createObjectNode();
            result.put("status", "success");
            result.put("file_id", updated.getId());
            result.put("old_filename", args.path("filename").asText(""));
            result.put("new_filename", updated.getFilename());
            result.put("message", "文件名已更新为「" + updated.getFilename() + "」");

            log.info("file_update 完成 | userId={} | fileId={} | newFilename={}",
                    userId, fileId, newFilename);
            return objectMapper.writeValueAsString(result);

        } catch (IllegalArgumentException e) {
            log.warn("file_update 参数错误 | args={}", argumentsJson, e);
            return errorJson(e.getMessage());
        } catch (Exception e) {
            log.error("file_update 执行失败 | args={}", argumentsJson, e);
            return errorJson("文件更新失败：" + e.getMessage());
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