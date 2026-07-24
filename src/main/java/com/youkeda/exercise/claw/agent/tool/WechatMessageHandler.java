package com.youkeda.exercise.claw.agent.tool;

import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;

import java.util.List;

/**
 * 微信消息处理器接口
 *
 * <p>WeChat 消息路由体系中的处理器接口。
 * 所有消息处理逻辑实现此接口，通过 Spring 容器自动收集。
 *
 * <p>返回 {@link List}{@code <WechatReply>} 支持一条消息拆分为多条回复
 * （如先发图片、再发文字），发送层按顺序逐条发送。
 */
public interface WechatMessageHandler {

    /**
     * 处理微信消息并返回回复内容列表
     *
     * @param message 微信消息（包含类型、发送者、内容等）
     * @return 回复列表，返回 null 或空列表表示不回复
     */
    List<WechatReply> handle(WechatMessage message);
}