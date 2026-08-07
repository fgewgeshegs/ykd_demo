package com.youkeda.exercise.claw.infrastructure.channel.wechat.login;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wechat.ilink.sdk.core.context.ContextKey;
import com.github.wechat.ilink.sdk.core.context.ConversationContext;
import com.github.wechat.ilink.sdk.core.context.ResumeContext;
import com.github.wechat.ilink.sdk.core.login.LoginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ResumeContext 的 JSON 编解码。
 *
 * <p>在 BotManager 序列化 loginContext + updatesCursor 的基础上，额外保留
 * conversation contexts（按 userId 的 latest context token），使机器人重启后
 * 无需用户重新发消息即可向已对话过的用户发起主动推送（如动漫/校园通知）。
 * 向后兼容不含 contexts 的旧版 JSON（旧数据反序列化后为空池）。
 */
public class ResumeContextCodec {

    private static final Logger log = LoggerFactory.getLogger(ResumeContextCodec.class);

    private final ObjectMapper objectMapper;

    public ResumeContextCodec() {
        this(new ObjectMapper());
    }

    public ResumeContextCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(ResumeContext ctx) {
        try {
            LoginContext lc = ctx.getLoginContext();
            var map = new LinkedHashMap<String, Object>();
            map.put("botToken", lc.getBotToken());
            map.put("userId", lc.getUserId());
            map.put("botId", lc.getBotId());
            map.put("baseUrl", lc.getBaseUrl());
            map.put("updatesCursor", ctx.getUpdatesCursor());
            map.put("conversations", encodeConversations(ctx.getConversationContextMap()));
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化 ResumeContext 失败", e);
        }
    }

    public ResumeContext deserialize(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            String botToken = textOrNull(root, "botToken");
            String userId = textOrNull(root, "userId");
            String botId = textOrNull(root, "botId");
            String baseUrl = textOrNull(root, "baseUrl");
            String updatesCursor = textOrNull(root, "updatesCursor");

            LoginContext lc = new LoginContext(botToken, userId, botId, baseUrl);
            ResumeContext.Builder builder = ResumeContext.builder(lc);
            if (updatesCursor != null) {
                builder.updatesCursor(updatesCursor);
            }
            Map<String, ConversationContext> conversations = decodeConversations(root.get("conversations"));
            builder.conversationContexts(conversations);
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException("反序列化 ResumeContext 失败", e);
        }
    }

    private List<Map<String, String>> encodeConversations(
            Map<String, ConversationContext> contexts) {
        List<Map<String, String>> result = new ArrayList<>();
        if (contexts == null) return result;
        for (ConversationContext ctx : contexts.values()) {
            if (ctx == null || !ctx.hasContextToken()) continue;
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("userId", ctx.getKey().getUserId());
            entry.put("botId", ctx.getKey().getBotId());
            entry.put("token", ctx.getLatestContextToken());
            result.add(entry);
        }
        return result;
    }

    private Map<String, ConversationContext> decodeConversations(JsonNode node) {
        Map<String, ConversationContext> result = new LinkedHashMap<>();
        if (node == null || !node.isArray()) return result;
        for (JsonNode item : node) {
            String userId = textOrNull(item, "userId");
            String botId = textOrNull(item, "botId");
            String token = textOrNull(item, "token");
            if (userId == null || botId == null || token == null) continue;
            ConversationContext ctx = new ConversationContext(new ContextKey(botId, userId));
            ctx.setLatestContextToken(token);
            result.put(userId, ctx);
        }
        return result;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        return value.asText();
    }
}
