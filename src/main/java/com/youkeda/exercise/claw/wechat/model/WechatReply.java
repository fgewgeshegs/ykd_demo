package com.youkeda.exercise.claw.wechat.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 微信回复模型
 *
 * 封装对微信消息的回复内容，支持 TEXT、IMAGE、VOICE 和 FILE 四种回复类型。
 * 取代之前所有 Handler 直接返回 String 的方式，使回复内容可以携带图片、语音或文件字节。
 */
@Data
@Accessors(chain = true)
public class WechatReply {

    /**
     * 回复类型：TEXT / IMAGE / VOICE / FILE
     */
    private MessageType type;

    /**
     * 文本回复内容（TEXT 类型时有效）
     */
    private String text;

    /**
     * 图片字节数据（IMAGE 类型时有效）
     */
    private byte[] imageBytes;

    /**
     * 语音字节数据（VOICE 类型时有效）
     */
    private byte[] voiceBytes;

    /**
     * 语音播放时长（VOICE 类型时有效，毫秒）
     */
    private int playtime;

    /**
     * 语音编码类型（VOICE 类型时有效）
     */
    private int encodeType;

    /**
     * 语音采样率（VOICE 类型时有效，Hz，TTS 用）
     */
    private int sampleRate = 8000;

    /**
     * 语音发送失败的兜底文本（VOICE 类型时有效）
     * 当语音发送失败时，回退发送此文本
     */
    private String textFallback;

    /**
     * 文件字节数据（FILE 类型时有效）
     */
    private byte[] fileBytes;

    /**
     * 文件名（FILE 类型时有效，如 "AI语音回复.mp3"）
     */
    private String fileName;

    /**
     * 文件描述文本（FILE 类型时有效，会随文件一起显示）
     */
    private String fileDescription;

    public static WechatReply text(String text) {
        return new WechatReply().setType(MessageType.TEXT).setText(text);
    }

    public static WechatReply image(byte[] imageBytes) {
        return new WechatReply().setType(MessageType.IMAGE).setImageBytes(imageBytes);
    }

    /**
     * 创建语音回复
     *
     * @param voiceBytes   语音音频字节数据
     * @param playtime     语音时长（毫秒）
     * @param encodeType   语音编码类型
     * @param sampleRate   语音采样率（Hz）
     * @param textFallback 语音发送失败的兜底文字，可为 null
     * @return WechatReply
     */
    public static WechatReply voice(byte[] voiceBytes, int playtime, int encodeType,
                                     int sampleRate, String textFallback) {
        return new WechatReply()
                .setType(MessageType.VOICE)
                .setVoiceBytes(voiceBytes)
                .setPlaytime(playtime)
                .setEncodeType(encodeType)
                .setSampleRate(sampleRate)
                .setTextFallback(textFallback);
    }

    /**
     * 创建文件回复（如发送 MP3 音频文件）
     *
     * @param fileBytes       文件字节数据
     * @param fileName        文件名（如 "AI语音回复.mp3"）
     * @param fileDescription 文件描述文本，可为 null
     * @return WechatReply
     */
    public static WechatReply file(byte[] fileBytes, String fileName, String fileDescription) {
        return new WechatReply()
                .setType(MessageType.FILE)
                .setFileBytes(fileBytes)
                .setFileName(fileName)
                .setFileDescription(fileDescription);
    }

    /**
     * 判断该回复是否有有效内容
     */
    public boolean hasContent() {
        return (type == MessageType.TEXT && text != null && !text.isEmpty())
                || (type == MessageType.IMAGE && imageBytes != null && imageBytes.length > 0)
                || (type == MessageType.VOICE && voiceBytes != null && voiceBytes.length > 0)
                || (type == MessageType.FILE && fileBytes != null && fileBytes.length > 0);
    }
}
