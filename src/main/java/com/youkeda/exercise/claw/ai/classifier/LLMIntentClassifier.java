package com.youkeda.exercise.claw.ai.classifier;

import com.youkeda.exercise.claw.ai.llm.LLMClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 基于 LLM 的意图分类器实现
 *
 * 调用已有 LLMClient，通过特定分类 Prompt 判断用户意图
 */
@Slf4j
@Component
public class LLMIntentClassifier implements IntentClassifier {

    /**
     * 意图分类系统提示词
     * <p>
     * 要求 LLM 严格返回 JSON 格式的分类结果
     */
    private static final String CLASSIFICATION_PROMPT =
            "你是一个意图分类器。\n" +
            "判断用户输入属于：\n" +
            "\n" +
            "CHAT:\n" +
            "普通聊天\n" +
            "\n" +
            "IMAGE_GENERATE:\n" +
            "生成图片\n" +
            "\n" +
            "IMAGE_ANALYZE:\n" +
            "分析图片\n" +
            "\n" +
            "只返回JSON格式，不要任何其他内容：\n" +
            "{\"intent\":\"CHAT\"}";

    private final LLMClient llmClient;

    public LLMIntentClassifier(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public Intent classify(String message) {
        log.info("开始意图分类 | message={}", message);

        try {
            // 调用 LLM，使用分类专用系统提示词
            String rawResult = llmClient.chatWithSystemPrompt(CLASSIFICATION_PROMPT, message);

            if (rawResult == null || rawResult.isEmpty()) {
                log.warn("意图分类返回为空，默认归类为 CHAT");
                return Intent.CHAT;
            }

            // 解析 LLM 返回的 JSON
            Intent intent = parseIntent(rawResult.trim());
            log.info("意图分类完成 | message={} | intent={}", message, intent);
            return intent;

        } catch (Exception e) {
            log.error("意图分类异常，默认归类为 CHAT | error={}", e.getMessage());
            return Intent.CHAT;
        }
    }

    /**
     * 从 LLM 返回的 JSON 字符串中解析 Intent
     *
     * @param jsonStr LLM 返回的原始文本（可能包含多余字符）
     * @return 解析出的 Intent，解析失败默认返回 CHAT
     */
    private Intent parseIntent(String jsonStr) {
        try {
            // 尝试提取 JSON 对象（处理 LLM 可能返回多余内容的情况）
            int start = jsonStr.indexOf('{');
            int end = jsonStr.lastIndexOf('}');
            if (start == -1 || end == -1 || start >= end) {
                log.warn("意图分类结果不包含有效 JSON: {}", jsonStr);
                return Intent.CHAT;
            }

            String json = jsonStr.substring(start, end + 1);

            // 简单解析 "intent" 字段（避免引入额外依赖）
            if (json.contains("\"IMAGE_GENERATE\"")) {
                return Intent.IMAGE_GENERATE;
            } else if (json.contains("\"IMAGE_ANALYZE\"")) {
                return Intent.IMAGE_ANALYZE;
            } else if (json.contains("\"CHAT\"")) {
                return Intent.CHAT;
            }

            log.warn("无法从意图分类结果中解析 intent 字段: {}", json);
            return Intent.CHAT;

        } catch (Exception e) {
            log.warn("解析意图分类结果异常: {}", e.getMessage());
            return Intent.CHAT;
        }
    }
}