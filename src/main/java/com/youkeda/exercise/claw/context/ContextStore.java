package com.youkeda.exercise.claw.context;

import java.util.List;

/**
 * 会话上下文存储
 *
 * 职责：存取用户对话历史，为 LLM 多轮对话提供记忆能力。
 * 每条 Message 可携带可选的媒体参数（CDN 下载参数、URL、TTS 音频），
 * 不再需要单独的 setLastImage / addVoiceMedia 等碎片方法。
 */
public interface ContextStore {

    /**
     * 获取用户最近 maxMessages 条历史消息（按时间正序）
     */
    List<Message> getHistory(String userId, int maxMessages);

    /**
     * 追加一条纯文本消息
     */
    void append(String userId, String role, String content);

    /**
     * 追加一条带媒体附件的消息
     *
     * @param mediaEncryptParam 媒体 CDN 加密参数（无则 null）
     * @param mediaAesKey       媒体 CDN 解密密钥（无则 null）
     * @param mediaUrl          媒体 URL（无则 null）
     */
    void append(String userId, String role, String content,
                String mediaEncryptParam, String mediaAesKey,
                String mediaUrl);

    /**
     * 找到最近一条 content 以 prefix 开头的消息（反向扫描）
     */
    Message findLastByPrefix(String userId, String contentPrefix);

    /**
     * 找到所有 content 以 prefix 开头的消息（正序）
     */
    List<Message> findAllByPrefix(String userId, String contentPrefix);

    /**
     * 清除指定用户的全部上下文
     */
    void clear(String userId);
}
