package com.youkeda.exercise.claw.schedule;

import com.youkeda.exercise.claw.wechat.client.WechatILinkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 课前提醒服务
 *
 * <p>无独立调度线程，依赖外部（{@link com.youkeda.exercise.claw.task.scheduler.TaskSchedulerService}）
 * 周期性调用 {@link #checkReminders()} 来扫描课程并发送提醒。
 *
 * <p>扫描逻辑：
 * <ul>
 *   <li>每 60 秒扫描一次所有用户的课程（由 {@code TaskSchedulerService} 触发）</li>
 *   <li>课程开始前 30 分钟（±30 秒容差）发送微信提醒</li>
 *   <li>自动判断当前教学周和单双周</li>
 * </ul>
 *
 * <p>提醒去重：使用内存缓存 {@link #notifiedCache} 避免同一天同一门课重复发送。
 * 缓存最多保留 10000 条，超限时全量清理。
 */
@Component
public class ScheduleReminderService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleReminderService.class);

    /** 提醒提前量：30 分钟 */
    private static final int REMINDER_ADVANCE_MINUTES = 30;

    /** 提醒窗口容差（秒）：允许提前或滞后 30 秒触发 */
    private static final int REMINDER_TOLERANCE_SECONDS = 30;

    // 典型节次时间表（第几节 ~ 开始时间）
    private static final int[] PERIOD_START_HOUR = {
            0, 8, 8, 9, 10, 11, 14, 14, 15, 15, 16, 19, 19
    };
    private static final int[] PERIOD_START_MINUTE = {
            0, 0, 50, 50, 40, 30, 0, 50, 50, 40, 30, 0, 45
    };

    private final CourseRepository courseRepository;
    private final SemesterConfig semesterConfig;
    private final WechatILinkClient wechatClient;

    /** 已发送提醒的课程 ID 缓存（避免重复发送），key = userId:courseId:yyyyMMdd */
    private final ConcurrentHashMap<String, Boolean> notifiedCache = new ConcurrentHashMap<>();

    public ScheduleReminderService(CourseRepository courseRepository,
                                   SemesterConfig semesterConfig,
                                   WechatILinkClient wechatClient) {
        this.courseRepository = courseRepository;
        this.semesterConfig = semesterConfig;
        this.wechatClient = wechatClient;
    }

    /**
     * 扫描全部课程，检查是否需要发送提醒。
     *
     * <p>由 {@code TaskSchedulerService} 定期调用（约每 60 秒一次）。
     * 多次调用安全：内部使用去重缓存防止重复发送。
     */
    public void checkReminders() {
        try {
            int currentWeek = semesterConfig.getCurrentWeek();
            if (currentWeek <= 0) {
                return; // 学期未开始
            }

            List<CourseEntity> allCourseEntitys = courseRepository.findAll();
            if (allCourseEntitys.isEmpty()) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            int today = semesterConfig.getCurrentDayOfWeek();

            int sentCount = 0;
            for (CourseEntity course : allCourseEntitys) {
                try {
                    if (checkCourseEntity(course, currentWeek, today, now)) {
                        sentCount++;
                    }
                } catch (Exception e) {
                    log.warn("检查课程提醒异常 | courseId={} | error={}", course.getId(), e.getMessage());
                }
            }

            if (sentCount > 0) {
                log.info("课前提醒扫描完成 | sent={} | totalCourseEntitys={}", sentCount, allCourseEntitys.size());
            }

            // 清理缓存（避免内存泄漏）
            cleanCacheIfNeeded();

        } catch (Exception e) {
            log.error("课前提醒扫描异常", e);
        }
    }

    /**
     * 检查单条课程是否需要发送提醒
     *
     * @return true 表示已发送提醒
     */
    private boolean checkCourseEntity(CourseEntity course, int currentWeek, int today, LocalDateTime now) {
        // 1. 不是今天的课 → 跳过
        if (course.getDayOfWeek() != today) return false;

        // 2. 本周不上课 → 跳过
        if (!course.isActiveInWeek(currentWeek)) return false;

        // 3. 计算课程开始时间
        LocalDateTime courseStartTime = getCourseEntityStartTime(course);
        if (courseStartTime == null) return false;

        // 4. 计算提醒时间窗口
        LocalDateTime reminderTarget = courseStartTime.minusMinutes(REMINDER_ADVANCE_MINUTES);
        long diffSeconds = Duration.between(now, reminderTarget).abs().getSeconds();

        if (diffSeconds > REMINDER_TOLERANCE_SECONDS) return false;

        // 5. 检查是否已发送过（同一天同一门课不重复）
        String cacheKey = course.getUserId() + ":" + course.getId() + ":"
                + now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        if (notifiedCache.putIfAbsent(cacheKey, Boolean.TRUE) != null) return false;

        // 6. 发送提醒
        sendReminder(course, courseStartTime, currentWeek);
        return true;
    }

    /**
     * 发送课前提醒微信消息
     */
    private void sendReminder(CourseEntity course, LocalDateTime courseStartTime, int currentWeek) {
        String timeStr = courseStartTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        String message = buildReminderMessage(course, timeStr, currentWeek);

        try {
            wechatClient.sendTextMessage(course.getUserId(), message);
            log.info("课前提醒已发送 | userId={} | course={} | time={}",
                    course.getUserId(), course.getCourseName(), timeStr);
        } catch (Exception e) {
            log.error("课前提醒发送失败 | userId={} | course={}",
                    course.getUserId(), course.getCourseName(), e);
        }
    }

    /**
     * 构建提醒消息文本
     */
    private String buildReminderMessage(CourseEntity course, String timeStr, int currentWeek) {
        StringBuilder sb = new StringBuilder();
        sb.append("⏰ 课前提醒\n\n");
        sb.append("📚 ").append(course.getCourseName()).append("\n");
        if (!course.getClassroom().isBlank()) {
            sb.append("📍 ").append(course.getClassroom()).append("\n");
        }
        if (!course.getTeacher().isBlank()) {
            sb.append("👨‍🏫 ").append(course.getTeacher()).append("\n");
        }
        sb.append("⏱ ").append(timeStr).append("  (").append(course.getPeriodDisplay()).append("节)\n");
        sb.append("📅 第 ").append(currentWeek).append(" 周");
        if (!CourseEntity.WEEK_ALL.equals(course.getWeekType())) {
            sb.append("（").append(CourseEntity.WEEK_ODD.equals(course.getWeekType()) ? "单周" : "双周").append("）");
        }
        return sb.toString();
    }

    /**
     * 根据课程节次计算今日开始时间
     */
    private LocalDateTime getCourseEntityStartTime(CourseEntity course) {
        int period = course.getStartPeriod();
        if (period < 1 || period > 12) return null;
        LocalDateTime now = LocalDateTime.now();
        return now.withHour(PERIOD_START_HOUR[period])
                  .withMinute(PERIOD_START_MINUTE[period])
                  .withSecond(0).withNano(0);
    }

    /**
     * 清理去重缓存，避免内存泄漏
     */
    private void cleanCacheIfNeeded() {
        if (notifiedCache.size() > 10000) {
            notifiedCache.clear();
            log.info("课前提醒缓存已清理");
        }
    }
}