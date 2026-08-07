package com.youkeda.exercise.claw.infrastructure.channel;

/**
 * 通知通道接口。
 *
 * <p>所有通知通道（微信、邮件、未来 WebPush 等）统一实现此接口，
 * 由 {@link NotificationRouter} 根据可用性选择通道。
 *
 * <p>recipient 语义由各通道自行解释：
 * <ul>
 *   <li>微信通道：userId（微信 SDK 用户标识）</li>
 *   <li>邮件通道：email 地址</li>
 * </ul>
 */
public interface NotificationChannel {

    /** 发送通知文本，返回 true 表示发送成功 */
    boolean send(String recipient, String text);

    /** 通道当前是否可用 */
    boolean isAvailable();

    /** 通道名称（日志/审计用） */
    String name();
}