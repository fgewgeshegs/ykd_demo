package com.youkeda.exercise.claw.wechat.handler;

/**
 * 微信消息处理器接口
 *
 * 所有消息处理逻辑实现此接口，通过 Spring 容器自动收集
 */
public interface MessageHandler {

    /**
     * 处理微信消息并返回回复内容
     *
     * @param userId 消息发送者 userId
     * @param text   消息文本内容
     * @return 回复内容，返回 null 表示不回复
     */
    String handle(String userId, String text);
}
