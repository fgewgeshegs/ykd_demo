package com.youkeda.exercise.claw.scout.notifier;

import com.youkeda.exercise.claw.scout.judge.Recommendation;
import com.youkeda.exercise.claw.wechat.client.WechatILinkClient;
import com.youkeda.exercise.claw.wechat.user.WechatUserManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationServiceDeliveryTest {

    @Test
    void sendsOnlyAConciseActionableMessageForWorkflowFailure() {
        WechatILinkClient wechatClient = mock(WechatILinkClient.class);
        WechatUserManager userManager = mock(WechatUserManager.class);
        when(userManager.getOwnerUserId()).thenReturn("owner-1");
        when(wechatClient.sendTextMessage(anyString(), anyString())).thenReturn(true);
        NotificationService service = new NotificationService(
                wechatClient, userManager,
                mock(RecommendationSummaryService.class));

        service.notifyFailure();

        verify(wechatClient).sendTextMessage(
                "owner-1", "信息猎手本次运行失败，请稍后重试。");
    }

    @Test
    void doesNotSendWhenOwnerIsUnavailable() {
        WechatILinkClient wechatClient = mock(WechatILinkClient.class);
        WechatUserManager userManager = mock(WechatUserManager.class);
        RecommendationSummaryService summaryService = mock(RecommendationSummaryService.class);
        when(userManager.getOwnerUserId()).thenReturn(null);
        NotificationService service = new NotificationService(
                wechatClient, userManager, summaryService);

        service.notifyWithSummary(List.of(new Recommendation(
                "rec-1", "title", "summary", "reason", "suggestion",
                "https://example.com", 0.9f, System.currentTimeMillis())));

        verify(wechatClient, never()).sendTextMessage(any(), anyString());
        verify(summaryService, never()).summarize(anyList());
    }

    @Test
    void doesNotSummarizeWhenWechatSendFails() {
        WechatILinkClient wechatClient = mock(WechatILinkClient.class);
        WechatUserManager userManager = mock(WechatUserManager.class);
        RecommendationSummaryService summaryService = mock(RecommendationSummaryService.class);
        when(userManager.getOwnerUserId()).thenReturn("owner-1");
        when(wechatClient.sendTextMessage(eq("owner-1"), anyString())).thenReturn(false);
        NotificationService service = new NotificationService(
                wechatClient, userManager, summaryService);

        service.notifyWithSummary(List.of(new Recommendation(
                "rec-1", "title", "summary", "reason", "suggestion",
                "https://example.com", 0.9f, System.currentTimeMillis())));

        verify(summaryService, never()).summarize(anyList());
    }

    @Test
    void sendsSummaryAfterWechatSendSucceeds() {
        WechatILinkClient wechatClient = mock(WechatILinkClient.class);
        WechatUserManager userManager = mock(WechatUserManager.class);
        RecommendationSummaryService summaryService = mock(RecommendationSummaryService.class);
        when(userManager.getOwnerUserId()).thenReturn("owner-1");
        when(wechatClient.sendTextMessage(eq("owner-1"), anyString())).thenReturn(true);
        when(summaryService.summarize(anyList())).thenReturn("📌 信息猎手总结\n\n总结内容");
        NotificationService service = new NotificationService(
                wechatClient, userManager, summaryService);

        service.notifyWithSummary(List.of(new Recommendation(
                "rec-1", "title", "summary", "reason", "suggestion",
                "https://example.com", 0.9f, System.currentTimeMillis())));

        verify(wechatClient, times(2)).sendTextMessage(eq("owner-1"), anyString());
    }

    @Test
    void sendsTheSameRecommendationAgainOnLaterCalls() {
        WechatILinkClient wechatClient = mock(WechatILinkClient.class);
        WechatUserManager userManager = mock(WechatUserManager.class);
        RecommendationSummaryService summaryService = mock(RecommendationSummaryService.class);
        when(userManager.getOwnerUserId()).thenReturn("owner-1");
        when(wechatClient.sendTextMessage(eq("owner-1"), anyString())).thenReturn(true);
        NotificationService service = new NotificationService(
                wechatClient, userManager, summaryService);
        List<Recommendation> recommendations = List.of(new Recommendation(
                "rec-1", "title", "summary", "reason", "suggestion",
                "https://example.com", 0.9f, System.currentTimeMillis()));

        service.notify(recommendations);
        service.notify(recommendations);

        verify(wechatClient, times(2))
                .sendTextMessage(eq("owner-1"), anyString());
    }

    @Test
    void groupsStrongAndDiscoveryRecommendationsInTheReport() {
        WechatILinkClient wechatClient = mock(WechatILinkClient.class);
        WechatUserManager userManager = mock(WechatUserManager.class);
        RecommendationSummaryService summaryService = mock(RecommendationSummaryService.class);
        when(userManager.getOwnerUserId()).thenReturn("owner-1");
        when(wechatClient.sendTextMessage(eq("owner-1"), anyString())).thenReturn(true);
        when(summaryService.summarize(anyList())).thenReturn("📌 信息猎手总结\n\n综合结论");
        NotificationService service = new NotificationService(
                wechatClient, userManager, summaryService);

        service.notifyWithSummary(List.of(
                new Recommendation("strong", "强推荐", "summary", "reason", "suggestion",
                        "https://example.com/strong", 0.55f,
                        Recommendation.Tier.STRONG, System.currentTimeMillis()),
                new Recommendation("scan", "可浏览", "summary", "reason", "suggestion",
                        "https://example.com/scan", 0.90f,
                        Recommendation.Tier.DISCOVERY, System.currentTimeMillis())));

        ArgumentCaptor<String> report = ArgumentCaptor.forClass(String.class);
        verify(wechatClient, times(2)).sendTextMessage(eq("owner-1"), report.capture());
        org.junit.jupiter.api.Assertions.assertTrue(
                report.getAllValues().get(0).contains("🔥 强推荐"));
        org.junit.jupiter.api.Assertions.assertTrue(
                report.getAllValues().get(0).contains("👀 值得扫一眼"));
        org.junit.jupiter.api.Assertions.assertTrue(
                report.getAllValues().get(1).contains("📌 信息猎手总结"));
    }

    @Test
    void plainNotificationDoesNotSendScoutSummary() {
        WechatILinkClient wechatClient = mock(WechatILinkClient.class);
        WechatUserManager userManager = mock(WechatUserManager.class);
        RecommendationSummaryService summaryService = mock(RecommendationSummaryService.class);
        when(userManager.getOwnerUserId()).thenReturn("owner-1");
        when(wechatClient.sendTextMessage(eq("owner-1"), anyString())).thenReturn(true);
        NotificationService service = new NotificationService(
                wechatClient, userManager, summaryService);

        service.notify(List.of(new Recommendation(
                "campus", "考试提醒", "summary", "reason", "suggestion",
                "https://example.com/campus", 1.0f, System.currentTimeMillis())));

        verify(wechatClient, times(1)).sendTextMessage(eq("owner-1"), anyString());
        verify(summaryService, never()).summarize(anyList());
    }

    @Test
    void splitsLongDetailReportAtRecommendationBoundariesBeforeSummary() {
        WechatILinkClient wechatClient = mock(WechatILinkClient.class);
        WechatUserManager userManager = mock(WechatUserManager.class);
        RecommendationSummaryService summaryService = mock(RecommendationSummaryService.class);
        when(userManager.getOwnerUserId()).thenReturn("owner-1");
        when(wechatClient.sendTextMessage(eq("owner-1"), anyString())).thenReturn(true);
        when(summaryService.summarize(anyList())).thenReturn("📌 信息猎手总结\n\n综合结论");
        NotificationService service = new NotificationService(
                wechatClient, userManager, summaryService);

        String longSummary = "这是一段较长的推荐摘要，用于验证长文本会按完整条目分段发送。".repeat(12);
        List<Recommendation> recommendations = java.util.stream.IntStream.rangeClosed(1, 6)
                .mapToObj(index -> new Recommendation(
                        "rec-" + index,
                        "推荐标题 " + index,
                        longSummary,
                        "匹配用户画像",
                        "阅读原文",
                        "https://example.com/" + index,
                        0.6f,
                        Recommendation.Tier.DISCOVERY,
                        System.currentTimeMillis()))
                .toList();

        service.notifyWithSummary(recommendations);

        ArgumentCaptor<String> messages = ArgumentCaptor.forClass(String.class);
        verify(wechatClient, atLeast(3))
                .sendTextMessage(eq("owner-1"), messages.capture());
        List<String> sent = messages.getAllValues();
        assertTrue(sent.get(0).startsWith("🔍 信息猎手发现 6 条"));
        assertTrue(sent.stream().limit(sent.size() - 1L)
                .allMatch(message -> message.startsWith("🔍 信息猎手")));
        assertTrue(sent.get(sent.size() - 1).startsWith("📌 信息猎手总结"));
        assertTrue(sent.stream().limit(sent.size() - 1L)
                .allMatch(message -> message.length() <= 1800));
    }
}
