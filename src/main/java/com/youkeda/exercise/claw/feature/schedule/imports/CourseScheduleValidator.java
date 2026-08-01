package com.youkeda.exercise.claw.feature.schedule.imports;

import com.youkeda.exercise.claw.feature.schedule.CourseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 课程解析校验器
 *
 * <p>对解析出的原始课程列表做三步规范化，供预览/入库前使用：
 * <ol>
 *   <li><b>规范化</b>：剔除空课程、修正节次/星期越界、单双周二次检测、实践课识别</li>
 *   <li><b>相邻节次自动合并</b>：同一课程相邻节次合并为一段（如周一第1节 + 周一第2节 → 1-2节）</li>
 *   <li><b>组内冲突检测</b>：同一时间出现多门课 → 告警（不阻断导入）</li>
 * </ol>
 *
 * <p>实践课程（无 weekday/period）不参与合并与冲突计算，原样保留。
 */
@Component
public class CourseScheduleValidator {

    private static final Logger log = LoggerFactory.getLogger(CourseScheduleValidator.class);

    /** 节次上限（一天最多 12 节） */
    private static final int MAX_PERIODS = 12;

    /** 校验结果 */
    public record ValidationResult(
            List<CourseEntity> courses,
            List<String> warnings
    ) {
        public boolean hasConflict() {
            return warnings.stream().anyMatch(w -> w.contains("时间冲突"));
        }
    }

    /**
     * 校验并规范化课程列表
     *
     * @param rawCourses 解析出的原始课程列表（可为 null）
     * @return 规范化后的课程列表 + 告警信息
     */
    public ValidationResult validate(List<CourseEntity> rawCourses) {
        List<String> warnings = new ArrayList<>();
        if (rawCourses == null || rawCourses.isEmpty()) {
            return new ValidationResult(List.of(), warnings);
        }

        // 1. 逐条规范化
        List<CourseEntity> normalized = new ArrayList<>();
        for (CourseEntity c : rawCourses) {
            CourseEntity n = normalize(c, warnings);
            if (n != null) {
                normalized.add(n);
            }
        }
        if (normalized.isEmpty()) {
            return new ValidationResult(List.of(), warnings);
        }

        // 2. 相邻节次自动合并
        List<CourseEntity> merged = mergeAdjacentPeriods(normalized);

        // 3. 组内冲突检测（合并后）
        detectInternalConflicts(merged, warnings);

        log.info("课程校验完成 | raw={} normalized={} merged={} warnings={}",
                rawCourses.size(), normalized.size(), merged.size(), warnings.size());
        return new ValidationResult(merged, warnings);
    }

    // ==================== 规范化 ====================

    private CourseEntity normalize(CourseEntity c, List<String> warnings) {
        if (c == null || c.getCourseName() == null || c.getCourseName().isBlank()) {
            return null;
        }

        CourseEntity n = new CourseEntity();
        n.setCourseName(c.getCourseName().trim());
        n.setUserId(c.getUserId());
        n.setTeacher(c.getTeacher() != null ? c.getTeacher() : "");
        n.setClassroom(c.getClassroom() != null ? c.getClassroom() : "");
        n.setStartWeek(Math.max(1, c.getStartWeek()));
        n.setEndWeek(Math.max(n.getStartWeek(), c.getEndWeek()));
        n.setWeekType(c.getWeekType() != null ? c.getWeekType() : CourseEntity.WEEK_ALL);
        n.setWeekPattern(c.getWeekPattern());
        n.setSemesterId(c.getSemesterId());
        n.setSource(c.getSource() != null ? c.getSource() : CourseEntity.SOURCE_MANUAL);

        // 单双周二次检测：weekType=ALL 但课程文本含明确单双周标记 → 覆盖
        // 注意用严格标记（(单)/单周/(双)/双周），避免误判「单片机」「双学位」等含字课程
        if (CourseEntity.WEEK_ALL.equals(n.getWeekType())) {
            String detected = detectParityMarker(n.getCourseName() + n.getClassroom() + n.getTeacher());
            if (detected != null) {
                n.setWeekType(detected);
            }
        }

        Integer day = c.getDayOfWeek();
        Integer start = c.getStartPeriod();
        Integer end = c.getEndPeriod();

        // 实践课 / 缺少时间信息 → 保持无固定时间
        if (day == null || start == null) {
            if (day != null || start != null) {
                warnings.add("「" + n.getCourseName() + "」缺少星期或节次信息，已按实践课（无固定时间）处理");
            }
            n.setDayOfWeek(null);
            n.setStartPeriod(null);
            n.setEndPeriod(null);
            return n;
        }

        // 常规课程：节次/星期越界修正
        if (day < 1 || day > 7) {
            n.setDayOfWeek(1);
            warnings.add("「" + n.getCourseName() + "」星期值异常(" + day + ")，已修正为周一");
        } else {
            n.setDayOfWeek(day);
        }
        if (start < 1) {
            start = 1;
        }
        if (end == null) {
            end = start;
        }
        if (end < start) {
            end = start;
        }
        if (start > MAX_PERIODS) {
            warnings.add("「" + n.getCourseName() + "」节次超出范围(" + start + ")，已修正");
            start = MAX_PERIODS;
        }
        if (end > MAX_PERIODS) {
            end = MAX_PERIODS;
        }
        n.setStartPeriod(start);
        n.setEndPeriod(end);

        return n;
    }

    /** 从文本中检测明确的单双周标记（严格匹配，避免误判含「单/双」字的课程名） */
    private String detectParityMarker(String text) {
        if (text == null) {
            return null;
        }
        if (text.contains("(单)") || text.contains("（单）") || text.contains("单周")) {
            return CourseEntity.WEEK_ODD;
        }
        if (text.contains("(双)") || text.contains("（双）") || text.contains("双周")) {
            return CourseEntity.WEEK_EVEN;
        }
        return null;
    }

    // ==================== 相邻节次合并 ====================

    /**
     * 合并同一课程在相邻节次的记录
     * <p>按 (课程名, 教师, 教室, 星期, 周次, 单双周, 周次模式) 分组，
     * 组内按开始节次排序，后一节紧接着前一节（end+1 == start）时合并为一段。</p>
     */
    private List<CourseEntity> mergeAdjacentPeriods(List<CourseEntity> courses) {
        Map<String, List<CourseEntity>> groups = new LinkedHashMap<>();
        for (CourseEntity c : courses) {
            if (c.isPractice()) {
                groups.computeIfAbsent("practice:" + c.getCourseName(), k -> new ArrayList<>()).add(c);
            } else {
                groups.computeIfAbsent(mergeKey(c), k -> new ArrayList<>()).add(c);
            }
        }

        List<CourseEntity> result = new ArrayList<>();
        for (List<CourseEntity> group : groups.values()) {
            if (group.get(0).isPractice()) {
                // 实践课无节次，不做相邻节次合并
                result.addAll(group);
            } else {
                result.addAll(mergeGroup(group));
            }
        }
        return result;
    }

    private String mergeKey(CourseEntity c) {
        return c.getCourseName() + "|" + c.getTeacher() + "|" + c.getClassroom()
                + "|" + c.getDayOfWeek() + "|" + c.getStartWeek() + "|" + c.getEndWeek()
                + "|" + c.getWeekType() + "|" + (c.getWeekPattern() != null ? c.getWeekPattern() : "");
    }

    private List<CourseEntity> mergeGroup(List<CourseEntity> group) {
        if (group.size() <= 1) {
            return group;
        }

        List<CourseEntity> sorted = new ArrayList<>(group);
        sorted.sort(Comparator.comparingInt(c -> c.getStartPeriod()));

        List<CourseEntity> merged = new ArrayList<>();
        CourseEntity current = null;
        for (CourseEntity next : sorted) {
            if (current == null) {
                current = copyOf(next);
                continue;
            }
            // 相邻（end + 1 == start）→ 合并
            if (current.getEndPeriod() + 1 == next.getStartPeriod()) {
                if (current.getClassroom().isBlank() && !next.getClassroom().isBlank()) {
                    current.setClassroom(next.getClassroom());
                }
                current.setEndPeriod(next.getEndPeriod());
            } else {
                merged.add(current);
                current = copyOf(next);
            }
        }
        if (current != null) {
            merged.add(current);
        }
        return merged;
    }

    private CourseEntity copyOf(CourseEntity c) {
        CourseEntity n = new CourseEntity();
        n.setId(c.getId());
        n.setUserId(c.getUserId());
        n.setCourseName(c.getCourseName());
        n.setTeacher(c.getTeacher());
        n.setDayOfWeek(c.getDayOfWeek());
        n.setStartPeriod(c.getStartPeriod());
        n.setEndPeriod(c.getEndPeriod());
        n.setClassroom(c.getClassroom());
        n.setStartWeek(c.getStartWeek());
        n.setEndWeek(c.getEndWeek());
        n.setWeekType(c.getWeekType());
        n.setWeekPattern(c.getWeekPattern());
        n.setSemesterId(c.getSemesterId());
        n.setSource(c.getSource());
        return n;
    }

    // ==================== 组内冲突检测 ====================

    private void detectInternalConflicts(List<CourseEntity> courses, List<String> warnings) {
        List<CourseEntity> nonPractice = courses.stream()
                .filter(c -> !c.isPractice())
                .toList();
        for (int i = 0; i < nonPractice.size(); i++) {
            for (int j = i + 1; j < nonPractice.size(); j++) {
                CourseEntity a = nonPractice.get(i);
                CourseEntity b = nonPractice.get(j);
                if (hasTimeConflict(a, b)) {
                    warnings.add("「" + a.getCourseName() + "」与「" + b.getCourseName()
                            + "」在" + a.getDayDisplay() + " " + a.getPeriodDisplay()
                            + "节时间冲突（" + a.getWeekDisplay() + " / " + b.getWeekDisplay() + "）");
                }
            }
        }
    }

    /**
     * 判断两门课是否存在时间冲突（同天、节次重叠、活跃周有交集）
     */
    private boolean hasTimeConflict(CourseEntity a, CourseEntity b) {
        if (a.isPractice() || b.isPractice()) {
            return false;
        }
        if (!a.getDayOfWeek().equals(b.getDayOfWeek())) {
            return false;
        }
        boolean periodOverlap = a.getStartPeriod() <= b.getEndPeriod()
                && b.getStartPeriod() <= a.getEndPeriod();
        if (!periodOverlap) {
            return false;
        }
        int maxStart = Math.max(a.getStartWeek(), b.getStartWeek());
        int minEnd = Math.min(a.getEndWeek(), b.getEndWeek());
        if (maxStart > minEnd) {
            return false;
        }
        for (int week = maxStart; week <= minEnd; week++) {
            if (a.isActiveInWeek(week) && b.isActiveInWeek(week)) {
                return true;
            }
        }
        return false;
    }
}
