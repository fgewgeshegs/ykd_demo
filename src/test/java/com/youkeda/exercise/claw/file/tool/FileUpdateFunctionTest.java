package com.youkeda.exercise.claw.file.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.ai.file.FileParseProperties;
import com.youkeda.exercise.claw.ai.file.FileParseService;
import com.youkeda.exercise.claw.agent.tool.FunctionExecutionContext;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import com.youkeda.exercise.claw.file.FileLocalStorage;
import com.youkeda.exercise.claw.file.FileMetadataRepository;
import com.youkeda.exercise.claw.file.FileService;
import com.youkeda.exercise.claw.file.entity.FileMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileUpdateFunctionTest {

    @TempDir
    Path tempDir;

    private FileUpdateFunction function;
    private FileService fileService;
    private ObjectMapper objectMapper;
    private String userId = "testUser";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        var parseProperties = new FileParseProperties();
        parseProperties.setMaxFileSize(10 * 1024 * 1024);
        parseProperties.setMaxTextLength(10000);
        parseProperties.setMaxEmbeddedImages(5);
        var fileParseService = new FileParseService(parseProperties);

        var metadataRepository = new FileMetadataRepository();
        setField(metadataRepository, "dbPath", new File(tempDir.toFile(), "claw-files.db").getAbsolutePath());
        metadataRepository.init();

        var fileLocalStorage = new FileLocalStorage();
        setField(fileLocalStorage, "filesRoot", tempDir.resolve("users").toFile().getAbsolutePath());
        setField(fileLocalStorage, "maxFileSize", 20 * 1024 * 1024L);

        fileService = new FileService(fileLocalStorage, metadataRepository, fileParseService, fileLocalStorage);

        var registry = new LLMFunctionRegistry();
        function = new FileUpdateFunction(fileService, registry, objectMapper);
        function.init();
    }

    @Test
    @DisplayName("重命名文件")
    void renameFile() throws Exception {
        FileMetadata meta = fileService.saveFile(userId, "content".getBytes(StandardCharsets.UTF_8), "oldname.md");

        String args = "{\"file_id\":" + meta.getId() + ",\"filename\":\"newname.md\"}";
        String result = function.execute(args, context(userId));
        JsonNode json = objectMapper.readTree(result);

        assertEquals("success", json.get("status").asText());
        assertEquals("newname.md", json.get("new_filename").asText());
    }

    @Test
    @DisplayName("不存在的文件返回错误")
    void nonExistentFile() throws Exception {
        String args = "{\"file_id\":99999,\"filename\":\"new.md\"}";
        String result = function.execute(args, context(userId));
        JsonNode json = objectMapper.readTree(result);
        assertEquals("error", json.get("status").asText());
    }

    @Test
    @DisplayName("缺少参数返回错误")
    void missingArgs() throws Exception {
        String args = "{}";
        String result = function.execute(args, context(userId));
        JsonNode json = objectMapper.readTree(result);
        assertEquals("error", json.get("status").asText());
    }

    private FunctionExecutionContext context(String userId) {
        return new FunctionExecutionContext("", null, userId);
    }

    private void setField(Object target, String name, Object value) {
        try {
            var f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}