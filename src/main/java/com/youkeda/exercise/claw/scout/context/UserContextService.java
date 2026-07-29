package com.youkeda.exercise.claw.scout.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.agent.memory.longterm.LongTermMemoryService;
import com.youkeda.exercise.claw.agent.memory.longterm.MemoryItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户画像服务
 *
 * 从长期记忆中提取用户兴趣、项目、技术栈、目标
 */
@Service
public class UserContextService {

    private static final Logger log = LoggerFactory.getLogger(UserContextService.class);

    private static final String SYSTEM_PROMPT = """
            你是用户画像分析专家。根据用户的长期记忆，提取结构化画像信息。

            提取维度：
            - interests: 用户感兴趣的领域（如 AI、Java、创业等）
            - currentProjects: 用户当前在做的项目
            - techStack: 用户使用的技术栈
            - goals: 用户的学习或职业目标

            要求：
            1. 只从记忆中提取，不要编造
            2. 每个维度最多 5 项
            3. 每项用简短的中文描述
            4. 返回严格的 JSON 格式

            返回格式：
            {
              "interests": ["AI Agent", "Java"],
              "currentProjects": ["微信ClawBot"],
              "techStack": ["Spring Boot", "Qdrant"],
              "goals": ["掌握AI Agent开发"]
            }
            """;

    private final LongTermMemoryService memoryService;
    private final LLMClient llmClient;
    private final ObjectMapper objectMapper;

    public UserContextService(LongTermMemoryService memoryService,
                              LLMClient llmClient,
                              ObjectMapper objectMapper) {
        this.memoryService = memoryService;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 从长期记忆构建用户画像
     */
    public UserProfile buildProfile() {
        // 1. 获取用户所有长期记忆
        List<MemoryItem> memories = memoryService.listAll();
        if (memories.isEmpty()) {
            log.info("用户无长期记忆，返回空画像");
            return new UserProfile(List.of(), List.of(), List.of(), List.of(), "");
        }

        // 2. 拼接记忆文本
        String memoryText = formatMemories(memories);

        // 3. LLM 提取画像
        try {
            String prompt = "以下是用户的长期记忆：\n\n" + memoryText;
            String json = llmClient.chatWithSystemPrompt(SYSTEM_PROMPT, prompt);
            if (json == null || json.isBlank()) {
                log.warn("LLM 返回空，使用默认画像");
                return fallbackProfile(memories);
            }

            return parseProfile(json);
        } catch (Exception e) {
            log.error("用户画像提取失败，使用降级方案", e);
            return fallbackProfile(memories);
        }
    }

    private String formatMemories(List<MemoryItem> memories) {
        StringBuilder sb = new StringBuilder();
        for (MemoryItem m : memories) {
            sb.append("- [").append(m.category()).append("] ");
            if (m.topicKey() != null && !m.topicKey().isBlank()) {
                sb.append(m.topicKey()).append(": ");
            }
            sb.append(m.content()).append("\n");
        }
        return sb.toString();
    }

    private UserProfile parseProfile(String json) {
        try {
            // 提取 JSON 部分（LLM 可能返回 markdown 包裹的 JSON）
            String jsonStr = extractJson(json);
            JsonNode root = objectMapper.readTree(jsonStr);

            List<String> interests = parseStringArray(root, "interests");
            List<String> projects = parseStringArray(root, "currentProjects");
            List<String> techStack = parseStringArray(root, "techStack");
            List<String> goals = parseStringArray(root, "goals");

            String summary = "兴趣：" + String.join("、", interests)
                    + "；项目：" + String.join("、", projects)
                    + "；技术：" + String.join("、", techStack);

            log.info("用户画像提取成功 | interests={} | projects={}", interests.size(), projects.size());

            return new UserProfile(interests, projects, techStack, goals, summary);
        } catch (Exception e) {
            log.error("画像 JSON 解析失败 | json={}", json, e);
            return new UserProfile(List.of(), List.of(), List.of(), List.of(), "");
        }
    }

    private UserProfile fallbackProfile(List<MemoryItem> memories) {
        // 降级方案：直接从记忆内容中提取关键词
        List<String> interests = new ArrayList<>();
        for (MemoryItem m : memories) {
            if (m.content().length() < 50) {
                interests.add(m.content());
            }
            if (interests.size() >= 5) break;
        }
        return new UserProfile(interests, List.of(), List.of(), List.of(), "");
    }

    private List<String> parseStringArray(JsonNode root, String field) {
        List<String> result = new ArrayList<>();
        JsonNode arr = root.get(field);
        if (arr != null && arr.isArray()) {
            for (JsonNode item : arr) {
                String text = item.asText("").trim();
                if (!text.isEmpty()) {
                    result.add(text);
                }
            }
        }
        return result;
    }

    private String extractJson(String text) {
        // 处理 LLM 返回的 markdown 包裹的 JSON
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return trimmed.substring(start, end + 1);
            }
        }
        return trimmed;
    }
}
