package com.youkeda.exercise.claw.context;

import java.util.List;

/**
 * 会话上下文存储
 *
 * 职责：存取用户对话历史，为 LLM 多轮对话提供记忆能力。
 * 不同实现（内存 / Redis / 数据库）只需实现此接口。
 */
public interface ContextStore {

    /**
     * 获取用户最近 maxMessages 条历史消息（按时间正序）
     *
     * @param userId      用户标识
     * @param maxMessages 最大条数
     * @return 历史消息列表，从未有过该用户时返回空列表
     */
    List<Message> getHistory(String userId, int maxMessages);

    /**
     * 追加一条对话消息
     *
     * @param userId  用户标识
     * @param role    角色（user / assistant）
     * @param content 消息文本
     */
    void append(String userId, String role, String content);

    /**
     * 保存该用户最近一张图片的 CDN 下载参数
     */
    void setLastImage(String userId, String encryptParam, String aesKey);

    /**
     * 获取该用户最近一张图片的 CDN 下载参数
     */
    String[] getLastImage(String userId);

    /**
     * 保存该用户最近一张图片的 HTTP 下载地址
     */
    void setLastImageUrl(String userId, String url);

    /**
     * 获取该用户最近一张图片的 HTTP 下载地址
     */
    String getLastImageUrl(String userId);

    /**
     * 保存最近一条语音（MP3 路径 + 转写文字）
     */
    void setLastVoice(String userId, String mp3Path, String text);

    /**
     * 获取最近一条语音 [mp3Path, text]，无数据返回 null
     */
    String[] getLastVoice(String userId);

    /**
     * 清除指定用户的全部历史
     *
     * @param userId 用户标识
     */
    void clear(String userId);
}
