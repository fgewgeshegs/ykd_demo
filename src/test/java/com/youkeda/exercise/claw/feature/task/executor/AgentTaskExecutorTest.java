package com.youkeda.exercise.claw.feature.task.executor;

import com.youkeda.exercise.claw.agent.ReActAgentExecutor;
import com.youkeda.exercise.claw.feature.task.model.ScheduledTask;
import com.youkeda.exercise.claw.infrastructure.channel.NotificationRouter;
import com.youkeda.exercise.claw.infrastructure.channel.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AgentTaskExecutor 单元测试
 *
 * <p>覆盖：
 * - Agent 任务正常执行流程
 * - Agent 任务执行结果通过 NotificationRouter 发送
 * - 异常场景
 */
@ExtendWith(MockitoExtension.class)
class AgentTaskExecutorTest {

    @Mock
    private ReActAgentExecutor agentExecutor;

    @Mock
    private NotificationRouter notificationRouter;

    private AgentTaskExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new AgentTaskExecutor(agentExecutor, notificationRouter);
    }

    @Test
    void shouldExecuteAgentTaskAndSendResult() throws Exception {
        ScheduledTask task = new ScheduledTask("test_user", "总结今天AI新闻",
                java.time.LocalDateTime.now().plusMinutes(30));
        task.setTaskType(ScheduledTask.TASK_TYPE_AGENT);

        when(agentExecutor.execute(any())).thenReturn("这是今天的AI新闻总结：...");
        when(notificationRouter.send(anyString(), any(), anyString())).thenReturn(true);

        executor.execute(task);

        ArgumentCaptor<com.youkeda.exercise.claw.agent.AgentContext> contextCaptor =
                ArgumentCaptor.forClass(com.youkeda.exercise.claw.agent.AgentContext.class);
        verify(agentExecutor, times(1)).execute(contextCaptor.capture());

        com.youkeda.exercise.claw.agent.AgentContext ctx = contextCaptor.getValue();
        assertEquals("test_user", ctx.getUserId());
        assertEquals("总结今天AI新闻", ctx.getMessage());
        assertEquals(com.youkeda.exercise.claw.infrastructure.channel.wechat.model.MessageType.TEXT, ctx.getMessageType());

        verify(notificationRouter, times(1)).send(eq("test_user"), eq(NotificationType.AGENT_RESULT), anyString());
    }

    @Test
    void shouldHandleAgentExecutionError() {
        ScheduledTask task = new ScheduledTask("error_user", "查询天气",
                java.time.LocalDateTime.now().plusMinutes(5));
        task.setTaskType(ScheduledTask.TASK_TYPE_AGENT);

        when(agentExecutor.execute(any())).thenThrow(new RuntimeException("LLM 调用超时"));

        assertThrows(RuntimeException.class, () -> executor.execute(task));

        verify(notificationRouter, never()).send(anyString(), any(), anyString());
    }

    @Test
    void shouldIncludeTaskContentInContext() throws Exception {
        String agentContent = "帮我写一份项目周报";
        ScheduledTask task = new ScheduledTask("user_w", agentContent,
                java.time.LocalDateTime.now().plusHours(1));
        task.setTaskType(ScheduledTask.TASK_TYPE_AGENT);

        when(agentExecutor.execute(any())).thenReturn("周报已生成：...");
        when(notificationRouter.send(anyString(), any(), anyString())).thenReturn(true);

        executor.execute(task);

        ArgumentCaptor<com.youkeda.exercise.claw.agent.AgentContext> captor =
                ArgumentCaptor.forClass(com.youkeda.exercise.claw.agent.AgentContext.class);
        verify(agentExecutor).execute(captor.capture());
        assertEquals(agentContent, captor.getValue().getMessage());
    }

    @Test
    void shouldNotSendNotificationWhenAgentFails() {
        ScheduledTask task = new ScheduledTask("user_fail", "提醒任务",
                java.time.LocalDateTime.now().plusMinutes(10));
        task.setTaskType(ScheduledTask.TASK_TYPE_AGENT);

        when(agentExecutor.execute(any())).thenThrow(new RuntimeException("Agent 执行异常"));

        assertThrows(RuntimeException.class, () -> executor.execute(task));

        verify(notificationRouter, never()).send(anyString(), any(), anyString());
    }
}