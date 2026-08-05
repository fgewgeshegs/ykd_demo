package com.youkeda.exercise.claw.feature.schedule;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课表消息格式化工具
 *
 * <p>为微信聊天场景优化课程信息的文本展示格式。
 * 使用 emoji + 结构化缩进 + 分隔线，让课表信息一目了然。
 * 所有方法返回的字符串可直接用 {@code wechatClient.sendTextMessage()} 发送。
 *
 * <p>各方法需要通过 {@link ScheduleTimeResolver} 将节次号解析为具体时间，
 * 以支持不同学校的作息配置差异。
 */
@Component
public class CourseMessageFormatter {

    private static final String[] DAY_LABELS = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private final ScheduleTimeResolver timeResolver;

    public CourseMessageFormatter(ScheduleTimeResolver timeResolver) {
        this.timeResolver = timeResolver;
    }

    /**
     * 格式化今日课表
     *
     * @param courses     今日课程列表（已按学期周次过滤）
     * @param currentWeek 当前教学周
     * @return 格式化后的微信消息文本
     */
    public String formatTodayCourses(List<CourseEntity> courses, int currentWeek) {
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
    public String formatFreeTimeSlots(String userId, List<CourseService.TimeSlot> freeSlots) {
        if (freeSlots == null || freeSlots.isEmpty()) {
            return "📅 今天全天满课，没有空闲时间 😅";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🕐 **今日空闲时间**\n");
        sb.append("─────\n");

        int idx = 1;
        for (CourseService.TimeSlot slot : freeSlots) {
            sb.append("  ").append(idx++).append(". ").append(slot.display());
            // 追加时间信息
            String startTime = safeGetTime(userId, slot.startPeriod(), true);
            String endTime = safeGetTime(userId, slot.endPeriod(), false);
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
    public String formatWeekOverview(List<CourseEntity> courses, int currentWeek) {
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
    public String formatCourseDetail(CourseEntity c, int currentWeek) {
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
     * 课表导入预览（兼容签名，委托 {@link #formatPendingImportPreview} 生成带组内编号的预览）。
     *
     * @param courses     待预览课程列表
     * @param conflicts   冲突检测结果（元素可为 {@link CourseService.ConflictInfo} 或描述字符串，可为 null/空）
     * @param currentWeek 当前教学周
     * @return 格式化后的预览文本
     */
    public String formatImportPreview(List<CourseEntity> courses,
                                       List<?> conflicts,
                                       int currentWeek) {
        // 委托带编号版：保证任何调用路径的预览均带组内编号，与 modify_pending 的 course_index 对应
        return formatPendingImportPreview(courses, "", currentWeek, conflicts);
    }

    /**
     * 课表导入预览（按星期分组 + 组内编号 + 上下文信息）。
     *
     * <p>编号为该星期下的第几个（1-based），与 modify_pending 的 course_index 对应。
     * 学期信息、当前周次、冲突告警统一由本方法生成，调用方只负责组装参数，
     * 保证用户展示与 Agent 上下文看到的预览完全一致。
     * 供 CourseImportHandler（用户展示 + 写上下文）和 CourseImportFlowActions（modify 后展示）复用。
     *
     * @param courses     待确认课程列表
     * @param semesterInfo 学期信息文本（形如 "【2026秋】\n第1周：2026-09-07\n\n"，可为空字符串）
     * @param currentWeek  当前教学周（&lt;=0 表示无，不显示）
     * @param conflicts    冲突列表（元素可为 {@link CourseService.ConflictInfo} 或描述字符串，可为 null/空）；
     *                     含 ConflictInfo 时按「时间冲突告警 + 确认后覆盖」展示，全为字符串时按「识别错误」展示
     */
    public String formatPendingImportPreview(List<CourseEntity> courses,
                                             String semesterInfo,
                                             int currentWeek,
                                             List<?> conflicts) {
        StringBuilder sb = new StringBuilder();
        sb.append("📋 已识别出以下 ").append(courses.size()).append(" 门课程");
        if (currentWeek > 0) {
            sb.append("（当前第 ").append(currentWeek).append(" 周）");
        }
        sb.append("：\n\n");

        if (semesterInfo != null && !semesterInfo.isBlank()) {
            sb.append(semesterInfo);
        }

        Map<Integer, List<CourseEntity>> byDay = new LinkedHashMap<>();
        for (CourseEntity c : courses) {
            byDay.computeIfAbsent(c.getDayOfWeek(), k -> new ArrayList<>()).add(c);
        }
        for (int day = 1; day <= 7; day++) {
            List<CourseEntity> dayCourses = byDay.getOrDefault(day, List.of());
            if (dayCourses.isEmpty()) continue;
            sb.append("**").append(dayCourses.get(0).getDayDisplay()).append("**\n");
            int idx = 1;
            for (CourseEntity c : dayCourses) {
                sb.append("  ").append(idx++).append(". ");
                sb.append("【").append(c.getCourseName()).append("】");
                sb.append(" 第").append(c.getPeriodDisplay()).append("节");
                if (!c.getClassroom().isBlank()) sb.append(" ").append(c.getClassroom());
                if (!c.getTeacher().isBlank()) sb.append(" ").append(c.getTeacher());
                sb.append(" (").append(c.getWeekDisplay()).append(")");
                sb.append("\n");
            }
        }

        if (conflicts != null && !conflicts.isEmpty()) {
            boolean hasDbConflict = false;
            for (Object c : conflicts) {
                if (c instanceof CourseService.ConflictInfo) {
                    hasDbConflict = true;
                    break;
                }
            }
            if (hasDbConflict) {
                sb.append("\n⚠️ **时间冲突告警**\n");
            } else {
                sb.append("\n⚠️ 检测到 ").append(conflicts.size()).append(" 处时间冲突，可能是识别错误：\n");
            }
            for (Object conflict : conflicts) {
                String desc = conflict instanceof CourseService.ConflictInfo ci
                        ? ci.description()
                        : String.valueOf(conflict);
                sb.append("   · ").append(desc).append("\n");
            }
            if (hasDbConflict) {
                sb.append("确认后冲突课程将被覆盖。\n");
            }
        }

        sb.append("\n✅ 回复「确认」保存课表，回复「取消」丢弃。");
        sb.append("\n💡 如需修改某门课，请告诉我「星期 + 课程名」或「星期 + 第几个」，例如「周一的英语改成单周」。");
        return sb.toString();
    }

    // ==================== 内部工具方法 ====================

    /**
     * 格式化课程的时间段显示
     * 示例：第3-4节 (09:50-11:25)
     */
    public String formatPeriodTime(CourseEntity c) {
        String periodLabel = "第" + c.getPeriodDisplay() + "节";
        String timeRange = timeResolver.formatTimeRange(
                c.getUserId(), c.getStartPeriod(), c.getEndPeriod());
        if (timeRange.isEmpty()) return periodLabel;
        return periodLabel + " (" + timeRange + ")";
    }

    /**
     * 安全获取某节次的开始或结束时间文本
     */
    private String safeGetTime(String userId, int period, boolean isStart) {
        java.time.LocalTime t = isStart
                ? timeResolver.getStartTime(userId, period)
                : timeResolver.getEndTime(userId, period);
        return t != null ? t.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) : "";
    }
}
