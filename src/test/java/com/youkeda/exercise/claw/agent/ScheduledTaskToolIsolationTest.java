package com.youkeda.exercise.claw.agent;

import com.youkeda.exercise.claw.feature.task.executor.AgentTaskExecutor;
import com.youkeda.exercise.claw.feature.task.model.ScheduledTask;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.client.WechatILinkClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 定时任务执行上下文隔离测试（Fix 1 / D1）。
 *
 * <p>验证：
 * - scheduledTaskExecution=true 时，effectiveTools 不包含任务管理类工具（create_schedule_task 等）
 * - scheduledTaskExecution=false（普通用户请求）时，工具集不受影响
 * - AgentTaskExecutor 创建 AgentContext 时会置 scheduledTaskExecution=true
 * - AgentContext 默认 scheduledTaskExecution=false
 */
@ExtendWith(MockitoExtension.class)
class ScheduledTaskToolIsolationTest {

    @Mock
    private ReActAgentExecutor agentExecutor;

    @Mock
    private WechatILinkClient wechatClient;

    private AgentTaskExecutor taskExecutor;

    @BeforeEach
    void setUp() {
        taskExecutor = new AgentTaskExecutor(agentExecutor, wechatClient);
    }

    @Test
    void shouldRemoveTaskManagementToolsWhenScheduledExecution() {
        Set<String> effectiveTools = new LinkedHashSet<>(Set.of(
                "create_schedule_task",
                "cancel_schedule_task",
                "update_schedule_task",
                "pause_agent_task",
                "resume_agent_task",
                "plan_tasks",
                "execute_plan_tasks",
                "web_search",
                "time_query"
        ));

        Set<String> filtered = ReActAgentExecutor.filterTaskManagementTools(
                effectiveTools, true);

        // 任务管理类工具应全部被移除
        assertFalse(filtered.contains("create_schedule_task"));
        assertFalse(filtered.contains("cancel_schedule_task"));
        assertFalse(filtered.contains("update_schedule_task"));
        assertFalse(filtered.contains("pause_agent_task"));
        assertFalse(filtered.contains("resume_agent_task"));
        assertFalse(filtered.contains("plan_tasks"));
        assertFalse(filtered.contains("execute_plan_tasks"));

        // 普通工具应保留
        assertTrue(filtered.contains("web_search"));
        assertTrue(filtered.contains("time_query"));
    }

    @Test
    void shouldKeepAllToolsForNormalUserRequest() {
        Set<String> effectiveTools = new LinkedHashSet<>(Set.of(
                "create_schedule_task",
                "web_search"
        ));

        Set<String> filtered = ReActAgentExecutor.filterTaskManagementTools(
                effectiveTools, false);

        // 普通用户请求不受影响
        assertTrue(filtered.contains("create_schedule_task"));
        assertTrue(filtered.contains("web_search"));
    }

    @Test
    void agentTaskExecutorShouldMarkScheduledTaskExecution() throws Exception {
        ScheduledTask task = new ScheduledTask("user_iso", "总结AI新闻",
                LocalDateTime.now().plusMinutes(30));
        task.setTaskType(ScheduledTask.TASK_TYPE_AGENT);

        when(agentExecutor.execute(any())).thenReturn("总结完成");

        taskExecutor.execute(task);

        ArgumentCaptor<AgentContext> captor = ArgumentCaptor.forClass(AgentContext.class);
        verify(agentExecutor, times(1)).execute(captor.capture());

        assertTrue(captor.getValue().isScheduledTaskExecution(),
                "定时任务执行上下文应标记 scheduledTaskExecution=true");
    }

    @Test
    void agentContextDefaultsToFalse() {
        AgentContext context = new AgentContext();
        assertFalse(context.isScheduledTaskExecution());
    }

    @Test
    void agentTaskExecutorShouldNotSendWechatWhenAgentFails() {
        ScheduledTask task = new ScheduledTask("user_fail", "查询天气",
                LocalDateTime.now().plusMinutes(5));
        task.setTaskType(ScheduledTask.TASK_TYPE_AGENT);

        when(agentExecutor.execute(any())).thenThrow(new RuntimeException("LLM 超时"));

        assertThrows(RuntimeException.class, () -> taskExecutor.execute(task));
        verify(wechatClient, never()).sendTextMessage(anyString(), anyString());
    }
}
