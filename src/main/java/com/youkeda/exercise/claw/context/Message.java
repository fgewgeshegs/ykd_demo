package com.youkeda.exercise.claw.context;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * 单条对话消息记录
 *
 * 媒体数据（语音 CDN 参数、图片 URL）嵌入 Message，统一存入队列。
 * 用普通 POJO 而非 record，避免 Jackson 序列化兼容问题。
 */
public final class Message {

    private final String role;
    private final String content;
    private final String mediaEncryptParam;
    private final String mediaAesKey;
    private final String mediaUrl;

    @JsonCreator
    public Message(@JsonProperty("role") String role,
                   @JsonProperty("content") String content,
                   @JsonProperty("mediaEncryptParam") String mediaEncryptParam,
                   @JsonProperty("mediaAesKey") String mediaAesKey,
                   @JsonProperty("mediaUrl") String mediaUrl) {
        this.role = role;
        this.content = content;
        this.mediaEncryptParam = mediaEncryptParam;
        this.mediaAesKey = mediaAesKey;
        this.mediaUrl = mediaUrl;
    }

    /** 纯文本消息 */
    public Message(String role, String content) {
        this(role, content, null, null, null);
    }

    @JsonProperty("role")
    public String role() { return role; }
    @JsonProperty("content")
    public String content() { return content; }
    @JsonProperty("mediaEncryptParam")
    public String mediaEncryptParam() { return mediaEncryptParam; }
    @JsonProperty("mediaAesKey")
    public String mediaAesKey() { return mediaAesKey; }
    @JsonProperty("mediaUrl")
    public String mediaUrl() { return mediaUrl; }

    /** 是否有媒体 CDN 参数 */
    public boolean hasMedia() {
        return mediaEncryptParam != null && mediaAesKey != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message m)) return false;
        return Objects.equals(role, m.role)
                && Objects.equals(content, m.content)
                && Objects.equals(mediaEncryptParam, m.mediaEncryptParam)
                && Objects.equals(mediaAesKey, m.mediaAesKey)
                && Objects.equals(mediaUrl, m.mediaUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, content, mediaEncryptParam, mediaAesKey, mediaUrl);
    }

    @Override
    public String toString() {
        return "Message[role=" + role + ", content=" + content + "]";
    }
}
