package com.youkeda.exercise.claw.wechat.service;

import com.youkeda.exercise.claw.agent.memory.ContextStore;
import com.youkeda.exercise.claw.wechat.MessageRouter;
import com.youkeda.exercise.claw.wechat.client.WechatILinkClient;
import com.youkeda.exercise.claw.wechat.config.WechatProperties;
import com.youkeda.exercise.claw.wechat.user.WechatUserManager;
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