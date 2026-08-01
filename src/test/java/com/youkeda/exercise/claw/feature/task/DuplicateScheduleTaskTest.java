package com.youkeda.exercise.claw.feature.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.feature.task.model.ScheduledTask;
import com.youkeda.exercise.claw.feature.task.repository.ScheduledTaskRepository;
import com.youkeda.exercise.claw.feature.task.service.TaskCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 定时任务创建幂等测试（Fix 2 / D2）。
 *
 * <p>验证：
 * - 完全重复的任务（同用户、同内容、同周期、分钟级相同执行时间、ACTIVE）创建被拒绝
 * - 重复创建时数据库只保留一个任务
 * - 执行时间不同分钟时允许创建
 * - 不同用户允许创建
 */
class DuplicateScheduleTaskTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String TEST_USER = "dup_user";

    @TempDir
    File tempDir;

    private ScheduledTaskRepository repository;
    private TaskCreator taskCreator;

    @BeforeEach
    void setUp() {
        repository = new ScheduledTaskRepository();
        setField(repository, "dbPath", new File(tempDir, "test-tasks.db").getAbsolutePath());
        repository.init();
        taskCreator = new TaskCreator(repository);
    }

    @Test
    void shouldRejectIdenticalTaskCreation() {
        // 第一次创建成功
        TaskCreator.CreateResult first = taskCreator.createFromNode(
                TEST_USER, buildNode("推送AI新闻", "2026-08-02 08:00:00", "DAILY", "AGENT"));
        assertTrue(first.isSuccess());

        // 第二次完全相同（秒不同但分钟相同）→ 拒绝
        TaskCreator.CreateResult second = taskCreator.createFromNode(
                TEST_USER, buildNode("推送AI新闻", "2026-08-02 08:00:59", "DAILY", "AGENT"));
        assertFalse(second.isSuccess());
        assertTrue(second.getErrorMessage().contains("已存在相同任务"));

        // 数据库只有一个任务
        List<ScheduledTask> tasks = repository.findByUserId(TEST_USER);
        assertEquals(1, tasks.size());
    }

    @Test
    void shouldRejectIdenticalReminderTask() {
        // 普通提醒任务同样受幂等保护
        TaskCreator.CreateResult first = taskCreator.createFromNode(
                TEST_USER, buildNode("喝水", "2026-08-02 10:00:00", "NONE", "REMINDER"));
        assertTrue(first.isSuccess());

        TaskCreator.CreateResult second = taskCreator.createFromNode(
                TEST_USER, buildNode("喝水", "2026-08-02 10:00:30", "NONE", "REMINDER"));
        assertFalse(second.isSuccess());

        assertEquals(1, repository.findByUserId(TEST_USER).size());
    }

    @Test
    void shouldAllowCreationWhenMinuteDiffers() {
        taskCreator.createFromNode(TEST_USER, buildNode("推送AI新闻", "2026-08-02 08:00:00", "DAILY", "AGENT"));

        // 不同分钟 → 允许创建
        TaskCreator.CreateResult second = taskCreator.createFromNode(
                TEST_USER, buildNode("推送AI新闻", "2026-08-02 08:05:00", "DAILY", "AGENT"));
        assertTrue(second.isSuccess());

        assertEquals(2, repository.findByUserId(TEST_USER).size());
    }

    @Test
    void shouldAllowCreationForDifferentUser() {
        taskCreator.createFromNode(TEST_USER, buildNode("推送AI新闻", "2026-08-02 08:00:00", "DAILY", "AGENT"));

        // 不同用户 → 允许创建
        TaskCreator.CreateResult other = taskCreator.createFromNode(
                "other_user", buildNode("推送AI新闻", "2026-08-02 08:00:00", "DAILY", "AGENT"));
        assertTrue(other.isSuccess());

        assertEquals(1, repository.findByUserId(TEST_USER).size());
        assertEquals(1, repository.findByUserId("other_user").size());
    }

    // ==================== 工具方法 ====================

    private ObjectNode buildNode(String content, String executeTime,
                                 String repeatType, String taskType) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("content", content);
        node.put("execute_time", executeTime);
        node.put("repeat_type", repeatType);
        node.put("task_type", taskType);
        return node;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}