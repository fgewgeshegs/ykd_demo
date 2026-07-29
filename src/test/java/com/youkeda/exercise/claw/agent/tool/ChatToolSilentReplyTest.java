package com.youkeda.exercise.claw.agent.tool;

import com.youkeda.exercise.claw.agent.ReActAgentExecutor;
import com.youkeda.exercise.claw.map.PlaceImageFunction;
import com.youkeda.exercise.claw.wechat.client.WechatILinkClient;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import com.youkeda.exercise.claw.wechat.user.WechatUserManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatToolSilentReplyTest {

    @Test
    void convertsHandledAgentResultToSilentWechatReply() {
        ReActAgentExecutor executor = mock(ReActAgentExecutor.class);
        ChatTool chatTool = new ChatTool(
                executor,
                mock(VoiceFunction.class),
                mock(FileGenerationTool.class),
                mock(ImageGenerationTool.class),
                mock(PlaceImageFunction.class),
                mock(WechatILinkClient.class),
                mock(WechatUserManager.class));
        when(executor.execute(any())).thenReturn(ReActAgentExecutor.SILENT_REPLY);

        WechatMessage message = new WechatMessage();
        message.setType(MessageType.TEXT);
        message.setText("最近有什么值得关注的事情吗");

        WechatReply reply = chatTool.handle(message);

        assertTrue(reply.isSilent());
    }
}
