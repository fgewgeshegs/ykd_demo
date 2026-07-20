package com.youkeda.exercise.claw.wechat.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 微信回复模型
 *
 * 封装对微信消息的回复内容，支持 TEXT 和 IMAGE 两种回复类型。
 * 取代之前所有 Handler 直接返回 String 的方式，使回复内容可以携带图片字节。
 */
@Data
@Accessors(chain = true)
public class WechatReply {

    /**
     * 回复类型：TEXT 或 IMAGE
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

    public static WechatReply text(String text) {
        return new WechatReply().setType(MessageType.TEXT).setText(text);
    }

    public static WechatReply image(byte[] imageBytes) {
        return new WechatReply().setType(MessageType.IMAGE).setImageBytes(imageBytes);
    }

    /**
     * 判断该回复是否有有效内容
     */
    public boolean hasContent() {
        return (type == MessageType.TEXT && text != null && !text.isEmpty())
                || (type == MessageType.IMAGE && imageBytes != null && imageBytes.length > 0);
    }
}
