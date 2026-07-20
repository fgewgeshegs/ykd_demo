package com.youkeda.exercise.claw.wechat.model;

/**
 * 微信消息类型枚举
 *
 * 标识从微信接收到的消息类型
 */
public enum MessageType {

    /**
     * 文本消息
     */
    TEXT,

    /**
     * 图片消息
     */
    IMAGE,

    // ========== 预留类型 ==========

    /**
     * 语音消息（预留）
     */
    VOICE,

    /**
     * 视频消息（预留）
     */
    VIDEO,

    /**
     * 文件消息（预留）
     */
    FILE
}
