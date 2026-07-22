package com.youkeda.exercise.claw.agent.memory;

/**
 * 单条对话消息记录
 *
 * 媒体数据（语音 CDN 参数、图片 URL）嵌入 Message，统一存入队列。
 * AI 语音回复不存 MP3 字节——文字已在 content 中，需要时重新 TTS 即可；
 * 用户语音的 CDN 参数在 mediaEncryptParam/mediaAesKey 中，需要时重新下载即可。
 *
 * @param role              角色：user / assistant
 * @param content           消息文本（展示给 LLM 的内容）
 * @param mediaEncryptParam 媒体 CDN 加密参数（语音/图片消息时有值）
 * @param mediaAesKey       媒体 CDN 解密密钥（语音/图片消息时有值）
 * @param mediaUrl          媒体 URL（图片时有值）
 */
public record Message(String role, String content,
                       String mediaEncryptParam, String mediaAesKey,
                       String mediaUrl) {

    /** 纯文本消息 */
    public Message(String role, String content) {
        this(role, content, null, null, null);
    }

    /** 是否有媒体 CDN 参数 */
    public boolean hasMedia() {
        return mediaEncryptParam != null && mediaAesKey != null;
    }
}
