package com.youkeda.exercise.claw.feature.schedule.imports;

import com.youkeda.exercise.claw.feature.schedule.CourseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 周次表达式解析器
 *
 * <p>将正方教务系统常见的周次描述解析为 {@link WeekSpec}（起止周 + 单双周 + 复杂周次模式）。
 *
 * <p>支持格式：
 * <ul>
 *   <li>{@code 1-17周} → start=1, end=17, ALL</li>
 *   <li>{@code 1-16(单)} / {@code 1-16单周} → start=1, end=16, ODD</li>
 *   <li>{@code 第1,3,5周} → start=1, end=5, ALL, pattern=1,3,5</li>
 *   <li>{@code 1-8,11-17周(双)} → start=1, end=17, EVEN, pattern=1-8,11-17</li>
 *   <li>{@code 单周} / {@code 双周} → 1-20 单/双周</li>
 * </ul>
 *
 * <p>{@code weekPattern} 仅在无法用 (start, end, weekType) 表达的复杂模式下保留，
 * 供 {@link CourseEntity#isActiveInWeek(int)} 精确匹配。
 */
public final class WeekParser {

    private static final int DEFAULT_START = 1;
    private static final int DEFAULT_END = 20;

    /** 匹配周次段：如 1 / 1-8 / 11-17 */
    private static final Pattern SEGMENT_PATTERN = Pattern.compile("(\\d{1,2})(?:\\s*[-~至到]\\s*(\\d{1,2}))?");

    private WeekParser() {
    }

    /**
     * 解析结果
     *
     * @param startWeek   起始周（含）
     * @param endWeek     结束周（含）
     * @param weekType    ALL / ODD / EVEN
     * @param weekPattern 复杂周次模式（连续范围时为 null），如 "1,3,5" / "1-8,11-17"
     */
    public record WeekSpec(int startWeek, int endWeek, String weekType, String weekPattern) {
    }

    /**
     * 解析周次表达式，空/无法识别时返回默认 1-20 周 ALL
     */
    public static WeekSpec parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return defaults();
        }

        // 1. 检测并剥离单双周标记
        String weekType = detectWeekType(raw);
        String cleaned = stripWeekTypeMarker(raw);
        cleaned = cleaned.replace("第", "")
                .replace("周次", "")
                .replace("周", "")
                .replace(" ", "")
                .replace("　", "")
                .replace("(", "")
                .replace(")", "")
                .replace("（", "")
                .replace("）", "");

        // 2. 提取所有周次段
        List<int[]> segments = new ArrayList<>();
        Matcher m = SEGMENT_PATTERN.matcher(cleaned);
        while (m.find()) {
            int start = Integer.parseInt(m.group(1));
            int end = m.group(2) != null ? Integer.parseInt(m.group(2)) : start;
            if (end < start) end = start;
            segments.add(new int[]{start, end});
        }

        // 3. 无任何数字 → 仅保留单双周信息（如"单周"→ 1-20 ODD）
        if (segments.isEmpty()) {
            return new WeekSpec(DEFAULT_START, DEFAULT_END, weekType, null);
        }

        // 4. 计算起止周
        int startWeek = segments.stream().mapToInt(s -> s[0]).min().getAsInt();
        int endWeek = segments.stream().mapToInt(s -> s[1]).max().getAsInt();

        // 5. 判定是否需要复杂周次模式：单段连续范围时无需 pattern
        boolean singleContiguous = segments.size() == 1 && segments.get(0)[1] > segments.get(0)[0];
        String weekPattern = singleContiguous ? null : buildPattern(segments);

        return new WeekSpec(startWeek, endWeek, weekType, weekPattern);
    }

    /** 默认周次：1-20 全部周 */
    public static WeekSpec defaults() {
        return new WeekSpec(DEFAULT_START, DEFAULT_END, CourseEntity.WEEK_ALL, null);
    }

    /**
     * 从任意文本检测单双周标记
     *
     * @return ALL / ODD / EVEN；无法识别返回 ALL
     */
    public static String detectWeekType(String text) {
        if (text == null || text.isBlank()) {
            return CourseEntity.WEEK_ALL;
        }
        String t = text.trim().toLowerCase();
        if (t.contains("单")) {
            return CourseEntity.WEEK_ODD;
        }
        if (t.contains("双")) {
            return CourseEntity.WEEK_EVEN;
        }
        if (t.contains("all") || t.contains("全")) {
            return CourseEntity.WEEK_ALL;
        }
        return CourseEntity.WEEK_ALL;
    }

    /**
     * 剥离文本中的单双周标记（单/双/周字 + 括号），返回剩余部分
     */
    private static String stripWeekTypeMarker(String text) {
        if (text == null) {
            return "";
        }
        String t = text;
        t = t.replace("(单)", "").replace("（单）", "").replace("(双)", "").replace("（双）", "")
                .replace("单周", "").replace("双周", "")
                .replace("单", "").replace("双", "");
        return t;
    }

    /** 将周次段数组格式化为规范模式串，如 "1,3,5" / "1-8,11-17" */
    private static String buildPattern(List<int[]> segments) {
        StringBuilder sb = new StringBuilder();
        for (int[] seg : segments) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            if (seg[1] > seg[0]) {
                sb.append(seg[0]).append('-').append(seg[1]);
            } else {
                sb.append(seg[0]);
            }
        }
        return sb.toString();
    }
}
