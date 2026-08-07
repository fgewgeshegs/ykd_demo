package com.youkeda.exercise.claw.feature.anime;

import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.domain.anime.Anime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 番剧中文译名翻译器。
 *
 * <p>AniList 不提供中文标题，此组件通过 LLM 将罗马音/日文标题翻译为中文译名。
 * 策略（对齐 AnimeSeasonSource 的推荐 prompt）：官方中文译名优先，无法确认时直译。
 * LLM 失败、返回空、或回显原片名时返回 null——调用方回退罗马音，每日回填会重试。
 */
@Component
@ConditionalOnProperty(name = "anime.enabled", havingValue = "true")
public class AnimeTitleTranslator {

    private static final Logger log = LoggerFactory.getLogger(AnimeTitleTranslator.class);

    private static final String SYSTEM_PROMPT = "你是番剧译名助手。将用户给出的番剧名称翻译为官方中文译名。";

    private static final String TRANSLATE_PROMPT = """
            把下列番剧名称翻译成中文译名。
            要求：
            1. 官方中文译名优先（如《Grand Blue》→《碧蓝之海》），无法确认官方译名时按意译直译。
            2. 只输出译名本身，不要任何解释、书名号、引号、前后缀。
            3. 若无法确认任何合理中文译名（包括输入本身已是中文或无法翻译），原样输出输入的罗马音/日文名。
            4. 输出一个名称，不要多个备选。

            番剧罗马音名：%s
            番剧日文名：%s
            """;

    /** 首尾成对包围符号（开符号在偶数位，闭符号紧跟其后），用于剥离 LLM 违规输出的书名号/引号 */
    private static final String WRAPPER_PAIRS = "《》「」『』【】\"\"''“”‘’";

    private final LLMClient llmClient;

    public AnimeTitleTranslator(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    /**
     * 翻译番剧中文译名。
     *
     * @return 中文译名；无法翻译（LLM 失败/空/回显原片名）时返回 null
     */
    public String translate(Anime anime) {
        if (anime == null || anime.getTitle() == null || anime.getTitle().isBlank()) {
            return null;
        }
        try {
            String titleJa = anime.getTitleJa() != null ? anime.getTitleJa() : "";
            String result = llmClient.chatWithSystemPrompt(
                    SYSTEM_PROMPT, String.format(TRANSLATE_PROMPT, anime.getTitle(), titleJa),
                    LLMClient.REASONING_SAFE_MAX_TOKENS);
            if (result == null || result.isBlank()) {
                log.warn("译名 LLM 返回空 | title={}", anime.getTitle());
                return null;
            }
            String cleaned = result.trim();
            // LLM 常违反「不要书名号/引号」约束：剥离首尾包围符号，避免《碧蓝之海》式污染落库
            cleaned = stripSurroundingWrapper(cleaned);
            if (cleaned.isBlank()) {
                log.warn("译名 LLM 返回空（剥离包围符号后为空白）| title={}", anime.getTitle());
                return null;
            }
            // 回显原片名 = 无法确认中文译名，视为失败
            if (cleaned.equalsIgnoreCase(anime.getTitle())) {
                return null;
            }
            // 回显日文名 = 同样无法确认中文译名（prompt 允许回显日文名），视为失败
            if (!titleJa.isBlank() && cleaned.equalsIgnoreCase(titleJa)) {
                return null;
            }
            return cleaned;
        } catch (Exception e) {
            log.warn("译名 LLM 调用失败 | title={} | error={}", anime.getTitle(), e.getMessage());
            return null;
        }
    }

    /**
     * 循环剥离首尾成对的包围符号（书名号/引号/括号），直到首尾不再成对。
     * 例：{@code 《碧蓝之海》} → {@code 碧蓝之海}；{@code 「《碧蓝之海》」} → {@code 碧蓝之海}。
     * 仅剥除包围在首尾的符号，绝不改动名称内部内容。
     */
    private static String stripSurroundingWrapper(String s) {
        int start = 0;
        int end = s.length();
        while (end - start >= 2) {
            int openIdx = WRAPPER_PAIRS.indexOf(s.charAt(start));
            if (openIdx < 0 || openIdx % 2 != 0 || s.charAt(end - 1) != WRAPPER_PAIRS.charAt(openIdx + 1)) {
                break;
            }
            start++;
            end--;
        }
        return s.substring(start, end);
    }
}
