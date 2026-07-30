package com.youkeda.exercise.claw.infrastructure.channel.wechat.handler;

import com.youkeda.exercise.claw.agent.ReActAgentExecutor;
import com.youkeda.exercise.claw.tool.voice.VoiceTool;
import com.youkeda.exercise.claw.tool.file.FileGenerationTool;
import com.youkeda.exercise.claw.tool.image.ImageGenerationTool;
import com.youkeda.exercise.claw.tool.map.PlaceImageTool;
import com.youkeda.exercise.claw.wechat.client.WechatILinkClient;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import com.youkeda.exercise.claw.wechat.user.WechatUserManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatHandlerSilentReplyTest {

    @Test
    void convertsHandledAgentResultToSilentWechatReply() {
        ReActAgentExecutor executor = mock(ReActAgentExecutor.class);
        ChatHandler chatHandler = new ChatHandler(
                executor,
                mock(VoiceTool.class),
                mock(FileGenerationTool.class),
                mock(ImageGenerationTool.class),
                mock(PlaceImageTool.class),
                mock(WechatILinkClient.class),
                mock(WechatUserManager.class));
        when(executor.execute(any())).thenReturn(ReActAgentExecutor.SILENT_REPLY);

        WechatMessage message = new WechatMessage();
        message.setType(MessageType.TEXT);
        message.setText("最近有什么值得关注的事情吗");

        WechatReply reply = chatHandler.handle(message);

        assertTrue(reply.isSilent());
    }
}
