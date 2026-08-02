package com.youkeda.exercise.claw.agent.memory;

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
     * 获取最近 maxMessages 条历史消息（按时间正序）
     */
    List<Message> getHistory(int maxMessages);

    /**
     * 追加一条纯文本消息
     */
    void append(String role, String content);

    /**
     * 追加一条带媒体附件的消息
     *
     * @param mediaEncryptParam 媒体 CDN 加密参数（无则 null）
     * @param mediaAesKey       媒体 CDN 解密密钥（无则 null）
     * @param mediaUrl          媒体 URL（无则 null）
     */
    void append(String role, String content,
                String mediaEncryptParam, String mediaAesKey,
                String mediaUrl);

    /**
     * 追加一条完整的 Message（保留 toolCallId / toolName / reasoningContent）。
     * <p>用于持久化工具调用与工具结果，使下一轮对话的 LLM 能看到真实的工具执行记录，
     * 避免因历史中缺失工具证据而误判上一轮结果为编造。
     */
    void append(Message message);

    /**
     * 找到最近一条 content 以 prefix 开头的消息（反向扫描）
     */
    Message findLastByPrefix(String contentPrefix);

    /**
     * 找到所有 content 以 prefix 开头的消息（正序）
     */
    List<Message> findAllByPrefix(String contentPrefix);

    /**
     * 清除全部上下文
     */
    void clear();
}
