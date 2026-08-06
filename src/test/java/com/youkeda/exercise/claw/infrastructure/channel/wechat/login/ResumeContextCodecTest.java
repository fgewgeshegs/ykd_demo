package com.youkeda.exercise.claw.infrastructure.channel.wechat.login;

import com.github.wechat.ilink.sdk.core.context.ContextKey;
import com.github.wechat.ilink.sdk.core.context.ConversationContext;
import com.github.wechat.ilink.sdk.core.context.ResumeContext;
import com.github.wechat.ilink.sdk.core.login.LoginContext;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumeContextCodecTest {

    private final ResumeContextCodec codec = new ResumeContextCodec();

    @Test
    void roundTripPreservesLoginContextFields() {
        LoginContext lc = new LoginContext("tok-1", "u-100", "bot-abc", "https://wechat.example");
        ResumeContext ctx = ResumeContext.builder(lc).updatesCursor("cursor-9").build();

        ResumeContext restored = codec.deserialize(codec.serialize(ctx));

        assertEquals("tok-1", restored.getLoginContext().getBotToken());
        assertEquals("u-100", restored.getLoginContext().getUserId());
        assertEquals("bot-abc", restored.getLoginContext().getBotId());
        assertEquals("https://wechat.example", restored.getLoginContext().getBaseUrl());
        assertEquals("cursor-9", restored.getUpdatesCursor());
    }

    @Test
    void roundTripPreservesConversationContextToken() {
        LoginContext lc = new LoginContext("tok-1", "u-100", "bot-abc", "https://wechat.example");
        ConversationContext conv = new ConversationContext(
                new ContextKey("bot-abc", "u-100"));
        conv.updateContextToken("latest-ctx-token-42", 100L, 200L);

        Map<String, ConversationContext> contexts = new LinkedHashMap<>();
        contexts.put("u-100", conv);
        ResumeContext ctx = ResumeContext.builder(lc)
                .conversationContexts(contexts)
                .build();

        ResumeContext restored = codec.deserialize(codec.serialize(ctx));

        ConversationContext restoredCtx = restored.getConversationContextMap().get("u-100");
        assertNotNull(restoredCtx);
        assertEquals("latest-ctx-token-42", restoredCtx.getLatestContextToken());
        assertTrue(restoredCtx.hasContextToken());
        // restore() 按 ContextKey(botId, userId) 入池，key 必须完整往返
        assertEquals("bot-abc", restoredCtx.getKey().getBotId());
        assertEquals("u-100", restoredCtx.getKey().getUserId());
    }

    @Test
    void deserializesLegacyJsonWithoutContexts() {
        String legacy = "{\"botToken\":\"tok-1\",\"userId\":\"u-100\",\"botId\":\"bot-abc\","
                + "\"baseUrl\":\"https://wechat.example\",\"updatesCursor\":\"cursor-9\"}";

        ResumeContext restored = codec.deserialize(legacy);

        assertEquals("bot-abc", restored.getLoginContext().getBotId());
        assertEquals(0, restored.getConversationContexts().size());
    }

    @Test
    void deserializeRejectsMalformedJson() {
        assertThrows(RuntimeException.class, () -> codec.deserialize("{not-json"));
    }
}
