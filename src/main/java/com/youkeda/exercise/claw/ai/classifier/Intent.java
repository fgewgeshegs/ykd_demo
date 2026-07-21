package com.youkeda.exercise.claw.ai.classifier;

/**
 * 用户意图枚举
 *
 * 标识用户消息的意图类型
 */
public enum Intent {

    /**
     * 普通聊天
     */
    CHAT,

    /**
     * 用户要求生成图片
     */
    IMAGE_GENERATE,

    /**
     * 用户要求分析图片
     */
    IMAGE_ANALYZE,

    /**
     * 用户要求语音回复
     */
    VOICE_REPLY
}