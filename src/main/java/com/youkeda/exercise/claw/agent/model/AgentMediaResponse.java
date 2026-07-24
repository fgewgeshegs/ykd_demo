package com.youkeda.exercise.claw.agent.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Agent 媒体回复模型
 *
 * <p>描述 LLM 返回给用户的富媒体回复内容。
 * ChatTool 通过 {@link #tryParse(String)} 从 LLM 回复文本中提取媒体信息，
 * 不关心媒体来自哪个具体的 Function。
 *
 * <p>LLM 约定格式：
 * <pre>{@code
 * {"text": "文字介绍...", "images": ["url1", "url2"]}
 * {"text": "文字介绍...", "images": ["url1"], "url": "https://xxx"}
 * }</pre>
 *
 * <p>支持 markdown 代码块包裹（```json ... ```）。
 */
public class AgentMediaResponse {

    private final String text;
    private final List<String> images;
    private final String url;

    public AgentMediaResponse(String text, List<String> images, String url) {
        this.text = text;
        this.images = images != null ? Collections.unmodifiableList(images) : Collections.emptyList();
        this.url = (url != null && !url.isBlank()) ? url : null;
    }

    public String getText() {
        return text;
    }

    public List<String> getImages() {
        return images;
    }

    public String getUrl() {
        return url;
    }

    public boolean hasImages() {
        return !images.isEmpty();
    }

    public boolean hasUrl() {
        return url != null && !url.isEmpty();
    }

    /**
     * 尝试从 LLM 回复文本中解析媒体回复。
     *
     * <p>如果原始文本是 JSON 格式（含 text/images/url 字段），
     * 则解析为 AgentMediaResponse；否则返回 null 表示普通文本。
     *
     * <p>自动处理：
     * <ul>
     *   <li>纯 JSON：{@code {"text":"...","images":[...]}}</li>
     *   <li>markdown 代码块：{@code ```json ... ```}</li>
     *   <li>LLM 尾随文本：{@code {"text":"..."}<br>额外说明}</li>
     * </ul>
     *
     * @param rawReply LLM 原始回复文本
     * @return 解析成功返回 AgentMediaResponse，非媒体格式返回 null
     */
    public static AgentMediaResponse tryParse(String rawReply) {
        if (rawReply == null || rawReply.isEmpty()) {
            return null;
        }

        String trimmed = rawReply.trim();

        // 如果以 ``` 开头，提取代码块内容
        String jsonPart = extractJsonBlock(trimmed);
        if (jsonPart == null) {
            // 尝试直接取第一行 JSON（LLM 可能在 JSON 后追加了说明文字）
            jsonPart = extractLeadingJson(trimmed);
        }
        if (jsonPart == null) {
            return null;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(jsonPart);

            // 必须有 text 字段
            if (!root.has("text")) {
                return null;
            }

            String text = root.path("text").asText("");

            // 解析 images（可选）
            List<String> imageUrls = new ArrayList<>();
            if (root.has("images")) {
                JsonNode imagesNode = root.path("images");
                if (imagesNode.isArray()) {
                    for (JsonNode img : imagesNode) {
                        String imgUrl = img.asText("");
                        if (!imgUrl.isBlank()) {
                            imageUrls.add(imgUrl);
                        }
                    }
                }
            }

            // 解析 url（可选）
            String url = root.has("url") ? root.path("url").asText("") : null;
            if (url != null && url.isBlank()) {
                url = null;
            }

            // 至少 text / images / url 之一有效
            if (text.isEmpty() && imageUrls.isEmpty() && url == null) {
                return null;
            }

            return new AgentMediaResponse(text, imageUrls, url);

        } catch (Exception e) {
            // 解析失败 → 普通文本
            return null;
        }
    }

    /**
     * 提取 markdown 代码块中的 JSON 内容。
     * 支持 ```json ... ``` 和 ``` ... ``` 两种形式。
     */
    private static String extractJsonBlock(String text) {
        if (!text.startsWith("```")) {
            return null;
        }
        // 找到第一个换行后的内容
        int start = text.indexOf('\n');
        if (start < 0) {
            return null;
        }
        // 找到闭合的 ```
        int end = text.indexOf("```", start + 1);
        if (end < 0) {
            return null;
        }
        String inner = text.substring(start + 1, end).trim();
        if (inner.startsWith("{")) {
            return inner;
        }
        return null;
    }

    /**
     * 提取文本开头的 JSON 对象（处理 LLM 在 JSON 后追加文字的情况）。
     */
    private static String extractLeadingJson(String text) {
        if (!text.startsWith("{")) {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++; // skip escaped char
                } else if (c == '"') {
                    inString = false;
                }
            } else {
                switch (c) {
                    case '"':
                        inString = true;
                        break;
                    case '{':
                        depth++;
                        break;
                    case '}':
                        depth--;
                        if (depth == 0) {
                            // 找到完整 JSON 对象
                            return text.substring(0, i + 1);
                        }
                        break;
                }
            }
        }
        // 整个文本就是一个 JSON 对象
        if (depth == 0 && text.endsWith("}")) {
            return text;
        }
        return null;
    }
}