package com.youkeda.exercise.claw.wechat.handler;

import com.youkeda.exercise.claw.wechat.model.WechatMessage;

/**
 * 微信消息处理器接口
 *
 * 所有消息处理逻辑实现此接口，通过 Spring 容器自动收集
 * 支持文本、图片等多种消息类型
 */
public interface MessageHandler {

    /**
     * 处理微信消息并返回回复内容
     *
     * @param message 微信消息（包含类型、发送者、内容等）
     * @return 回复内容，返回 null 表示不回复（由下一个 Handler 处理）
     */
    String handle(WechatMessage message);
}
