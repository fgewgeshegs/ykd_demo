package com.youkeda.exercise.claw.feature.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.feature.task.model.ScheduledTask;
import com.youkeda.exercise.claw.feature.task.model.TaskPlan;
import com.youkeda.exercise.claw.feature.task.repository.ScheduledTaskRepository;
import com.youkeda.exercise.claw.feature.task.repository.TaskPlanRepository;
import com.youkeda.exercise.claw.feature.task.service.TaskCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 任务规划模块单元测试
 *
 * <p>覆盖：
 * - 创建计划（preview）
 * - 查询计划（by ID / by user）
 * - 执行计划（批量创建任务）
 * - 用户隔离（不能执行他人的计划）
 * - 取消计划
 * - TaskCreator 创建任务
 */
class TaskPlanningTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String USER_A = "user_a";
    private static final String USER_B = "user_b";

    private ScheduledTaskRepository taskRepository;
    private TaskPlanRepository planRepository;
    private TaskCreator taskCreator;

    @TempDir
    File tempDir;

    @BeforeEach
    void setUp() {
        taskRepository = new ScheduledTaskRepository();
        setField(taskRepository, "dbPath", new File(tempDir, "test-tasks.db").getAbsolutePath());
        taskRepository.init();

        planRepository = new TaskPlanRepository();
        setField(planRepository, "dbPath", new File(tempDir, "test-tasks.db").getAbsolutePath());
        planRepository.init();

        taskCreator = new TaskCreator(taskRepository);
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

    // ==================== 辅助方法 ====================

    private TaskPlan createPlan(String userId, String goal, String tasksJson) {
        TaskPlan plan = new TaskPlan(userId, goal, tasksJson);
        return planRepository.save(plan);
    }

    private String buildTasksJson(String... taskJsons) {
        return "[" + String.join(",", taskJsons) + "]";
    }

    private String taskJson(int order, String content, int delayMinutes) {
        return "{\"order\":" + order + ",\"content\":\"" + content + "\",\"delay_minutes\":" + delayMinutes + "}";
    }

    // ==================== 创建计划（preview） ====================

    @Test
    void shouldCreatePlanInPreviewStatus() {
        String tasksJson = buildTasksJson(
                taskJson(1, "确定比赛方向", 10),
                taskJson(2, "完成Demo", 120),
                taskJson(3, "准备答辩PPT", 240)
        );

        TaskPlan plan = createPlan(USER_A, "准备软件杯比赛", tasksJson);

        assertNotNull(plan.getId());
        assertEquals(USER_A, plan.getUserId());
        assertEquals("准备软件杯比赛", plan.getGoal());
        assertEquals(TaskPlan.STATUS_PREVIEW, plan.getStatus());
        assertTrue(plan.isPreview());
        assertNotNull(plan.getCreatedTime());
    }

    @Test
    void shouldCreatePlanWithSingleTask() {
        String tasksJson = buildTasksJson(taskJson(1, "做一件事", 30));
        TaskPlan plan = createPlan(USER_A, "简单目标", tasksJson);

        assertNotNull(plan.getId());
        assertEquals(1, plan.getGoal().length() > 0 ? 1 : 0);
    }

    @Test
    void shouldCreatePlanWithAbsoluteTime() {
        String futureTime = LocalDateTime.now().plusHours(2).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String tasksJson = "[{\"order\":1,\"content\":\"测试\",\"execute_time\":\"" + futureTime + "\"}]";
        TaskPlan plan = createPlan(USER_A, "测试计划", tasksJson);

        assertNotNull(plan.getId());
        assertEquals(TaskPlan.STATUS_PREVIEW, plan.getStatus());
    }

    // ==================== 查询计划 ====================

    @Test
    void shouldFindPlanById() {
        TaskPlan plan = createPlan(USER_A, "目标", buildTasksJson(taskJson(1, "任务1", 10)));

        TaskPlan found = planRepository.findById(plan.getId());
        assertNotNull(found);
        assertEquals(plan.getGoal(), found.getGoal());
        assertEquals(plan.getUserId(), found.getUserId());
        assertEquals(plan.getTasksJson(), found.getTasksJson());
    }

    @Test
    void shouldReturnNullForNonExistingPlan() {
        TaskPlan found = planRepository.findById(999L);
        assertNull(found);
    }

    @Test
    void shouldListUserPlans() {
        createPlan(USER_A, "计划1", buildTasksJson(taskJson(1, "A1", 10)));
        createPlan(USER_A, "计划2", buildTasksJson(taskJson(1, "A2", 20)));
        createPlan(USER_B, "计划B", buildTasksJson(taskJson(1, "B1", 30)));

        List<TaskPlan> userAPlans = planRepository.findByUserId(USER_A);
        assertEquals(2, userAPlans.size());

        List<TaskPlan> userBPlans = planRepository.findByUserId(USER_B);
        assertEquals(1, userBPlans.size());
    }

    @Test
    void shouldListPlansByStatus() {
        TaskPlan plan = createPlan(USER_A, "测试", buildTasksJson(taskJson(1, "任务", 10)));
        planRepository.markConfirmed(plan.getId(), USER_A);

        List<TaskPlan> previewPlans = planRepository.findByUserIdAndStatus(USER_A, TaskPlan.STATUS_PREVIEW);
        List<TaskPlan> confirmedPlans = planRepository.findByUserIdAndStatus(USER_A, TaskPlan.STATUS_CONFIRMED);

        assertEquals(0, previewPlans.size());
        assertEquals(1, confirmedPlans.size());
    }

    // ==================== 执行计划（批量创建任务） ====================

    @Test
    void shouldExecutePlanAndCreateAllTasks() throws Exception {
        String tasksJson = buildTasksJson(
                taskJson(1, "第一步", 10),
                taskJson(2, "第二步", 30),
                taskJson(3, "第三步", 60)
        );
        TaskPlan plan = createPlan(USER_A, "三步计划", tasksJson);

        // 模拟确认和执行
        planRepository.markConfirmed(plan.getId(), USER_A);

        // 解析任务并通过 TaskCreator 创建
        JsonNode tasksArray = MAPPER.readTree(plan.getTasksJson());
        int created = 0;
        for (JsonNode taskNode : tasksArray) {
            TaskCreator.CreateResult result = taskCreator.createFromNode(USER_A, taskNode);
            if (result.isSuccess()) created++;
        }

        assertEquals(3, created, "应创建 3 个任务");

        // 验证任务已创建
        List<ScheduledTask> userTasks = taskRepository.findByUserId(USER_A);
        assertEquals(3, userTasks.size());

        // 更新计划状态为 DONE
        planRepository.markDone(plan.getId());
        TaskPlan donePlan = planRepository.findById(plan.getId());
        assertEquals(TaskPlan.STATUS_DONE, donePlan.getStatus());
    }

    @Test
    void shouldExecutePlanWithAbsoluteTimeTasks() throws Exception {
        String futureTime = LocalDateTime.now().plusHours(3).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String tasksJson = "[{\"order\":1,\"content\":\"绝对时间任务\",\"execute_time\":\"" + futureTime + "\"}]";
        TaskPlan plan = createPlan(USER_A, "绝对时间计划", tasksJson);

        JsonNode tasksArray = MAPPER.readTree(plan.getTasksJson());
        TaskCreator.CreateResult result = taskCreator.createFromNode(USER_A, tasksArray.get(0));

        assertTrue(result.isSuccess());
        assertNotNull(result.getTask().getId());
    }

    @Test
    void shouldExecutePlanWithRecurringTasks() throws Exception {
        String tasksJson = "[{\"order\":1,\"content\":\"每日提醒\",\"delay_minutes\":10,\"repeat_type\":\"DAILY\"}]";
        TaskPlan plan = createPlan(USER_A, "周期计划", tasksJson);

        JsonNode tasksArray = MAPPER.readTree(plan.getTasksJson());
        TaskCreator.CreateResult result = taskCreator.createFromNode(USER_A, tasksArray.get(0));

        assertTrue(result.isSuccess());
        assertEquals(ScheduledTask.REPEAT_TYPE_DAILY, result.getTask().getRepeatType());
        assertTrue(result.getTask().isRecurring());
    }

    // ==================== 用户隔离 ====================

    @Test
    void shouldNotConfirmOtherUsersPlan() {
        TaskPlan plan = createPlan(USER_A, "A的计划", buildTasksJson(taskJson(1, "任务", 10)));

        boolean confirmed = planRepository.markConfirmed(plan.getId(), USER_B);
        assertFalse(confirmed, "不能确认别人的计划");

        TaskPlan stillPreview = planRepository.findById(plan.getId());
        assertTrue(stillPreview.isPreview(), "状态应仍为 PREVIEW");
    }

    @Test
    void shouldNotCancelOtherUsersPlan() {
        TaskPlan plan = createPlan(USER_A, "A的计划", buildTasksJson(taskJson(1, "任务", 10)));

        boolean cancelled = planRepository.markCancelled(plan.getId(), USER_B);
        assertFalse(cancelled, "不能取消别人的计划");
    }

    // ==================== 取消计划 ====================

    @Test
    void shouldCancelPlan() {
        TaskPlan plan = createPlan(USER_A, "要取消的计划", buildTasksJson(taskJson(1, "任务", 10)));

        boolean cancelled = planRepository.markCancelled(plan.getId(), USER_A);
        assertTrue(cancelled);

        TaskPlan found = planRepository.findById(plan.getId());
        assertEquals(TaskPlan.STATUS_CANCELLED, found.getStatus());
    }

    @Test
    void shouldCancelPreviewPlan() {
        TaskPlan plan = createPlan(USER_A, "不需要了", buildTasksJson(taskJson(1, "任务", 10)));

        planRepository.markCancelled(plan.getId(), USER_A);
        TaskPlan found = planRepository.findById(plan.getId());
        assertEquals(TaskPlan.STATUS_CANCELLED, found.getStatus());
    }

    // ==================== TaskCreator 测试 ====================

    @Test
    void taskCreatorShouldCreateSingleTask() throws Exception {
        String taskJson = "{\"content\":\"测试任务\",\"delay_minutes\":15}";
        JsonNode node = MAPPER.readTree(taskJson);

        TaskCreator.CreateResult result = taskCreator.createFromNode(USER_A, node);
        assertTrue(result.isSuccess());
        assertEquals("测试任务", result.getTask().getContent());
        assertEquals(Integer.valueOf(15), result.getDelayMinutes());
        assertEquals(ScheduledTask.REPEAT_TYPE_NONE, result.getTask().getRepeatType());
    }

    @Test
    void taskCreatorShouldFailWithMissingContent() throws Exception {
        JsonNode node = MAPPER.readTree("{\"delay_minutes\":15}");
        TaskCreator.CreateResult result = taskCreator.createFromNode(USER_A, node);
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    void taskCreatorShouldFailWithMissingTimeParam() throws Exception {
        JsonNode node = MAPPER.readTree("{\"content\":\"只有内容\"}");
        TaskCreator.CreateResult result = taskCreator.createFromNode(USER_A, node);
        assertFalse(result.isSuccess());
    }

    @Test
    void taskCreatorShouldSupportDailyRepeat() throws Exception {
        String taskJson = "{\"content\":\"每天喝水\",\"delay_minutes\":30,\"repeat_type\":\"DAILY\"}";
        JsonNode node = MAPPER.readTree(taskJson);

        TaskCreator.CreateResult result = taskCreator.createFromNode(USER_A, node);
        assertTrue(result.isSuccess());
        assertEquals(ScheduledTask.REPEAT_TYPE_DAILY, result.getTask().getRepeatType());
        assertTrue(result.getTask().isRecurring());
    }

    @Test
    void taskCreatorShouldSupportWeeklyRepeat() throws Exception {
        String taskJson = "{\"content\":\"每周写周报\",\"delay_minutes\":60,\"repeat_type\":\"WEEKLY\"}";
        JsonNode node = MAPPER.readTree(taskJson);

        TaskCreator.CreateResult result = taskCreator.createFromNode(USER_A, node);
        assertTrue(result.isSuccess());
        assertEquals(ScheduledTask.REPEAT_TYPE_WEEKLY, result.getTask().getRepeatType());
    }

    // ==================== 完整流程测试 ====================

    @Test
    void completeTaskPlanningFlow() throws Exception {
        // 1. 创建计划（preview）
        String tasksJson = buildTasksJson(
                taskJson(1, "写代码", 10),
                taskJson(2, "做PPT", 60),
                taskJson(3, "准备答辩", 120)
        );
        TaskPlan plan = createPlan(USER_A, "准备比赛", tasksJson);
        assertNotNull(plan.getId());
        assertTrue(plan.isPreview());

        // 2. 确认计划
        boolean confirmed = planRepository.markConfirmed(plan.getId(), USER_A);
        assertTrue(confirmed);

        // 3. 执行计划（批量创建任务）
        JsonNode tasksArray = MAPPER.readTree(plan.getTasksJson());
        int successCount = 0;
        for (JsonNode taskNode : tasksArray) {
            if (taskCreator.createFromNode(USER_A, taskNode).isSuccess()) successCount++;
        }
        assertEquals(3, successCount);

        // 4. 标记计划完成
        planRepository.markDone(plan.getId());

        // 5. 验证最终状态
        TaskPlan donePlan = planRepository.findById(plan.getId());
        assertEquals(TaskPlan.STATUS_DONE, donePlan.getStatus());

        List<ScheduledTask> tasks = taskRepository.findByUserId(USER_A);
        assertEquals(3, tasks.size());
    }
}