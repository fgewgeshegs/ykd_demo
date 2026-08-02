package com.youkeda.exercise.claw.tool.skill;
import com.youkeda.exercise.claw.ai.retrieval.SkillKnowledgeStore;
import com.youkeda.exercise.claw.ai.retrieval.DocumentChunker;
import com.youkeda.exercise.claw.ai.retrieval.SkillKnowledgeChunk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.memory.longterm.EmbeddingClient;
import com.youkeda.exercise.claw.agent.runtime.AbstractTool;
import com.youkeda.exercise.claw.agent.runtime.ToolExecutionContext;
import com.youkeda.exercise.claw.agent.runtime.ToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class SkillKnowledgeManageTool extends AbstractTool {

    private static final Logger log = LoggerFactory.getLogger(SkillKnowledgeManageTool.class);

    private final SkillKnowledgeStore knowledgeStore;
    private final EmbeddingClient embeddingClient;
    private final DocumentChunker chunker;

    public SkillKnowledgeManageTool(ToolRegistry registry,
                                        SkillKnowledgeStore knowledgeStore,
                                        EmbeddingClient embeddingClient,
                                        DocumentChunker chunker,
                                        ObjectMapper objectMapper) {
        super(registry, objectMapper);
        this.knowledgeStore = knowledgeStore;
        this.embeddingClient = embeddingClient;
        this.chunker = chunker;
    }

    @Override
    public String getName() {
        return "skill_knowledge_manage";
    }

    @Override
    public String getDescription() {
        return "管理技能知识库：导入文档、列出文档、删除文档、重建索引、查看状态。actions: import(导入文本), list_documents, delete_document, reindex, status";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode action = objectMapper.createObjectNode();
        action.put("type", "string");
        action.put("description", "操作类型: import, list_documents, delete_document, reindex, status");
        action.putArray("enum").add("import").add("list_documents").add("delete_document").add("reindex").add("status");

        return schema()
                .raw("action", action, true)
                .string("skillName", "目标技能名称", true)
                .string("content", "要导入的文本内容（import时必填）")
                .string("documentId", "文档ID（delete_document时必填）")
                .string("source", "文档来源（import时可选）")
                .build();
    }

    @Override
    public String execute(String argumentsJson, ToolExecutionContext context) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            String action = args.get("action").asText();
            String skillName = args.get("skillName").asText();
            return switch (action) {
                case "import" -> handleImport(args, skillName);
                case "list_documents" -> "{\"status\":\"list not supported via tool\",\"skillName\":\"" + skillName + "\"}";
                case "delete_document" -> handleDeleteDocument(args);
                case "reindex" -> "{\"status\":\"reindex not implemented\",\"skillName\":\"" + skillName + "\"}";
                case "status" -> "{\"status\":\"OK\",\"skillName\":\"" + skillName + "\",\"collection\":\"skill_knowledge\"}";
                default -> "{\"error\":\"unknown action: " + action + "\"}";
            };
        } catch (Exception e) {
            log.error("skill_knowledge_manage failed", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    private String handleImport(JsonNode args, String skillName) {
        if (!args.has("content")) return "{\"error\":\"content is required for import\"}";
        String content = args.get("content").asText();
        String source = args.has("source") ? args.get("source").asText() : "manual";
        String documentId = UUID.randomUUID().toString();
        List<DocumentChunker.Chunk> chunks = chunker.chunkPlainText(content, documentId, source);
        int successCount = 0;

        for (DocumentChunker.Chunk chunk : chunks) {
            try {
                float[] vector = embeddingClient.embed(chunk.content());
                SkillKnowledgeChunk kc = new SkillKnowledgeChunk(
                    UUID.randomUUID().toString(), skillName, chunk.documentId(),
                    chunk.chunkIndex(), chunk.content(), chunk.source(), null, "1.0");
                knowledgeStore.upsert(kc, vector);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to import chunk {}:{}", documentId, chunk.chunkIndex(), e);
            }
        }
        return String.format(
            "{\"status\":\"imported\",\"documentId\":\"%s\",\"chunks\":%d,\"successCount\":%d}",
            documentId, chunks.size(), successCount);
    }

    private String handleDeleteDocument(JsonNode args) {
        if (!args.has("documentId")) return "{\"error\":\"documentId is required\"}";
        knowledgeStore.deleteByDocument(args.get("documentId").asText());
        return "{\"status\":\"deleted\",\"documentId\":\"" + args.get("documentId").asText() + "\"}";
    }
}
