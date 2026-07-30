package com.youkeda.exercise.claw.wechat;

import com.youkeda.exercise.claw.infrastructure.channel.wechat.handler.ChatHandler;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.handler.FileHandler;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.handler.SimpleReplyHandler;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.handler.VisionHandler;
import com.youkeda.exercise.claw.tool.voice.VoiceTool;
import com.youkeda.exercise.claw.schedule.CourseImportHandler;
import com.youkeda.exercise.claw.schedule.CourseImportStateManager;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class MessageRouterSilentReplyTest {

    @Test
    void passesHandledSilentReplyWithoutFallbackMessage() {
        ChatHandler chatTool = mock(ChatHandler.class);
        VisionHandler visionHandler = mock(VisionHandler.class);
        SimpleReplyHandler fallbackTool = mock(SimpleReplyHandler.class);
        VoiceTool voiceTool = mock(VoiceTool.class);
        FileHandler fileHandler = mock(FileHandler.class);
        CourseImportStateManager importStateManager = mock(CourseImportStateManager.class);
        CourseImportHandler importHandler = mock(CourseImportHandler.class);
        MessageRouter router = new MessageRouter(
                chatTool, visionHandler, fallbackTool, voiceTool, fileHandler, importStateManager, importHandler);

        WechatMessage message = new WechatMessage();
        message.setType(MessageType.TEXT);
        message.setText("最近有什么值得关注的事情吗");
        when(chatTool.handle(message)).thenReturn(WechatReply.silent());

        WechatReply reply = router.route(message);

        assertTrue(reply.isSilent());
        verifyNoInteractions(fallbackTool);
    }
}
