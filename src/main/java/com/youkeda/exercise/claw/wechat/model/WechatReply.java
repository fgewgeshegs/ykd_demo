package com.youkeda.exercise.claw.wechat.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 微信回复模型
 *
 * 封装对微信消息的回复内容，支持 TEXT、IMAGE 和 FILE 三种回复类型。
 * 取代之前所有 Handler 直接返回 String 的方式，使回复内容可以携带图片或文件字节。
 */
@Data
@Accessors(chain = true)
public class WechatReply {

    /**
     * 回复类型：TEXT / IMAGE / FILE
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
                || (type == MessageType.FILE && fileBytes != null && fileBytes.length > 0);
    }
}
