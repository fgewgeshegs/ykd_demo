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

class MessageRouterTest {

    @Test
    void handledReplyDoesNotTriggerFallback() {
        ChatTool chatTool = mock(ChatTool.class);
        VisionTool visionTool = mock(VisionTool.class);
        SimpleReplyTool fallbackTool = mock(SimpleReplyTool.class);
        VoiceFunction voiceTool = mock(VoiceFunction.class);
        FileTool fileTool = mock(FileTool.class);
        MessageRouter router = new MessageRouter(
                chatTool, visionTool, fallbackTool, voiceTool, fileTool);

        WechatMessage message = new WechatMessage();
        message.setType(MessageType.TEXT);
        message.setUserId("owner");
        message.setText("最近有什么值得关注的？");
        when(chatTool.handle(message)).thenReturn(WechatReply.handled());

        WechatReply reply = router.route(message);

        assertTrue(reply.isHandledWithoutReply());
        verifyNoInteractions(fallbackTool);
    }
}
