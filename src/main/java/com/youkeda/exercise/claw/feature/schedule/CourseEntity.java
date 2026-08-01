package com.youkeda.exercise.claw.feature.schedule;

/**
 * 课程实体
 *
 * <p>表示课表中的一条课程记录，支持周次、单双周过滤与复杂周次模式。
 * 持久化到 SQLite {@code course_schedule} 表，以 {@code userId} 作为隔离键。
 *
 * <p>实践课程（如实训、无固定时间）的 {@code dayOfWeek}/{@code startPeriod}/{@code endPeriod}
 * 为 null，通过 {@link #isPractice()} 判断，不参与提醒与时间冲突计算。
 */
public class CourseEntity {

    /** 双周类型：全部周 */
    public static final String WEEK_ALL = "ALL";
    /** 双周类型：单周 */
    public static final String WEEK_ODD = "ODD";
    /** 双周类型：双周 */
    public static final String WEEK_EVEN = "EVEN";

    /** 数据来源：手动/对话录入（默认） */
    public static final String SOURCE_MANUAL = "MANUAL";

    private Long id;
    private String userId;
    private String courseName;
    private String teacher;
    /** 星期几：1=周一 ~ 7=周日；实践课程为 null */
    private Integer dayOfWeek;
    /** 开始节次（1-based）；实践课程为 null */
    private Integer startPeriod;
    /** 结束节次（1-based，含）；实践课程为 null */
    private Integer endPeriod;
    private String classroom;
    private int startWeek;
    private int endWeek;
    /** 单双周：ALL / ODD / EVEN */
    private String weekType;
    /** 复杂周次模式（可为 null）：如 "1,3,5" / "1-8,11-17"。存在时 isActiveInWeek 优先按此精确匹配 */
    private String weekPattern;
    /** 所属学期 ID（nullable，兼容历史数据） */
    private Long semesterId;
    /** 数据来源：ZHENGFANG / EXCEL / PDF / OCR / DOC / MANUAL */
    private String source;

    public CourseEntity() {
    }

    public CourseEntity(String userId, String courseName, String teacher,
                        int dayOfWeek, int startPeriod, int endPeriod,
                        String classroom, int startWeek, int endWeek, String weekType) {
        this.userId = userId;
        this.courseName = courseName;
        this.teacher = teacher != null ? teacher : "";
        this.dayOfWeek = dayOfWeek;
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
        this.classroom = classroom != null ? classroom : "";
        this.startWeek = startWeek;
        this.endWeek = endWeek;
        this.weekType = weekType != null ? weekType : WEEK_ALL;
        this.source = SOURCE_MANUAL;
    }

    /**
     * 支持实践课程的创建工厂：dayOfWeek / startPeriod / endPeriod 可传 null（无固定时间）
     *
     * @see #isPractice()
     */
    public static CourseEntity create(String userId, String courseName, String teacher,
                                      Integer dayOfWeek, Integer startPeriod, Integer endPeriod,
                                      String classroom, int startWeek, int endWeek, String weekType) {
        CourseEntity c = new CourseEntity();
        c.userId = userId;
        c.courseName = courseName;
        c.teacher = teacher != null ? teacher : "";
        c.dayOfWeek = dayOfWeek;
        c.startPeriod = startPeriod;
        c.endPeriod = endPeriod;
        c.classroom = classroom != null ? classroom : "";
        c.startWeek = startWeek;
        c.endWeek = endWeek;
        c.weekType = weekType != null ? weekType : WEEK_ALL;
        c.source = SOURCE_MANUAL;
        return c;
    }

    /**
     * 判断是否为实践课程（无固定时间：缺少 weekday 或节次）
     */
    public boolean isPractice() {
        return dayOfWeek == null || startPeriod == null || endPeriod == null;
    }

    /**
     * 判断课程在当前周是否上课
     *
     * @param currentWeek 当前教学周
     * @return true 表示本周有课
     */
    public boolean isActiveInWeek(int currentWeek) {
        if (currentWeek < startWeek || currentWeek > endWeek) {
            return false;
        }
        boolean weekTypeOk = switch (weekType) {
            case WEEK_ODD -> currentWeek % 2 == 1;
            case WEEK_EVEN -> currentWeek % 2 == 0;
            default -> true;
        };
        if (!weekTypeOk) {
            return false;
        }
        if (weekPattern != null && !weekPattern.isBlank()) {
            return matchesWeekPattern(currentWeek, weekPattern);
        }
        return true;
    }

    /**
     * 按周次模式精确匹配，如 "1,3,5" / "1-8,11-17"
     */
    private static boolean matchesWeekPattern(int week, String pattern) {
        for (String part : pattern.split(",")) {
            part = part.trim();
            if (part.isEmpty()) continue;
            int dash = part.indexOf('-');
            if (dash > 0) {
                try {
                    int s = Integer.parseInt(part.substring(0, dash).trim());
                    int e = Integer.parseInt(part.substring(dash + 1).trim());
                    if (week >= s && week <= e) return true;
                } catch (NumberFormatException ignored) {
                }
            } else {
                try {
                    if (Integer.parseInt(part.trim()) == week) return true;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return false;
    }

    // ==================== Getters & Setters ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getTeacher() { return teacher; }
    public void setTeacher(String teacher) { this.teacher = teacher; }

    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public Integer getStartPeriod() { return startPeriod; }
    public void setStartPeriod(Integer startPeriod) { this.startPeriod = startPeriod; }

    public Integer getEndPeriod() { return endPeriod; }
    public void setEndPeriod(Integer endPeriod) { this.endPeriod = endPeriod; }

    public String getClassroom() { return classroom; }
    public void setClassroom(String classroom) { this.classroom = classroom; }

    public int getStartWeek() { return startWeek; }
    public void setStartWeek(int startWeek) { this.startWeek = startWeek; }

    public int getEndWeek() { return endWeek; }
    public void setEndWeek(int endWeek) { this.endWeek = endWeek; }

    public String getWeekType() { return weekType; }
    public void setWeekType(String weekType) { this.weekType = weekType; }

    public String getWeekPattern() { return weekPattern; }
    public void setWeekPattern(String weekPattern) { this.weekPattern = weekPattern; }

    public Long getSemesterId() { return semesterId; }
    public void setSemesterId(Long semesterId) { this.semesterId = semesterId; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    /** 课表显示节次范围，如 "3-4"；实践课程返回 "无固定时间" */
    public String getPeriodDisplay() {
        if (startPeriod == null || endPeriod == null) {
            return "无固定时间";
        }
        return startPeriod.intValue() == endPeriod.intValue()
                ? String.valueOf(startPeriod)
                : startPeriod + "-" + endPeriod;
    }

    /** 周次显示，如 "1-16周(单周)"；存在复杂周次模式时优先展示，如 "1,3,5周" */
    public String getWeekDisplay() {
        String suffix = switch (weekType) {
            case WEEK_ODD -> "(单周)";
            case WEEK_EVEN -> "(双周)";
            default -> "";
        };
        if (weekPattern != null && !weekPattern.isBlank()) {
            return weekPattern + "周" + suffix;
        }
        return startWeek + "-" + endWeek + "周" + suffix;
    }

    /** 星期几中文；实践课程返回 "实践" */
    public String getDayDisplay() {
        if (dayOfWeek == null) {
            return "实践";
        }
        return switch (dayOfWeek) {
            case 1 -> "周一";
            case 2 -> "周二";
            case 3 -> "周三";
            case 4 -> "周四";
            case 5 -> "周五";
            case 6 -> "周六";
            case 7 -> "周日";
            default -> "周" + dayOfWeek;
        };
    }
}