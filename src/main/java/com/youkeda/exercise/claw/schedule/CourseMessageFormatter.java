package com.youkeda.exercise.claw.schedule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课表消息格式化工具
 *
 * <p>为微信聊天场景优化课程信息的文本展示格式。
 * 使用 emoji + 结构化缩进 + 分隔线，让课表信息一目了然。
 * 所有方法返回的字符串可直接用 {@code wechatClient.sendTextMessage()} 发送。
 */
public class CourseMessageFormatter {

    private static final String[] DAY_LABELS = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private static final String[][] PERIOD_TIME = {
            {},
            {"08:00", "08:45"}, {"08:50", "09:35"}, {"09:50", "10:35"}, {"10:40", "11:25"},
            {"11:30", "12:15"}, {"14:00", "14:45"}, {"14:50", "15:35"}, {"15:50", "16:35"},
            {"16:40", "17:25"}, {"17:30", "18:15"}, {"19:00", "19:45"}, {"19:50", "20:35"}
    };

    private CourseMessageFormatter() {
        // 工具类，禁止实例化
    }

    /**
     * 格式化今日课表
     *
     * @param courses     今日课程列表（已按学期周次过滤）
     * @param currentWeek 当前教学周
     * @return 格式化后的微信消息文本
     */
    public static String formatTodayCourses(List<CourseEntity> courses, int currentWeek) {
        if (courses == null || courses.isEmpty()) {
            return "🎉 今天没有课，好好休息吧！";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📚 **今日课表**");
        if (currentWeek > 0) {
            sb.append("  第").append(currentWeek).append("周");
        }
        sb.append("\n");
        sb.append("─────\n");

        // 按 startPeriod 排序
        List<CourseEntity> sorted = new ArrayList<>(courses);
        sorted.sort(Comparator.comparingInt(CourseEntity::getStartPeriod));

        int idx = 1;
        for (CourseEntity c : sorted) {
            sb.append(idx++).append(". ");
            sb.append("📖 ").append(c.getCourseName()).append("\n");
            sb.append("   🕐 ").append(formatPeriodTime(c)).append("\n");
            if (c.getClassroom() != null && !c.getClassroom().isBlank()) {
                sb.append("   🏫 ").append(c.getClassroom()).append("\n");
            }
            if (c.getTeacher() != null && !c.getTeacher().isBlank()) {
                sb.append("   👨‍🏫 ").append(c.getTeacher()).append("\n");
            }
        }

        sb.append("\n─── 💡 ───\n");
        sb.append("共 ").append(courses.size()).append(" 门课");

        return sb.toString();
    }

    /**
     * 格式化今日空闲时段
     *
     * @param freeSlots 空闲时段列表（由 CourseService.getFreeTimeSlots 返回）
     * @return 格式化后的微信消息文本
     */
    public static String formatFreeTimeSlots(List<CourseService.TimeSlot> freeSlots) {
        if (freeSlots == null || freeSlots.isEmpty()) {
            return "📅 今天全天满课，没有空闲时间 😅";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🕐 **今日空闲时间**\n");
        sb.append("─────\n");

        int idx = 1;
        for (CourseService.TimeSlot slot : freeSlots) {
            String startTime = slot.startPeriod() <= 12 ? PERIOD_TIME[slot.startPeriod()][0] : "";
            String endTime = slot.endPeriod() <= 12 ? PERIOD_TIME[slot.endPeriod()][1] : "";
            sb.append("  ").append(idx++).append(". 第").append(slot.startPeriod());
            sb.append("-").append(slot.endPeriod()).append("节");
            if (!startTime.isEmpty()) {
                sb.append("  (").append(startTime).append("-").append(endTime).append(")");
            }
            sb.append("\n");
        }

        sb.append("\n共 ").append(freeSlots.size()).append(" 个空闲时段");
        return sb.toString();
    }

    /**
     * 格式化完整课表（按星期几分组）
     *
     * @param courses     用户全部课程
     * @param currentWeek 当前教学周
     * @return 格式化后的微信消息文本
     */
    public static String formatWeekOverview(List<CourseEntity> courses, int currentWeek) {
        if (courses == null || courses.isEmpty()) {
            return "📋 还没有导入课表，快上传课表文件或告诉我课程信息吧！";
        }

        // 按 dayOfWeek 分组
        Map<Integer, List<CourseEntity>> grouped = courses.stream()
                .collect(Collectors.groupingBy(CourseEntity::getDayOfWeek));

        StringBuilder sb = new StringBuilder();
        sb.append("📋 **本周课表**");
        if (currentWeek > 0) {
            sb.append("  第").append(currentWeek).append("周");
        }
        sb.append("\n");
        sb.append("═══════════════\n");

        for (int day = 1; day <= 7; day++) {
            List<CourseEntity> dayCourses = grouped.getOrDefault(day, List.of());
            if (dayCourses.isEmpty()) continue;

            sb.append("\n**").append(DAY_LABELS[day]).append("**\n");

            // 按节次排序
            dayCourses.sort(Comparator.comparingInt(CourseEntity::getStartPeriod));

            for (CourseEntity c : dayCourses) {
                sb.append("  📖 ").append(c.getCourseName());
                sb.append("  🕐第").append(c.getPeriodDisplay()).append("节");
                if (c.getClassroom() != null && !c.getClassroom().isBlank()) {
                    sb.append(" 🏫").append(c.getClassroom());
                }
                sb.append("\n");
            }
        }

        sb.append("\n═══════════════\n");
        sb.append("共 ").append(courses.size()).append(" 门课");
        if (currentWeek > 0) {
            sb.append("  |  第").append(currentWeek).append("周");
        }

        return sb.toString();
    }

    /**
     * 格式化单门课程详情
     *
     * @param c           课程实体
     * @param currentWeek 当前教学周（小于等于 0 表示假期）
     * @return 格式化后的微信消息文本
     */
    public static String formatCourseDetail(CourseEntity c, int currentWeek) {
        StringBuilder sb = new StringBuilder();
        sb.append("📖 **").append(c.getCourseName()).append("**\n");
        sb.append("📅 ").append(c.getDayDisplay()).append("\n");
        sb.append("🕐 ").append(formatPeriodTime(c)).append("\n");
        if (c.getClassroom() != null && !c.getClassroom().isBlank()) {
            sb.append("🏫 ").append(c.getClassroom()).append("\n");
        }
        if (c.getTeacher() != null && !c.getTeacher().isBlank()) {
            sb.append("👨‍🏫 ").append(c.getTeacher()).append("\n");
        }
        sb.append("📆 ").append(c.getWeekDisplay());
        if (currentWeek > 0) {
            sb.append("  |  ").append(c.isActiveInWeek(currentWeek) ? "✅ 本周有课" : "⏸️ 本周无课");
        }
        return sb.toString();
    }

    /**
     * 格式化导入预览（包含冲突信息）
     *
     * @param courses     解析出的课程列表
     * @param conflicts   冲突检测结果（可为 null 或空）
     * @param currentWeek 当前教学周
     * @return 格式化后的预览文本
     */
    public static String formatImportPreview(List<CourseEntity> courses,
                                              List<?> conflicts,
                                              int currentWeek) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 **课表预览**\n");
        sb.append("共解析到 ").append(courses.size()).append(" 门课程\n\n");

        // 按星期几分组展示
        Map<Integer, List<CourseEntity>> grouped = courses.stream()
                .collect(Collectors.groupingBy(CourseEntity::getDayOfWeek));

        for (int day = 1; day <= 7; day++) {
            List<CourseEntity> dayCourses = grouped.getOrDefault(day, List.of());
            if (dayCourses.isEmpty()) continue;

            sb.append("**").append(DAY_LABELS[day]).append("**\n");
            for (CourseEntity c : dayCourses) {
                sb.append("  📖 ").append(c.getCourseName());
                sb.append("  🕐第").append(c.getPeriodDisplay()).append("节");
                if (c.getClassroom() != null && !c.getClassroom().isBlank()) {
                    sb.append(" 🏫").append(c.getClassroom());
                }
                sb.append("\n");
            }
        }

        if (conflicts != null && !conflicts.isEmpty()) {
            sb.append("\n⚠️ **时间冲突告警**\n");
            for (Object cf : conflicts) {
                if (cf instanceof CourseService.ConflictInfo ci) {
                    sb.append("  ❌ ").append(ci.description()).append("\n");
                }
            }
            sb.append("\n确认后冲突课程将被覆盖。");
        }

        sb.append("\n\n请确认是否导入以上课程？✅ 确认 / ❌ 取消");
        return sb.toString();
    }

    // ==================== 内部工具方法 ====================

    /**
     * 格式化课程的时间段显示
     * 示例：第3-4节 (09:50-11:25)
     */
    private static String formatPeriodTime(CourseEntity c) {
        int s = c.getStartPeriod();
        int e = c.getEndPeriod();
        String periodLabel = "第" + c.getPeriodDisplay() + "节";
        if (s < 1 || s > 12) return periodLabel;
        String start = PERIOD_TIME[s][0];
        String end = (e >= 1 && e <= 12) ? PERIOD_TIME[e][1] : "";
        return periodLabel + " (" + start + "-" + end + ")";
    }
}