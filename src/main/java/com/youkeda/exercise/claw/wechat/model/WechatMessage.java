package com.youkeda.exercise.claw.wechat.model;

import lombok.Data;

/**
 * 微信消息模型
 *
 * 统一封装从微信接收到的各类消息，供 MessageHandler 处理
 */
@Data
public class WechatMessage {

    /**
     * 消息发送者 userId
     */
    private String userId;

    /**
     * 消息类型
     */
    private MessageType type;

    /**
     * 文本内容（TEXT 类型时有效）
     */
    private String text;

    /**
     * 图片 URL（IMAGE 类型时有效，公开可访问的地址）
     */
    private String imageUrl;

    /**
     * 图片 CDN 加密参数（IMAGE 类型时有效，用于从微信 CDN 下载原始图片）
     */
    private String encryptQueryParam;

    /**
     * 图片解密密钥（IMAGE 类型时有效）
     */
    private String aesKey;

    /**
     * 回复上下文 token，用于发送消息时关联原始会话
     */
    private String contextToken;

    // ========== 语音相关字段（VOICE 类型时有效）==========

    /**
     * 语音 ASR 文本（微信服务端语音识别结果）
     */
    private String voiceText;

    /**
     * 语音加密查询参数（下载音频 CDN 用）
     */
    private String voiceEncryptQueryParam;

    /**
     * 语音解密密钥（下载音频 CDN 用）
     */
    private String voiceAesKey;

    /**
     * 语音播放时长（毫秒）
     */
    private Integer playtime;

    /**
     * 语音编码类型
     */
    private Integer encodeType;

    /**
     * 语音采样率（VOICE 类型时有效，新 SDK 新增字段）
     */
    private Integer voiceSampleRate;
}
