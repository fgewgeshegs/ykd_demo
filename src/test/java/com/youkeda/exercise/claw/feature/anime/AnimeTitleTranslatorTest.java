package com.youkeda.exercise.claw.feature.anime;

import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.domain.anime.Anime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnimeTitleTranslatorTest {

    @Mock
    private LLMClient llmClient;

    private AnimeTitleTranslator translator;

    @BeforeEach
    void setUp() {
        translator = new AnimeTitleTranslator(llmClient);
    }

    private Anime anime(String title, String titleJa) {
        return new Anime(1, title, titleJa, "", "RELEASING", 12, List.of(), 80, 100000);
    }

    @Test
    @DisplayName("调用 LLM 时预留推理安全的 max_tokens，避免 thinking 吃光预算")
    void callsLlmWithReasoningSafeMaxTokens() {
        when(llmClient.chatWithSystemPrompt(anyString(), anyString(), anyInt()))
                .thenReturn("碧蓝之海 第三季");
        translator.translate(anime("Grand Blue Season 3", "ぐらんぶる"));

        ArgumentCaptor<Integer> maxTokensCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(llmClient).chatWithSystemPrompt(anyString(), anyString(), maxTokensCaptor.capture());
        assertTrue(maxTokensCaptor.getValue() >= 2000,
                "推理模型 max_tokens 需为 thinking+正文预留空间，当前=" + maxTokensCaptor.getValue());
    }

    @Test
    @DisplayName("LLM 返回中文译名时返回该译名")
    void returnsChineseTitleFromLlm() {
        when(llmClient.chatWithSystemPrompt(anyString(), anyString(), anyInt()))
                .thenReturn("碧蓝之海 第三季");
        assertEquals("碧蓝之海 第三季", translator.translate(anime("Grand Blue Season 3", "ぐらんぶる")));
    }

    @Test
    @DisplayName("LLM 失败或返回空时返回 null（不落死值）")
    void returnsNullOnFailureOrBlank() {
        when(llmClient.chatWithSystemPrompt(anyString(), anyString(), anyInt()))
                .thenReturn("   ");
        assertNull(translator.translate(anime("Grand Blue Season 3", "ぐらんぶる")),
                "空结果应返回 null");

        when(llmClient.chatWithSystemPrompt(anyString(), anyString(), anyInt()))
                .thenThrow(new RuntimeException("LLM down"));
        assertNull(translator.translate(anime("Grand Blue Season 3", "ぐらんぶる")),
                "LLM 异常应返回 null");
    }

    @Test
    @DisplayName("LLM 返回《碧蓝之海》时剥离书名号返回碧蓝之海")
    void stripsSurroundingBookTitleMarks() {
        when(llmClient.chatWithSystemPrompt(anyString(), anyString(), anyInt()))
                .thenReturn("《碧蓝之海》");
        assertEquals("碧蓝之海", translator.translate(anime("Grand Blue Season 3", "ぐらんぶる")),
                "应剥离首尾书名号，原样返回内部译名");
    }

    @Test
    @DisplayName("LLM 返回多层包围符号时逐层剥离")
    void stripsNestedSurroundingWrappers() {
        when(llmClient.chatWithSystemPrompt(anyString(), anyString(), anyInt()))
                .thenReturn("「《碧蓝之海》」");
        assertEquals("碧蓝之海", translator.translate(anime("Grand Blue Season 3", "ぐらんぶる")),
                "应逐层剥离「」与《》");
    }

    @Test
    @DisplayName("LLM 返回带书名号的原片名时剥离后回显仍视为无法翻译")
    void stripsWrapperThenEchoStillReturnsNull() {
        when(llmClient.chatWithSystemPrompt(anyString(), anyString(), anyInt()))
                .thenReturn("《Grand Blue Season 3》");
        assertNull(translator.translate(anime("Grand Blue Season 3", "ぐらんぶる")),
                "剥离书名号后仍回显原片名，应返回 null");
    }

    @Test
    @DisplayName("LLM 返回空白书名号时返回 null")
    void stripsWrapperDownToBlankReturnsNull() {
        when(llmClient.chatWithSystemPrompt(anyString(), anyString(), anyInt()))
                .thenReturn("《》");
        assertNull(translator.translate(anime("Grand Blue Season 3", "ぐらんぶる")),
                "剥离后为空白应返回 null");
    }

    @Test
    @DisplayName("LLM 回显罗马音原文时返回 null（无法确认中文译名）")
    void returnsNullWhenLlmEchoesOriginal() {
        when(llmClient.chatWithSystemPrompt(anyString(), anyString(), anyInt()))
                .thenReturn("Grand Blue Season 3");
        assertNull(translator.translate(anime("Grand Blue Season 3", "ぐらんぶる")),
                "回显原片名视为无法翻译");
    }

    @Test
    @DisplayName("LLM 回显日文名 titleJa 时返回 null（无法确认中文译名）")
    void returnsNullWhenLlmEchoesJapaneseTitle() {
        when(llmClient.chatWithSystemPrompt(anyString(), anyString(), anyInt()))
                .thenReturn("ぐらんぶる");
        assertNull(translator.translate(anime("Grand Blue Season 3", "ぐらんぶる")),
                "回显日文名视为无法翻译");
    }
}
