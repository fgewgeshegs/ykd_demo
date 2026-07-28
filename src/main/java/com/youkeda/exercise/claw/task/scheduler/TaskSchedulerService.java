package com.youkeda.exercise.claw.task.scheduler;

import com.youkeda.exercise.claw.task.executor.AgentTaskExecutor;
import com.youkeda.exercise.claw.task.model.ScheduledTask;
import com.youkeda.exercise.claw.task.repository.ScheduledTaskRepository;
import com.youkeda.exercise.claw.task.service.RepeatCalculator;
import com.youkeda.exercise.claw.wechat.client.WechatILinkClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 定时任务调度器
 *
 * <p>后台守护线程，定期扫描到期任务并通过微信推送提醒。
 * 支持一次性任务和周期任务。
 *
 * <p>执行策略：
 * <ul>
 *   <li>一次性任务（ONCE）：执行后标记 DONE</li>
 *   <li>周期任务（DAILY/WEEKLY）：执行后通过 {@link RepeatCalculator} 计算下次时间，
 *       更新 next_execute_time，保持 ACTIVE 状态</li>
 * </ul>
 */
@Component
public class TaskSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(TaskSchedulerService.class);

    private static final int DEFAULT_INTERVAL_SECONDS = 5;
    private static final int INITIAL_DELAY_SECONDS = 10;

    private final ScheduledTaskRepository taskRepository;
    private final WechatILinkClient wechatClient;
    private final RepeatCalculator repeatCalculator;
    private final AgentTaskExecutor agentTaskExecutor;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;

    public TaskSchedulerService(ScheduledTaskRepository taskRepository,
                                WechatILinkClient wechatClient,
                                RepeatCalculator repeatCalculator,
                                AgentTaskExecutor agentTaskExecutor) {
        this.taskRepository = taskRepository;
        this.wechatClient = wechatClient;
        this.repeatCalculator = repeatCalculator;
        this.agentTaskExecutor = agentTaskExecutor;
    }

    @PostConstruct
    public void start() {
        log.info("定时任务调度器启动中...");
        running.set(true);
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "task-scheduler-thread");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleWithFixedDelay(
                this::checkAndExecute,
                INITIAL_DELAY_SECONDS,
                DEFAULT_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );

        log.info("定时任务调度器已启动 | interval={}s | initialDelay={}s",
                DEFAULT_INTERVAL_SECONDS, INITIAL_DELAY_SECONDS);
    }

    private void checkAndExecute() {
        if (!running.get()) return;

        try {
            List<ScheduledTask> dueTasks = taskRepository.findPendingAndDue();
            if (dueTasks.isEmpty()) return;

            log.info("定时任务调度器：发现 {} 个到期任务", dueTasks.size());

            for (ScheduledTask task : dueTasks) {
                executeTask(task);
            }
        } catch (Exception e) {
            log.error("定时任务调度器扫描异常", e);
        }
    }

    /**
     * 执行单个定时任务
     *
     * <p>根据任务类型分流：
     * <ul>
     *   <li>{@link ScheduledTask#TASK_TYPE_REMINDER} — 发送提醒文字</li>
     *   <li>{@link ScheduledTask#TASK_TYPE_AGENT} — 调用 AgentTaskExecutor 执行</li>
     * </ul>
     */
    private void executeTask(ScheduledTask task) {
        LocalDateTime now = LocalDateTime.now();

        log.info("执行定时任务 | id={} | userId={} | content={} | repeat={} | taskType={}",
                task.getId(), task.getUserId(), task.getContent(),
                task.getRepeatType(), task.getTaskType());

        boolean executionSuccess = false;
        try {
            if (task.isAgentTask()) {
                // Agent 任务：委托 AgentTaskExecutor
                executeAgentTask(task);
            } else {
                // 普通提醒：发送文字消息
                executeReminderTask(task);
            }
            executionSuccess = true;
        } catch (Exception e) {
            log.error("定时任务执行失败 | id={} | taskType={} | error={}",
                    task.getId(), task.getTaskType(), e.getMessage(), e);
        }

        // === 执行后处理：周期任务 vs 一次性任务 ===
        handlePostExecution(task, now, executionSuccess);
    }

    /**
     * 执行 Agent 任务
     */
    private void executeAgentTask(ScheduledTask task) throws Exception {
        agentTaskExecutor.execute(task);
    }

    /**
     * 执行普通提醒任务
     */
    private void executeReminderTask(ScheduledTask task) {
        String message = buildReminderMessage(task);
        wechatClient.sendTextMessage(task.getUserId(), message);
    }

    /**
     * 任务执行后处理：周期任务更新下次执行时间，一次性任务标记完成。
     *
     * <p>设计原则：
     * <ul>
     *   <li>执行成功的一次性任务 → DONE</li>
     *   <li>执行成功的周期任务 → 计算 next_execute_time，保持 ACTIVE</li>
     *   <li>执行失败的周期任务 → 标记 FAILED，但仍计算下次执行时间，保持 ACTIVE</li>
     *   <li>执行失败的一次性任务 → 标记 FAILED</li>
     * </ul>
     */
    private void handlePostExecution(ScheduledTask task, LocalDateTime now, boolean success) {
        boolean isRecurring = repeatCalculator.hasNext(task);

        if (!success) {
            // 标记失败
            taskRepository.markFailed(task.getId(),
                    isRecurring ? "周期任务执行失败，下次重试" : "任务执行失败");
            log.warn("定时任务已标记 FAILED | id={} | isRecurring={}", task.getId(), isRecurring);

            // 周期任务失败后仍计算下次执行时间
            if (isRecurring) {
                calculateNextTime(task, now);
            }
            return;
        }

        // === 执行成功 ===
        if (isRecurring) {
            // 周期任务：计算下次执行时间，保持 ACTIVE
            calculateNextTime(task, now);
        } else {
            // 一次性任务：标记 DONE
            taskRepository.markDone(task.getId());
            log.info("一次性任务执行完成 | id={}", task.getId());
        }
    }

    /**
     * 计算周期任务的下次执行时间并更新。
     */
    private void calculateNextTime(ScheduledTask task, LocalDateTime now) {
        try {
            LocalDateTime nextTime = repeatCalculator.calculateNext(task, now);
            if (nextTime != null) {
                taskRepository.updateNextExecuteTime(task.getId(), nextTime);
                log.info("周期任务下次执行 | id={} | nextExecuteTime={}", task.getId(), nextTime);
            } else {
                taskRepository.markDone(task.getId());
                log.warn("周期任务计算下次时间失败，已标记 DONE | id={}", task.getId());
            }
        } catch (Exception e) {
            log.error("计算下次执行时间异常 | id={}", task.getId(), e);
            taskRepository.markDone(task.getId());
        }
    }

    private String buildReminderMessage(ScheduledTask task) {
        String repeatTag = task.isRecurring() ? " 🔁" : "";
        return "⏰ 提醒：" + task.getContent() + repeatTag;
    }

    @PreDestroy
    public void stop() {
        log.info("定时任务调度器正在关闭...");
        running.set(false);
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("定时任务调度器已关闭");
    }

    public int getPendingCount() {
        return taskRepository.findPendingAndDue().size();
    }
}