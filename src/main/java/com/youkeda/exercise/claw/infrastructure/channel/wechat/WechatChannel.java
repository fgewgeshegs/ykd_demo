package com.youkeda.exercise.claw.infrastructure.channel.wechat;

import com.youkeda.exercise.claw.infrastructure.channel.NotificationChannel;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.client.WechatILinkClient;
import org.springframework.stereotype.Component;

/**
 * 微信通知通道适配器。
 *
 * <p>将 {@link WechatILinkClient} 适配为 {@link NotificationChannel}，
 * 不改动 WechatILinkClient 的任何代码。
 */
@Component
public class WechatChannel implements NotificationChannel {

    private final WechatILinkClient wechatClient;

    public WechatChannel(WechatILinkClient wechatClient) {
        this.wechatClient = wechatClient;
    }

    @Override
    public boolean send(String userId, String text) {
        return wechatClient.sendTextMessage(userId, text);
    }

    @Override
    public boolean isAvailable() {
        return wechatClient.isLoggedIn();
    }

    @Override
    public String name() {
        return "wechat";
    }
}