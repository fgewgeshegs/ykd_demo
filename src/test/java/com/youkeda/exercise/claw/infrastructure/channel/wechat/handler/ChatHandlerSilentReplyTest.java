package com.youkeda.exercise.claw.infrastructure.channel.wechat.handler;

import com.youkeda.exercise.claw.agent.AgentExecutionPool;
import com.youkeda.exercise.claw.agent.CancellationManager;
import com.youkeda.exercise.claw.agent.ReActAgentExecutor;
import com.youkeda.exercise.claw.tool.voice.VoiceTool;
import com.youkeda.exercise.claw.tool.file.FileGenerationTool;
import com.youkeda.exercise.claw.tool.image.ImageGenerationTool;
import com.youkeda.exercise.claw.tool.map.PlaceImageTool;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.client.WechatILinkClient;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.model.MessageType;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.model.WechatReply;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.user.WechatUserManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatHandlerSilentReplyTest {

    @Test
    void convertsHandledAgentResultToSilentWechatReply() {
        ReActAgentExecutor executor = mock(ReActAgentExecutor.class);
        WechatUserManager userManager = mock(WechatUserManager.class);
        when(userManager.getOwnerUserId()).thenReturn("test-user");

        ChatHandler chatHandler = new ChatHandler(
                executor,
                mock(VoiceTool.class),
                mock(FileGenerationTool.class),
                mock(ImageGenerationTool.class),
                mock(PlaceImageTool.class),
                mock(WechatILinkClient.class),
                userManager,
                mock(AgentExecutionPool.class),
                mock(CancellationManager.class));

        WechatMessage message = new WechatMessage();
        message.setType(MessageType.TEXT);
        message.setText("最近有什么值得关注的事情吗");

        // Agent 执行现在是异步的，handle() 始终返回 silent
        WechatReply reply = chatHandler.handle(message);

        assertTrue(reply.isSilent());
    }

    // ==================== 取消命令 startsWith 匹配 ====================

    @Test
    void exactCancelKeywordShouldTriggerCancel() {
        WechatILinkClient wechatClient = mock(WechatILinkClient.class);
        WechatUserManager userManager = mock(WechatUserManager.class);
        when(userManager.getOwnerUserId()).thenReturn("test-user");
        CancellationManager cancelMgr = mock(CancellationManager.class);

        ChatHandler chatHandler = createChatHandler(wechatClient, userManager, cancelMgr);

        WechatMessage message = textMessage("算了");
        WechatReply reply = chatHandler.handle(message);

        assertTrue(reply.isSilent());
        verify(cancelMgr).cancel("test-user");
        verify(wechatClient).sendTextMessage("test-user", "👌 已取消当前任务。");
    }

    @Test
    void phraseStartingWithCancelKeywordShouldTriggerCancel() {
        WechatILinkClient wechatClient = mock(WechatILinkClient.class);
        WechatUserManager userManager = mock(WechatUserManager.class);
        when(userManager.getOwnerUserId()).thenReturn("test-user");
        CancellationManager cancelMgr = mock(CancellationManager.class);

        ChatHandler chatHandler = createChatHandler(wechatClient, userManager, cancelMgr);

        // 模拟真实场景："算了，太慢了，别画了"
        WechatMessage message = textMessage("算了，太慢了，别画了");
        WechatReply reply = chatHandler.handle(message);

        assertTrue(reply.isSilent());
        verify(cancelMgr).cancel("test-user");
        verify(wechatClient).sendTextMessage(eq("test-user"), contains("取消"));
    }

    @Test
    void stopWithExtraTextShouldTriggerCancel() {
        WechatILinkClient wechatClient = mock(WechatILinkClient.class);
        WechatUserManager userManager = mock(WechatUserManager.class);
        when(userManager.getOwnerUserId()).thenReturn("test-user");
        CancellationManager cancelMgr = mock(CancellationManager.class);

        ChatHandler chatHandler = createChatHandler(wechatClient, userManager, cancelMgr);

        WechatMessage message = textMessage("停止生成图片");
        WechatReply reply = chatHandler.handle(message);

        assertTrue(reply.isSilent());
        verify(cancelMgr).cancel("test-user");
        verify(wechatClient).sendTextMessage(eq("test-user"), contains("取消"));
    }

    @Test
    void keywordInMiddleShouldNotTriggerCancel() {
        // "如何取消订单" — 包含"取消"但不在开头，不应触发
        WechatILinkClient wechatClient = mock(WechatILinkClient.class);
        WechatUserManager userManager = mock(WechatUserManager.class);
        when(userManager.getOwnerUserId()).thenReturn("test-user");
        CancellationManager cancelMgr = mock(CancellationManager.class);
        AgentExecutionPool pool = mock(AgentExecutionPool.class);

        ChatHandler chatHandler = new ChatHandler(
                mock(ReActAgentExecutor.class),
                mock(VoiceTool.class),
                mock(FileGenerationTool.class),
                mock(ImageGenerationTool.class),
                mock(PlaceImageTool.class),
                wechatClient,
                userManager,
                pool,
                cancelMgr);

        WechatMessage message = textMessage("如何取消订单");
        chatHandler.handle(message);

        // 不应该触发取消，而是正常提交到线程池
        verify(cancelMgr, never()).cancel(anyString());
        verify(wechatClient, never()).sendTextMessage(anyString(), contains("取消"));
        verify(pool).execute(eq("test-user"), any(Runnable.class));
    }

    // ==================== 辅助方法 ====================

    private ChatHandler createChatHandler(
            WechatILinkClient wechatClient,
            WechatUserManager userManager,
            CancellationManager cancelMgr) {
        return new ChatHandler(
                mock(ReActAgentExecutor.class),
                mock(VoiceTool.class),
                mock(FileGenerationTool.class),
                mock(ImageGenerationTool.class),
                mock(PlaceImageTool.class),
                wechatClient,
                userManager,
                mock(AgentExecutionPool.class),
                cancelMgr);
    }

    private static WechatMessage textMessage(String text) {
        WechatMessage message = new WechatMessage();
        message.setType(MessageType.TEXT);
        message.setText(text);
        return message;
    }
}