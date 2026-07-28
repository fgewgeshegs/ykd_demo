package com.youkeda.exercise.claw.scout.notifier;

import com.youkeda.exercise.claw.scout.ScoutProperties;
import com.youkeda.exercise.claw.scout.judge.Recommendation;
import com.youkeda.exercise.claw.wechat.client.WechatILinkClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NotificationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void sameInformationIsOnlySentOnceDuringCooldown() {
        WechatILinkClient client = mock(WechatILinkClient.class);
        ScoutDeliveryStore deliveryStore = new ScoutDeliveryStore(
                tempDir.resolve("assistant.db").toString());
        deliveryStore.init();
        ScoutProperties props = new ScoutProperties();
        props.setDeliveryCooldownDays(30);
        NotificationService service = new NotificationService(client, deliveryStore, props);
        Recommendation recommendation = new Recommendation(
                "r1", "owner", "重要更新", "摘要", "与你的目标相关",
                "查看详情", "https://example.com/update?utm_source=wechat", 0.9f,
                System.currentTimeMillis());

        service.notify("owner", List.of(recommendation));
        service.notify("owner", List.of(recommendation));

        verify(client, times(1)).sendTextMessage(anyString(), anyString());
    }
}
