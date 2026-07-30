package com.youkeda.exercise.claw.infrastructure.channel.wechat.service;

import com.youkeda.exercise.claw.agent.memory.ContextStore;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.MessageRouter;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.client.WechatILinkClient;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.config.WechatProperties;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.user.WechatUserManager;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class WechatMessageServiceOwnerTrackingTest {

    @Test
    void recordsIncomingSenderForLaterBackgroundNotifications() {
        WechatUserManager userManager = mock(WechatUserManager.class);
        WechatMessageService service = new WechatMessageService(
                mock(WechatILinkClient.class),
                new WechatProperties(),
                mock(MessageRouter.class),
                mock(ContextStore.class),
                userManager);

        service.recordSender("wechat-owner-1");

        verify(userManager).recordInteraction("wechat-owner-1");
    }
}