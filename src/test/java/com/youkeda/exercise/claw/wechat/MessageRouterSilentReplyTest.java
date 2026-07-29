package com.youkeda.exercise.claw.wechat;

import com.youkeda.exercise.claw.agent.tool.ChatTool;
import com.youkeda.exercise.claw.agent.tool.FileTool;
import com.youkeda.exercise.claw.agent.tool.SimpleReplyTool;
import com.youkeda.exercise.claw.agent.tool.VisionTool;
import com.youkeda.exercise.claw.agent.tool.VoiceFunction;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class MessageRouterSilentReplyTest {

    @Test
    void passesHandledSilentReplyWithoutFallbackMessage() {
        ChatTool chatTool = mock(ChatTool.class);
        VisionTool visionTool = mock(VisionTool.class);
        SimpleReplyTool fallbackTool = mock(SimpleReplyTool.class);
        VoiceFunction voiceTool = mock(VoiceFunction.class);
        FileTool fileTool = mock(FileTool.class);
        MessageRouter router = new MessageRouter(
                chatTool, visionTool, fallbackTool, voiceTool, fileTool);

        WechatMessage message = new WechatMessage();
        message.setType(MessageType.TEXT);
        message.setText("最近有什么值得关注的事情吗");
        when(chatTool.handle(message)).thenReturn(WechatReply.silent());

        WechatReply reply = router.route(message);

        assertTrue(reply.isSilent());
        verifyNoInteractions(fallbackTool);
    }
}
