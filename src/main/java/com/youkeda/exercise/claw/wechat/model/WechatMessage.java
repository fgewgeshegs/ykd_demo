package com.youkeda.exercise.claw.wechat.model;

/**
 * 微信消息模型
 *
 * 统一封装从微信接收到的各类消息，供 MessageHandler 处理
 */
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

    // ========== 文件相关字段（FILE 类型时有效）==========

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件 CDN 加密查询参数（下载文件用）
     */
    private String fileEncryptQueryParam;

    /**
     * 文件 CDN 解密密钥（下载文件用）
     */
    private String fileAesKey;

    /**
     * 文件 MD5
     */
    private String fileMd5;

    /**
     * 文件大小（字符串形式，如 "1024"）
     */
    private String fileLen;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getEncryptQueryParam() {
        return encryptQueryParam;
    }

    public void setEncryptQueryParam(String encryptQueryParam) {
        this.encryptQueryParam = encryptQueryParam;
    }

    public String getAesKey() {
        return aesKey;
    }

    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }

    public String getContextToken() {
        return contextToken;
    }

    public void setContextToken(String contextToken) {
        this.contextToken = contextToken;
    }

    public String getVoiceText() {
        return voiceText;
    }

    public void setVoiceText(String voiceText) {
        this.voiceText = voiceText;
    }

    public String getVoiceEncryptQueryParam() {
        return voiceEncryptQueryParam;
    }

    public void setVoiceEncryptQueryParam(String voiceEncryptQueryParam) {
        this.voiceEncryptQueryParam = voiceEncryptQueryParam;
    }

    public String getVoiceAesKey() {
        return voiceAesKey;
    }

    public void setVoiceAesKey(String voiceAesKey) {
        this.voiceAesKey = voiceAesKey;
    }

    public Integer getPlaytime() {
        return playtime;
    }

    public void setPlaytime(Integer playtime) {
        this.playtime = playtime;
    }

    public Integer getEncodeType() {
        return encodeType;
    }

    public void setEncodeType(Integer encodeType) {
        this.encodeType = encodeType;
    }

    public Integer getVoiceSampleRate() {
        return voiceSampleRate;
    }

    public void setVoiceSampleRate(Integer voiceSampleRate) {
        this.voiceSampleRate = voiceSampleRate;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileEncryptQueryParam() {
        return fileEncryptQueryParam;
    }

    public void setFileEncryptQueryParam(String fileEncryptQueryParam) {
        this.fileEncryptQueryParam = fileEncryptQueryParam;
    }

    public String getFileAesKey() {
        return fileAesKey;
    }

    public void setFileAesKey(String fileAesKey) {
        this.fileAesKey = fileAesKey;
    }

    public String getFileMd5() {
        return fileMd5;
    }

    public void setFileMd5(String fileMd5) {
        this.fileMd5 = fileMd5;
    }

    public String getFileLen() {
        return fileLen;
    }

    public void setFileLen(String fileLen) {
        this.fileLen = fileLen;
    }
}