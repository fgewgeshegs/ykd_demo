package com.youkeda.exercise.claw.agent.runtime;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 任务操作幻觉防护测试（Fix 3 / D3）。
 *
 * <p>验证 ExecutionLoop 的取消/修改请求识别与工具调用检测：
 * - 用户要求取消任务（"删除8点提醒"等）能被识别
 * - 用户要求修改任务能被识别
 * - 普通删除请求（"删除文件""删除聊天记录"）不被误判
 * - cancel_schedule_task / update_schedule_task 是否被调用的检测
 *
 * <p>纠正逻辑本身（注入 system correction message）位于 ExecutionLoop 文本回复分支，
 * 由上述谓词组合触发：请求被识别 + 工具未被调用 → 纠正。
 */
class ScheduleMutationGuardTest {

    // ==================== 取消请求识别 ====================

    @Test
    void shouldRecognizeCancelRequest() {
        assertTrue(ExecutionLoop.isScheduleTaskCancelRequest("删除8点提醒"));
        assertTrue(ExecutionLoop.isScheduleTaskCancelRequest("取消任务"));
        assertTrue(ExecutionLoop.isScheduleTaskCancelRequest("移除提醒"));
        assertTrue(ExecutionLoop.isScheduleTaskCancelRequest("删除定时任务"));
        assertTrue(ExecutionLoop.isScheduleTaskCancelRequest("把推送取消掉"));
        assertTrue(ExecutionLoop.isScheduleTaskCancelRequest("提醒不需要了，帮我删除"));
    }

    @Test
    void shouldNotMisjudgeOrdinaryDeleteRequest() {
        assertFalse(ExecutionLoop.isScheduleTaskCancelRequest("删除文件"));
        assertFalse(ExecutionLoop.isScheduleTaskCancelRequest("删除聊天记录"));
        assertFalse(ExecutionLoop.isScheduleTaskCancelRequest("清理一下消息记录"));
        assertFalse(ExecutionLoop.isScheduleTaskCancelRequest("帮我删掉这张图片"));
        assertFalse(ExecutionLoop.isScheduleTaskCancelRequest("取消订单"));
    }

    // ==================== 修改请求识别 ====================

    @Test
    void shouldRecognizeUpdateRequest() {
        assertTrue(ExecutionLoop.isScheduleTaskUpdateRequest("修改提醒时间"));
        assertTrue(ExecutionLoop.isScheduleTaskUpdateRequest("调整任务"));
        assertTrue(ExecutionLoop.isScheduleTaskUpdateRequest("把8点的提醒改成9点"));
        assertTrue(ExecutionLoop.isScheduleTaskUpdateRequest("提醒提前到7点"));
    }

    @Test
    void shouldNotMisjudgeOrdinaryUpdateRequest() {
        assertFalse(ExecutionLoop.isScheduleTaskUpdateRequest("改一下报告"));
        assertFalse(ExecutionLoop.isScheduleTaskUpdateRequest("帮我改文案"));
        assertFalse(ExecutionLoop.isScheduleTaskUpdateRequest("修改图片尺寸"));
    }

    // ==================== 工具调用检测 ====================

    @Test
    void shouldDetectCancelToolCalled() {
        assertTrue(ExecutionLoop.wasScheduleTaskCancelCalled(Set.of(
                "cancel_schedule_task|{\"task_id\": 8}", "web_search|{}")));
        assertFalse(ExecutionLoop.wasScheduleTaskCancelCalled(Set.of(
                "web_search|{}", "create_schedule_task|{}")));
    }

    @Test
    void shouldDetectUpdateToolCalled() {
        assertTrue(ExecutionLoop.wasScheduleTaskUpdateCalled(Set.of(
                "update_schedule_task|{\"task_id\": 10}")));
        assertFalse(ExecutionLoop.wasScheduleTaskUpdateCalled(Set.of(
                "cancel_schedule_task|{}")));
    }
}