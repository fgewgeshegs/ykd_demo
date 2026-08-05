package com.youkeda.exercise.claw.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.memory.Message;
import com.youkeda.exercise.claw.agent.model.PlanState;
import com.youkeda.exercise.claw.agent.plan.DefaultPlanStore;
import com.youkeda.exercise.claw.agent.plan.PlanStore;
import com.youkeda.exercise.claw.agent.plan.PlanValidator;
import com.youkeda.exercise.claw.agent.runtime.ExecutionLoop;
import com.youkeda.exercise.claw.agent.runtime.SkillReplyGuardRegistry;
import com.youkeda.exercise.claw.agent.runtime.ToolExecutionContext;
import com.youkeda.exercise.claw.agent.runtime.ToolExecutor;
import com.youkeda.exercise.claw.agent.skill.PendingToolCoordinator;
import com.youkeda.exercise.claw.agent.skill.SkillSession;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.ai.llm.LLMResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Agent Interrupt 最小版本单元测试。
 *
 * <p>覆盖三个核心场景：
 * <ol>
 *   <li>正常任务不受影响</li>
 *   <li>用户取消后停止后续 tool 调用</li>
 *   <li>多用户互不影响</li>
 * </ol>
 */
class AgentInterruptTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== CancellationManager 单元测试 ====================

    @Test
    void cancelThenIsCancelledShouldReturnTrue() {
        CancellationManager manager = new CancellationManager();
        assertFalse(manager.isCancelled("user-a"));

        manager.cancel("user-a");
        assertTrue(manager.isCancelled("user-a"));
    }

    @Test
    void clearShouldResetCancelledState() {
        CancellationManager manager = new CancellationManager();
        manager.cancel("user-a");
        assertTrue(manager.isCancelled("user-a"));

        manager.clear("user-a");
        assertFalse(manager.isCancelled("user-a"));
    }

    @Test
    void isCancelledForUnknownUserShouldReturnFalse() {
        CancellationManager manager = new CancellationManager();
        assertFalse(manager.isCancelled("no-such-user"));
    }

    @Test
    void cancelDifferentUsersShouldNotInterfere() {
        CancellationManager manager = new CancellationManager();
        manager.cancel("user-a");
        assertTrue(manager.isCancelled("user-a"));
        assertFalse(manager.isCancelled("user-b"));
    }

    // ==================== 场景 1：正常任务不受影响 ====================

    @Test
    void normalTaskShouldCompleteWithoutInterruption() {
        // 构造：LLM 第一轮调用工具，第二轮返回文本
        LLMClient llm = mock(LLMClient.class);
        when(llm.chatWithTools(anyString(), anyList(), anyList()))
                .thenReturn(new LLMResponse(null,
                        List.of(new LLMResponse.ToolCall("tc1", "dummy_tool", "{}")),
                        "tool_calls"))
                .thenReturn(new LLMResponse("任务已完成。", List.of(), "stop"));

        CancellationManager cancellationManager = new CancellationManager();
        // 不调用 cancel()——保持正常状态

        ExecutionLoop loop = createExecutionLoop(llm, cancellationManager);

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", "帮我查询信息"));

        ExecutionLoop.Result result = loop.run(
                "sys", messages, List.of(), null,
                new ToolExecutionContext("帮我查询信息", null, "user-normal"),
                SkillSession.create("u"), "req", "test", "帮我查询信息");

        assertEquals(ExecutionLoop.LoopStatus.TEXT_REPLY, result.status());
        assertEquals("任务已完成。", result.reply());
        verify(llm, times(2)).chatWithTools(anyString(), anyList(), anyList());
    }

    // ==================== 场景 2：用户取消后停止后续 tool 调用 ====================

    @Test
    void cancelBeforeLlmCallShouldReturnCancelledImmediately() {
        LLMClient llm = mock(LLMClient.class);
        CancellationManager cancellationManager = new CancellationManager();

        // 在循环开始前就取消
        cancellationManager.cancel("user-interrupt");

        ExecutionLoop loop = createExecutionLoop(llm, cancellationManager);

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", "帮我查询信息"));

        ExecutionLoop.Result result = loop.run(
                "sys", messages, List.of(), null,
                new ToolExecutionContext("帮我查询信息", null, "user-interrupt"),
                SkillSession.create("u"), "req", "test", "帮我查询信息");

        // 应该在检查点1（LLM调用前）就返回 CANCELLED
        assertEquals(ExecutionLoop.LoopStatus.CANCELLED, result.status());
        // LLM 不应该被调用
        verify(llm, never()).chatWithTools(anyString(), anyList(), anyList());
    }

    @Test
    void cancelAfterLlmResponseShouldStopBeforeToolExecution() {
        LLMClient llm = mock(LLMClient.class);
        when(llm.chatWithTools(anyString(), anyList(), anyList()))
                .thenReturn(new LLMResponse(null,
                        List.of(new LLMResponse.ToolCall("tc1", "dummy_tool", "{}"),
                                new LLMResponse.ToolCall("tc2", "another_tool", "{}")),
                        "tool_calls"));

        CancellationManager cancellationManager = new CancellationManager();

        // 第一轮 LLM 调用后触发取消（检查点2）
        // 用 AtomicBoolean 控制取消时机
        AtomicBoolean cancelTriggered = new AtomicBoolean(false);
        ExecutionLoop loop = createExecutionLoopWithConditionalCancel(
                llm, cancellationManager, "user-cond", cancelTriggered);

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", "执行多个操作"));

        ExecutionLoop.Result result = loop.run(
                "sys", messages, List.of(), null,
                new ToolExecutionContext("执行多个操作", null, "user-cond"),
                SkillSession.create("u"), "req", "test", "执行多个操作");

        // 检查点2（LLM返回后）应检测到取消
        assertEquals(ExecutionLoop.LoopStatus.CANCELLED, result.status());
        // LLM 被调用了1次（返回了 tool_calls），工具不应该被执行
        verify(llm, times(1)).chatWithTools(anyString(), anyList(), anyList());
    }

    @Test
    void cancelShouldPreventToolExecution() {
        // 构造一个真实的 ToolExecutor 来验证工具未被调用
        com.youkeda.exercise.claw.agent.runtime.ToolRegistry registry =
                new com.youkeda.exercise.claw.agent.runtime.ToolRegistry();
        AtomicBoolean toolExecuted = new AtomicBoolean(false);
        registry.register(new com.youkeda.exercise.claw.agent.runtime.Tool() {
            @Override
            public String getName() { return "expensive_tool"; }
            @Override
            public String getDescription() { return "耗时工具"; }
            @Override
            public com.fasterxml.jackson.databind.JsonNode getParameters() {
                return objectMapper.createObjectNode().put("type", "object");
            }
            @Override
            public String execute(String argumentsJson,
                                com.youkeda.exercise.claw.agent.runtime.ToolExecutionContext context) {
                toolExecuted.set(true);
                return "{\"status\":\"SUCCESS\"}";
            }
        });

        ToolExecutor toolExecutor = new ToolExecutor(
                registry, new SafetyPolicy(),
                mock(com.youkeda.exercise.claw.agent.skill.SkillPendingCoordinator.class),
                mock(PendingToolCoordinator.class),
                mock(com.youkeda.exercise.claw.agent.activity.AgentActivityRecorder.class),
                mock(com.youkeda.exercise.claw.agent.ToolResultStatusParser.class),
                new DefaultPlanStore(), objectMapper);

        LLMClient llm = mock(LLMClient.class);
        when(llm.chatWithTools(anyString(), anyList(), anyList()))
                .thenReturn(new LLMResponse(null,
                        List.of(new LLMResponse.ToolCall("tc1", "expensive_tool", "{}")),
                        "tool_calls"));

        CancellationManager cancellationManager = new CancellationManager();
        // 让 CancellationManager 在第一次检查时返回 false，第二次返回 true
        // 这样 LLM 会调用，返回 tool_calls，然后在检查点3（tool执行前）被拦截
        AtomicBoolean firstCheck = new AtomicBoolean(true);
        CancellationManager conditionalCancel = new CancellationManager() {
            @Override
            public boolean isCancelled(String userId) {
                if ("user-tool-block".equals(userId) && firstCheck.get()) {
                    // 检查点1：让 LLM 通过
                    firstCheck.set(false);
                    return false;
                }
                // 检查点2、3：拦截
                return "user-tool-block".equals(userId);
            }
        };

        ExecutionLoop loop = new ExecutionLoop(
                llm, toolExecutor, mock(PlanStore.class), new PlanValidator(), objectMapper,
                List.of(), new SkillReplyGuardRegistry(List.of()), conditionalCancel);

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", "执行耗时操作"));

        ExecutionLoop.Result result = loop.run(
                "sys", messages, List.of(), null,
                new ToolExecutionContext("执行耗时操作", null, "user-tool-block"),
                SkillSession.create("u"), "req", "test", "执行耗时操作");

        assertEquals(ExecutionLoop.LoopStatus.CANCELLED, result.status());
        // 工具不能被执行
        assertFalse(toolExecuted.get(), "取消后工具不应被执行");
    }

    // ==================== 场景 3：多用户互不影响 ====================

    @Test
    void cancellingOneUserShouldNotAffectAnother() {
        CancellationManager cancellationManager = new CancellationManager();
        cancellationManager.cancel("user-a");

        // user-a 被取消
        LLMClient llmA = mock(LLMClient.class);
        ExecutionLoop loopA = createExecutionLoop(llmA, cancellationManager);
        List<Message> messagesA = new ArrayList<>();
        messagesA.add(new Message("user", "user-a task"));
        ExecutionLoop.Result resultA = loopA.run(
                "sys", messagesA, List.of(), null,
                new ToolExecutionContext("user-a task", null, "user-a"),
                SkillSession.create("u"), "req", "test", "user-a task");
        assertEquals(ExecutionLoop.LoopStatus.CANCELLED, resultA.status());
        verify(llmA, never()).chatWithTools(anyString(), anyList(), anyList());

        // user-b 不受影响，正常完成
        LLMClient llmB = mock(LLMClient.class);
        when(llmB.chatWithTools(anyString(), anyList(), anyList()))
                .thenReturn(new LLMResponse("user-b 任务完成。", List.of(), "stop"));
        ExecutionLoop loopB = createExecutionLoop(llmB, cancellationManager);
        List<Message> messagesB = new ArrayList<>();
        messagesB.add(new Message("user", "user-b task"));
        ExecutionLoop.Result resultB = loopB.run(
                "sys", messagesB, List.of(), null,
                new ToolExecutionContext("user-b task", null, "user-b"),
                SkillSession.create("u"), "req", "test", "user-b task");
        assertEquals(ExecutionLoop.LoopStatus.TEXT_REPLY, resultB.status());
        assertEquals("user-b 任务完成。", resultB.reply());
        verify(llmB, times(1)).chatWithTools(anyString(), anyList(), anyList());
    }

    @Test
    void clearCancelAndResubmitShouldAllowNewTask() {
        CancellationManager cancellationManager = new CancellationManager();
        String userId = "user-resume";

        // 第一次：取消
        cancellationManager.cancel(userId);
        LLMClient llm1 = mock(LLMClient.class);
        ExecutionLoop loop1 = createExecutionLoop(llm1, cancellationManager);
        List<Message> messages1 = new ArrayList<>();
        messages1.add(new Message("user", "task 1"));
        ExecutionLoop.Result result1 = loop1.run(
                "sys", messages1, List.of(), null,
                new ToolExecutionContext("task 1", null, userId),
                SkillSession.create("u"), "req", "test", "task 1");
        assertEquals(ExecutionLoop.LoopStatus.CANCELLED, result1.status());

        // 清除取消标记
        cancellationManager.clear(userId);

        // 第二次：新任务正常执行
        LLMClient llm2 = mock(LLMClient.class);
        when(llm2.chatWithTools(anyString(), anyList(), anyList()))
                .thenReturn(new LLMResponse("task 2 完成。", List.of(), "stop"));
        ExecutionLoop loop2 = createExecutionLoop(llm2, cancellationManager);
        List<Message> messages2 = new ArrayList<>();
        messages2.add(new Message("user", "task 2"));
        ExecutionLoop.Result result2 = loop2.run(
                "sys", messages2, List.of(), null,
                new ToolExecutionContext("task 2", null, userId),
                SkillSession.create("u"), "req", "test", "task 2");
        assertEquals(ExecutionLoop.LoopStatus.TEXT_REPLY, result2.status());
        assertEquals("task 2 完成。", result2.reply());
    }

    // ==================== AgentExecutionPool 单元测试 ====================

    @Test
    void agentExecutionPoolShouldSerializeTasksForSameUser() throws Exception {
        AgentExecutionPool pool = new AgentExecutionPool(2);
        List<String> executionOrder = new ArrayList<>();

        Runnable task1 = () -> {
            executionOrder.add("task1-start");
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            executionOrder.add("task1-end");
        };
        Runnable task2 = () -> {
            executionOrder.add("task2-start");
            executionOrder.add("task2-end");
        };

        pool.execute("user-x", task1);
        pool.execute("user-x", task2);

        // 等待异步任务完成
        Thread.sleep(300);

        // 同一 userId 的任务应串行：task1 完成后 task2 才开始
        int task1Start = executionOrder.indexOf("task1-start");
        int task1End = executionOrder.indexOf("task1-end");
        int task2Start = executionOrder.indexOf("task2-start");

        assertTrue(task1Start < task1End, "task1 应先开始后结束");
        assertTrue(task1End < task2Start,
                "task2 应在 task1 完成后才开始（同用户串行）");
    }

    @Test
    void agentExecutionPoolShouldParallelizeDifferentUsers() throws Exception {
        AgentExecutionPool pool = new AgentExecutionPool(4);
        List<String> executionOrder = new ArrayList<>();

        Runnable taskA = () -> {
            executionOrder.add("A-start");
            try { Thread.sleep(150); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            executionOrder.add("A-end");
        };
        Runnable taskB = () -> {
            executionOrder.add("B-start");
            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            executionOrder.add("B-end");
        };

        pool.execute("user-a", taskA);
        pool.execute("user-b", taskB);

        Thread.sleep(300);

        // 不同用户应并行：B 在 A 完成之前就结束了
        int aEnd = executionOrder.indexOf("A-end");
        int bEnd = executionOrder.indexOf("B-end");
        assertTrue(bEnd < aEnd,
                "user-b 任务应在 user-a 之前完成（不同用户并行）");
    }

    // ==================== 审查专项：不重复取消回复 ====================

    @Test
    void cancelledResultShouldNotTriggerDuplicateReply() {
        // CANCELLED 状态只做清理，不返回文本——取消确认已由 ChatHandler poll 线程发送
        CancellationManager manager = new CancellationManager();
        manager.cancel("user-x");

        LLMClient llm = mock(LLMClient.class);
        ExecutionLoop loop = createExecutionLoop(llm, manager);

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", "查询"));
        ExecutionLoop.Result result = loop.run(
                "sys", messages, List.of(), null,
                new ToolExecutionContext("查询", null, "user-x"),
                SkillSession.create("u"), "req", "test", "查询");

        assertEquals(ExecutionLoop.LoopStatus.CANCELLED, result.status());
        // reply 应为 null（CANCELLED 不携带文本，避免 ChatHandler 二次发送）
        assertNull(result.reply(), "CANCELLED 状态不应携带 reply 文本，防止重复回复");
        verify(llm, never()).chatWithTools(anyString(), anyList(), anyList());
    }

    // ==================== 审查专项：RejectedExecutionException 不泄漏锁 ====================

    @Test
    void rejectedExecutionShouldNotLeaveLockHeld() throws Exception {
        AgentExecutionPool pool = new AgentExecutionPool(1);
        // 立即关闭线程池，后续 submit 会抛 RejectedExecutionException
        pool.shutdown();

        try {
            pool.execute("user-x", () -> {});
        } catch (RejectedExecutionException expected) {
            // 预期行为
        }

        // 重新创建线程池，验证同一 userId 的新任务能正常提交执行
        AgentExecutionPool newPool = new AgentExecutionPool(1);
        List<String> results = new ArrayList<>();
        newPool.execute("user-x", () -> results.add("done"));
        Thread.sleep(100);
        newPool.shutdown();

        assertEquals(List.of("done"), results,
                "RejectedExecutionException 后，新任务应能正常提交执行，锁未被泄漏");
    }

    @Test
    void shutdownPoolSubmitShouldThrowAndNotBlockCaller() {
        AgentExecutionPool pool = new AgentExecutionPool(1);
        pool.shutdown();

        assertThrows(RejectedExecutionException.class,
                () -> pool.execute("user-z", () -> {}),
                "线程池关闭后 submit 应立即抛出 RejectedExecutionException");
    }

    // ==================== 审查专项：无运行任务时取消 ====================

    @Test
    void cancelWithNoRunningTaskShouldNotAffectNextTask() {
        CancellationManager manager = new CancellationManager();
        String userId = "user-idle";

        // 无运行任务时发送取消
        manager.cancel(userId);
        assertTrue(manager.isCancelled(userId));

        // 下一个任务启动时 clear 应重置状态
        manager.clear(userId);
        assertFalse(manager.isCancelled(userId));

        // 后续任务正常执行
        LLMClient llm = mock(LLMClient.class);
        when(llm.chatWithTools(anyString(), anyList(), anyList()))
                .thenReturn(new LLMResponse("正常回复。", List.of(), "stop"));

        ExecutionLoop loop = createExecutionLoop(llm, manager);
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", "查询"));
        ExecutionLoop.Result result = loop.run(
                "sys", messages, List.of(), null,
                new ToolExecutionContext("查询", null, userId),
                SkillSession.create("u"), "req", "test", "查询");

        assertEquals(ExecutionLoop.LoopStatus.TEXT_REPLY, result.status());
        assertEquals("正常回复。", result.reply());
    }

    // ==================== 审查专项：异常不导致锁永久占用 ====================

    @Test
    void taskThrowingExceptionShouldStillReleaseLock() throws Exception {
        AgentExecutionPool pool = new AgentExecutionPool(2);
        AtomicBoolean taskRan = new AtomicBoolean(false);
        AtomicBoolean secondTaskRan = new AtomicBoolean(false);

        // 第一个任务抛异常
        pool.execute("user-lock", () -> {
            taskRan.set(true);
            throw new RuntimeException("模拟执行异常");
        });

        // 等待第一个任务完成（包括异常传播）
        Thread.sleep(100);

        // 第二个同 userId 任务应能正常执行（证明锁已释放）
        pool.execute("user-lock", () -> secondTaskRan.set(true));
        Thread.sleep(100);
        pool.shutdown();

        assertTrue(taskRan.get(), "第一个任务应已执行");
        assertTrue(secondTaskRan.get(),
                "异常发生后锁应被释放，同 userId 后续任务应能执行");
    }

    @Test
    void errorInTaskShouldNotLeakLock() throws Exception {
        AgentExecutionPool pool = new AgentExecutionPool(2);
        List<String> order = new ArrayList<>();

        // task-1 抛出 Error（模拟 OOM 等严重错误）
        pool.execute("user-err", () -> {
            order.add("task1-start");
            throw new OutOfMemoryError("模拟 OOM");
        });

        Thread.sleep(100);

        // task-2 同 userId 应在 task-1 后执行（锁未被泄漏）
        pool.execute("user-err", () -> {
            order.add("task2-start");
            order.add("task2-end");
        });
        Thread.sleep(100);
        pool.shutdown();

        int task1Idx = order.indexOf("task1-start");
        int task2Idx = order.indexOf("task2-start");
        assertTrue(task1Idx >= 0, "task1 应已开始执行");
        assertTrue(task2Idx >= 0,
                "即使 task1 抛出 Error，锁也应在 finally 中释放，task2 应能执行");
        assertTrue(task1Idx < task2Idx,
                "task2 必须在 task1 之后执行（串行保证）");
    }

    // ==================== 审查专项：消息乱序 ====================

    @Test
    void sameUserTasksShouldNeverOverlap() throws Exception {
        // 核心保证：同一 userId 的任务串行执行（同一时刻最多一个在执行）
        // 注意：非公平 ReentrantLock 不保证严格 FIFO 提交顺序，但保证互斥
        AgentExecutionPool pool = new AgentExecutionPool(2);
        AtomicBoolean overlapping = new AtomicBoolean(false);
        AtomicBoolean taskRunning = new AtomicBoolean(false);
        List<String> events = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            final int seq = i;
            pool.execute("user-serial", () -> {
                // 检测重叠：如果上一个任务还在运行，说明并发执行了
                if (taskRunning.get()) {
                    overlapping.set(true);
                }
                taskRunning.set(true);
                events.add("task" + seq + "-start");
                try { Thread.sleep(20); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                events.add("task" + seq + "-end");
                taskRunning.set(false);
            });
        }

        Thread.sleep(400);
        pool.shutdown();

        assertFalse(overlapping.get(),
                "同一 userId 的任务不得重叠执行（串行保证）");
        assertTrue(events.contains("task1-start") && events.contains("task5-end"),
                "所有任务应完整执行");
    }

    // ==================== 辅助方法 ====================

    private ExecutionLoop createExecutionLoop(LLMClient llm, CancellationManager cancellationManager) {
        com.youkeda.exercise.claw.agent.runtime.ToolRegistry registry =
                new com.youkeda.exercise.claw.agent.runtime.ToolRegistry();
        registry.register(new com.youkeda.exercise.claw.agent.runtime.Tool() {
            @Override
            public String getName() { return "dummy_tool"; }
            @Override
            public String getDescription() { return "测试工具"; }
            @Override
            public com.fasterxml.jackson.databind.JsonNode getParameters() {
                return objectMapper.createObjectNode().put("type", "object");
            }
            @Override
            public String execute(String argumentsJson,
                                com.youkeda.exercise.claw.agent.runtime.ToolExecutionContext context) {
                return "{\"status\":\"SUCCESS\"}";
            }
        });
        registry.register(new com.youkeda.exercise.claw.agent.runtime.Tool() {
            @Override
            public String getName() { return "another_tool"; }
            @Override
            public String getDescription() { return "另一个工具"; }
            @Override
            public com.fasterxml.jackson.databind.JsonNode getParameters() {
                return objectMapper.createObjectNode().put("type", "object");
            }
            @Override
            public String execute(String argumentsJson,
                                com.youkeda.exercise.claw.agent.runtime.ToolExecutionContext context) {
                return "{\"status\":\"SUCCESS\"}";
            }
        });

        ToolExecutor toolExecutor = new ToolExecutor(
                registry, new SafetyPolicy(),
                mock(com.youkeda.exercise.claw.agent.skill.SkillPendingCoordinator.class),
                mock(PendingToolCoordinator.class),
                mock(com.youkeda.exercise.claw.agent.activity.AgentActivityRecorder.class),
                mock(com.youkeda.exercise.claw.agent.ToolResultStatusParser.class),
                new DefaultPlanStore(), objectMapper);

        return new ExecutionLoop(
                llm, toolExecutor, mock(PlanStore.class), new PlanValidator(), objectMapper,
                List.of(), new SkillReplyGuardRegistry(List.of()), cancellationManager);
    }

    private ExecutionLoop createExecutionLoopWithConditionalCancel(
            LLMClient llm, CancellationManager baseManager,
            String userId, AtomicBoolean cancelTriggered) {
        com.youkeda.exercise.claw.agent.runtime.ToolRegistry registry =
                new com.youkeda.exercise.claw.agent.runtime.ToolRegistry();
        registry.register(new com.youkeda.exercise.claw.agent.runtime.Tool() {
            @Override
            public String getName() { return "dummy_tool"; }
            @Override
            public String getDescription() { return "测试工具"; }
            @Override
            public com.fasterxml.jackson.databind.JsonNode getParameters() {
                return objectMapper.createObjectNode().put("type", "object");
            }
            @Override
            public String execute(String argumentsJson,
                                com.youkeda.exercise.claw.agent.runtime.ToolExecutionContext context) {
                return "{\"status\":\"SUCCESS\"}";
            }
        });

        ToolExecutor toolExecutor = new ToolExecutor(
                registry, new SafetyPolicy(),
                mock(com.youkeda.exercise.claw.agent.skill.SkillPendingCoordinator.class),
                mock(PendingToolCoordinator.class),
                mock(com.youkeda.exercise.claw.agent.activity.AgentActivityRecorder.class),
                mock(com.youkeda.exercise.claw.agent.ToolResultStatusParser.class),
                new DefaultPlanStore(), objectMapper);

        // 让取消在检查点2触发：第一轮LLM返回后
        CancellationManager conditionalCancel = new CancellationManager() {
            @Override
            public boolean isCancelled(String uid) {
                if (userId.equals(uid) && cancelTriggered.compareAndSet(false, true)) {
                    // 检查点1：放行
                    return false;
                }
                // 检查点2及之后：拦截
                return userId.equals(uid);
            }
        };

        return new ExecutionLoop(
                llm, toolExecutor, mock(PlanStore.class), new PlanValidator(), objectMapper,
                List.of(), new SkillReplyGuardRegistry(List.of()), conditionalCancel);
    }
}