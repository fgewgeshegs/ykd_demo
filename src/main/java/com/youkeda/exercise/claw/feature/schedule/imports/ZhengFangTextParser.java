package com.youkeda.exercise.claw.feature.schedule.imports;

import com.youkeda.exercise.claw.feature.schedule.CourseEntity;
import com.youkeda.exercise.claw.feature.schedule.imports.WeekParser.WeekSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正方教务系统粘贴文本解析器（规则优先）
 *
 * <p>解析用户从正方教务网页复制粘贴的课表文本，识别三种常见格式：
 * <ol>
 *   <li><b>Tab 矩阵</b>：表头「星期一\t星期二…」，行为节次，格内为课程（对应网页表格复制）</li>
 *   <li><b>标签格式</b>：形如「课程名称：高等数学 / 上课时间：星期一 第1-2节 / 周次：1-16周」</li>
 *   <li><b>块格式</b>：时间描述行 + 课程信息行，如「1-16周(单) 星期一 第3-4节」+「高等数学 段代凤 教2-203」，或同行内联</li>
 * </ol>
 *
 * <p>识别：单双周（单/双标记）、复杂周次（1,3,5 / 1-8,11-17）、实践课（实践/实习/无固定时间/分散/集中 → 无 weekday/period）。
 * 规则无法解析时由调用方（{@link CourseImportPipeline}）回退 LLM 结构化提取。
 *
 * <p>本解析器为纯规则实现，不依赖 LLM，可直接单元测试。
 */
@Component
public class ZhengFangTextParser {

    private static final Logger log = LoggerFactory.getLogger(ZhengFangTextParser.class);

    /** 节次范围：第1-2节 / 3-4节 */
    private static final Pattern PERIOD_RANGE = Pattern.compile("第?(\\d{1,2})\\s*[-~至到]\\s*(\\d{1,2})\\s*节");
    /** 单节次：第3节 / 5节 */
    private static final Pattern PERIOD_SINGLE = Pattern.compile("第?(\\d{1,2})\\s*节");
    /** 星期：星期一~日 / 周一~日 / 周1-7 */
    private static final Pattern DAY_PATTERN = Pattern.compile("(?:星期|周)\\s*([一二三四五六日天]|[1-7])");
    /** 周次（带"周"后缀或单双括号）：1-16周 / 第1,3,5周 / 1-8,11-17周(单) */
    private static final Pattern WEEK_EXPR = Pattern.compile(
            "第?\\s*(\\d{1,2}(?:\\s*[-~至到]\\s*\\d{1,2})?(?:[,，]\\s*\\d{1,2}(?:\\s*[-~至到]\\s*\\d{1,2})?)*)"
                    + "\\s*周(?:次)?\\s*[（(]?([单双])?[)）]?");
    /** 周次（无"周"后缀但带单双括号）：1-16(单) */
    private static final Pattern WEEK_EXPR_PARITY_ONLY = Pattern.compile(
            "第?\\s*(\\d{1,2}(?:\\s*[-~至到]\\s*\\d{1,2})?(?:[,，]\\s*\\d{1,2})*)"
                    + "\\s*[（(]([单双])[)）]");

    /** 教室标记 */
    private static final String[] ROOM_MARKERS = {
            "教", "楼", "馆", "室", "实验", "实训", "操场", "体育馆", "基地", "实验室", "机房"
    };

    /** 实践课标记（无固定时间/分散进行等） */
    private static final String[] PRACTICE_MARKERS = {
            "实践", "实习", "无固定时间", "分散", "集中", "课程设计", "毕业设计", "实训", "劳动"
    };

    private static final String[] DAY_NAMES = {"", "一", "二", "三", "四", "五", "六", "日", "天"};

    // ==================== 入口 ====================

    /**
     * 规则解析正方粘贴文本，返回课程列表（不含 userId / source）
     */
    public List<CourseEntity> parseByRules(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> rawLines = splitRawLines(text);

        // 1. Tab 矩阵格式
        Integer headerIdx = findMatrixHeader(rawLines);
        if (headerIdx != null) {
            List<CourseEntity> matrix = parseMatrix(rawLines, headerIdx);
            if (!matrix.isEmpty()) {
                log.info("正方文本：矩阵格式解析完成 | courses={}", matrix.size());
                return matrix;
            }
        }

        // 2. 标签格式（课程名称：…）——须在去标签前识别
        if (hasLabeledFormat(rawLines)) {
            List<CourseEntity> labeled = parseLabeled(rawLines);
            if (!labeled.isEmpty()) {
                log.info("正方文本：标签格式解析完成 | courses={}", labeled.size());
                return labeled;
            }
        }

        // 3. 块格式（时间描述行 + 课程信息行）
        List<String> stripped = new ArrayList<>();
        for (String line : rawLines) {
            String s = stripLabels(line).trim();
            if (!s.isBlank()) {
                stripped.add(s);
            }
        }
        List<CourseEntity> blocks = parseBlocks(stripped);
        log.info("正方文本：块格式解析完成 | courses={}", blocks.size());
        return blocks;
    }

    // ==================== 预处理 ====================

    private List<String> splitRawLines(String text) {
        List<String> result = new ArrayList<>();
        for (String raw : text.split("\\r?\\n")) {
            String line = raw.trim();
            if (!line.isBlank()) {
                result.add(line);
            }
        }
        return result;
    }

    /** 去除常见标签前缀：课程名称： / 教师： / 上课时间： 等 */
    private String stripLabels(String text) {
        String t = text;
        t = t.replaceAll("课程名称\\s*[:：]", " ");
        t = t.replaceAll("课程名\\s*[:：]", " ");
        t = t.replaceAll("授课教师\\s*[:：]", " ");
        t = t.replaceAll("教师\\s*[:：]", " ");
        t = t.replaceAll("老师\\s*[:：]", " ");
        t = t.replaceAll("上课时间\\s*[:：]", " ");
        t = t.replaceAll("上课地点\\s*[:：]", " ");
        t = t.replaceAll("周次\\s*[:：]", " ");
        t = t.replaceAll("时间\\s*[:：]", " ");
        t = t.replaceAll("地点\\s*[:：]", " ");
        t = t.replaceAll("教室\\s*[:：]", " ");
        t = t.replaceAll("节次\\s*[:：]", " ");
        return t;
    }

    // ==================== 矩阵格式 ====================

    /** 查找矩阵表头行（含 ≥2 个星期名且含 Tab） */
    private Integer findMatrixHeader(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.contains("\t")) {
                continue;
            }
            int dayCount = 0;
            for (String name : new String[]{"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"}) {
                if (line.contains(name)) {
                    dayCount++;
                }
            }
            if (dayCount >= 2) {
                return i;
            }
        }
        return null;
    }

    private List<CourseEntity> parseMatrix(List<String> lines, int headerIdx) {
        List<CourseEntity> result = new ArrayList<>();
        String[] headerCells = lines.get(headerIdx).split("\t");
        Map<Integer, Integer> colToDay = new HashMap<>();
        for (int c = 0; c < headerCells.length; c++) {
            int day = parseDayOfWeek(headerCells[c].trim());
            if (day > 0) {
                colToDay.put(c, day);
            }
        }
        // 数据行首列为节次标签；若表头首列已是星期（复制时未含节次标签列），数据列需左移 1 对齐
        int headerOffset = parseDayOfWeek(headerCells[0].trim()) > 0 ? 1 : 0;

        for (int r = headerIdx + 1; r < lines.size(); r++) {
            String[] cells = lines.get(r).split("\t");
            if (cells.length < 2) {
                continue;
            }
            int[] period = parsePeriodLabel(cells[0].trim());
            if (period == null) {
                continue;
            }
            for (int c = 1; c < cells.length; c++) {
                int headerCol = c - headerOffset;
                Integer day = colToDay.get(headerCol);
                if (day == null) {
                    continue;
                }
                String cellText = cells[c].trim();
                if (cellText.isBlank()) {
                    continue;
                }
                CourseEntity e = buildFromCell(cellText, day, period[0], period[1]);
                if (e != null) {
                    result.add(e);
                }
            }
        }
        return result;
    }

    /** 从矩阵单元格文本构建课程（单元格可能含周次信息） */
    private CourseEntity buildFromCell(String cellText, int day, int startPeriod, int endPeriod) {
        WeekSpec week = extractWeek(cellText);
        if (week == null) {
            week = WeekParser.defaults();
        }
        String info = removeTimeInfo(cellText);
        return parseCourseInfo(info, day, startPeriod, endPeriod, week);
    }

    /** 解析节次标签：1-2节 / 第3节 / 3 / 3,4 */
    private int[] parsePeriodLabel(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        Matcher mr = PERIOD_RANGE.matcher(label);
        if (mr.find()) {
            return new int[]{Integer.parseInt(mr.group(1)), Integer.parseInt(mr.group(2))};
        }
        Matcher ms = PERIOD_SINGLE.matcher(label);
        if (ms.find()) {
            return new int[]{Integer.parseInt(ms.group(1)), Integer.parseInt(ms.group(1))};
        }
        // 纯数字或 "1,2"
        String t = label.replaceAll("[^0-9,，]", "");
        String[] parts = t.split("[,，]");
        if (parts.length == 1 && !parts[0].isBlank()) {
            int n = Integer.parseInt(parts[0]);
            return new int[]{n, n};
        }
        if (parts.length >= 2) {
            try {
                int s = Integer.parseInt(parts[0]);
                int e = Integer.parseInt(parts[parts.length - 1]);
                return new int[]{s, e};
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    // ==================== 标签格式 ====================

    private boolean hasLabeledFormat(List<String> lines) {
        return lines.stream().anyMatch(l -> l.contains("课程名称") || l.contains("课程名"));
    }

    private List<CourseEntity> parseLabeled(List<String> lines) {
        List<List<String>> records = new ArrayList<>();
        List<String> current = new ArrayList<>();
        for (String line : lines) {
            if (line.contains("课程名称") || line.contains("课程名")) {
                if (!current.isEmpty()) {
                    records.add(current);
                }
                current = new ArrayList<>();
            }
            current.add(line);
        }
        if (!current.isEmpty()) {
            records.add(current);
        }

        List<CourseEntity> result = new ArrayList<>();
        for (List<String> record : records) {
            CourseEntity e = buildFromRecord(record);
            if (e != null) {
                result.add(e);
            }
        }
        return result;
    }

    private CourseEntity buildFromRecord(List<String> record) {
        String name = "", teacher = "", room = "";
        StringBuilder timeBuf = new StringBuilder();
        boolean practice = false;
        for (String line : record) {
            if (line.contains("课程名称") || line.contains("课程名")) {
                name = afterLabel(line, "课程名称", "课程名");
            } else if (line.contains("教师") || line.contains("老师")) {
                teacher = afterLabel(line, "授课教师", "教师", "老师");
            } else if (line.contains("地点") || line.contains("教室") || line.contains("实验室")) {
                room = afterLabel(line, "上课地点", "地点", "教室", "实验室");
            } else if (line.contains("上课时间") || line.contains("时间")) {
                timeBuf.append(' ').append(afterLabel(line, "上课时间", "时间"));
            } else if (line.contains("周次")) {
                timeBuf.append(' ').append(afterLabel(line, "周次"));
            } else if (isPracticeMarker(line)) {
                practice = true;
                timeBuf.append(' ').append(line);
            }
        }
        if (name.isBlank()) {
            return null;
        }

        Integer day = extractDay(timeBuf.toString());
        int[] period = extractPeriod(timeBuf.toString());
        WeekSpec week = extractWeek(timeBuf.toString());
        if (week == null) {
            week = WeekParser.defaults();
        }

        if ((day == null || period == null) && !practice) {
            // 缺少固定时间且非实践课 → 丢弃
            return null;
        }
        return parseCourseInfo(name + " " + teacher + " " + room,
                day, period != null ? period[0] : null, period != null ? period[1] : null, week);
    }

    private String afterLabel(String line, String... labels) {
        for (String label : labels) {
            int idx = line.indexOf(label);
            if (idx >= 0) {
                String v = line.substring(idx + label.length()).trim();
                return v.replaceAll("^[:：\\s]+", "").trim();
            }
        }
        return "";
    }

    // ==================== 块格式 ====================

    private List<CourseEntity> parseBlocks(List<String> lines) {
        List<CourseEntity> result = new ArrayList<>();
        CourseDescriptor pending = null;
        List<String> content = new ArrayList<>();
        List<String> buffered = new ArrayList<>(); // 时间描述行之前的内容（倒序格式兜底）

        for (String line : lines) {
            CourseDescriptor d = tryParseDescriptor(line);
            if (d != null) {
                // 新记录开始：先冲刷上一个
                flushBlock(result, pending, content);
                // 新块内容从缓冲开始（仅倒序格式下缓冲非空），不继承上一个块的 content
                List<String> blockContent = new ArrayList<>(buffered);
                buffered = new ArrayList<>();
                // 描述行本身可能含课程名（内联格式）
                String namePart = removeTimeInfo(line);
                if (!namePart.isBlank()) {
                    blockContent.add(namePart);
                }
                pending = d;
                content = blockContent;
            } else {
                if (pending != null) {
                    content.add(line);
                } else {
                    buffered.add(line);
                }
            }
        }
        flushBlock(result, pending, content);
        return result;
    }

    private void flushBlock(List<CourseEntity> result, CourseDescriptor d, List<String> content) {
        if (d == null) {
            return;
        }
        String courseText = String.join(" ", content).trim();
        if (courseText.isBlank()) {
            return;
        }
        WeekSpec week = d.week() != null ? d.week() : WeekParser.defaults();
        CourseEntity e = parseCourseInfo(courseText,
                d.day(), d.startPeriod(), d.endPeriod(), week);
        if (e != null) {
            result.add(e);
        }
    }

    /** 判断一行是否为时间描述行（含星期/节次/周次/实践标记） */
    private CourseDescriptor tryParseDescriptor(String line) {
        Integer day = extractDay(line);
        int[] period = extractPeriod(line);
        WeekSpec week = extractWeek(line);
        boolean practice = day == null && period == null && isPracticeMarker(line);
        if (day == null && period == null && week == null && !practice) {
            return null;
        }
        return new CourseDescriptor(day, period, week, practice);
    }

    /** 时间描述行信息 */
    private record CourseDescriptor(Integer day, int[] period, WeekSpec week, boolean practice) {
        Integer startPeriod() {
            return period != null ? period[0] : null;
        }

        Integer endPeriod() {
            return period != null ? period[1] : null;
        }
    }

    // ==================== 时间信息提取 ====================

    private Integer extractDay(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = DAY_PATTERN.matcher(text);
        if (m.find()) {
            String token = m.group(1);
            for (int d = 1; d <= 7; d++) {
                if (DAY_NAMES[d].equals(token)) {
                    return d;
                }
            }
            try {
                int n = Integer.parseInt(token);
                if (n >= 1 && n <= 7) {
                    return n;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private int[] extractPeriod(String text) {
        if (text == null) {
            return null;
        }
        Matcher mr = PERIOD_RANGE.matcher(text);
        if (mr.find()) {
            return new int[]{Integer.parseInt(mr.group(1)), Integer.parseInt(mr.group(2))};
        }
        Matcher ms = PERIOD_SINGLE.matcher(text);
        if (ms.find()) {
            int n = Integer.parseInt(ms.group(1));
            return new int[]{n, n};
        }
        return null;
    }

    private WeekSpec extractWeek(String text) {
        if (text == null) {
            return null;
        }
        Matcher m = WEEK_EXPR.matcher(text);
        if (m.find()) {
            String weeks = m.group(1).replace("第", "");
            String parity = m.group(2);
            return WeekParser.parse(parity != null ? weeks + "(" + parity + ")" : weeks);
        }
        Matcher mp = WEEK_EXPR_PARITY_ONLY.matcher(text);
        if (mp.find()) {
            String weeks = mp.group(1).replace("第", "");
            return WeekParser.parse(weeks + "(" + mp.group(2) + ")");
        }
        if (text.contains("单周")) {
            return new WeekSpec(1, 20, CourseEntity.WEEK_ODD, null);
        }
        if (text.contains("双周")) {
            return new WeekSpec(1, 20, CourseEntity.WEEK_EVEN, null);
        }
        return null;
    }

    /** 移除行内的时间信息（星期/节次/周次），返回剩余课程文本 */
    private String removeTimeInfo(String text) {
        if (text == null) {
            return "";
        }
        String t = text;
        t = PERIOD_RANGE.matcher(t).replaceAll(" ");
        t = PERIOD_SINGLE.matcher(t).replaceAll(" ");
        t = DAY_PATTERN.matcher(t).replaceAll(" ");
        t = WEEK_EXPR.matcher(t).replaceAll(" ");
        t = WEEK_EXPR_PARITY_ONLY.matcher(t).replaceAll(" ");
        t = t.replace("单周", " ").replace("双周", " ");
        return t.trim();
    }

    // ==================== 课程信息提取 ====================

    /**
     * 从课程文本构建课程实体：名称 / 教师 / 教室
     * <p>day/start/end 为 null 时构建实践课（无固定时间）。</p>
     */
    private CourseEntity parseCourseInfo(String text, Integer day, Integer startPeriod, Integer endPeriod, WeekSpec week) {
        if (text == null) {
            return null;
        }
        String name = "", teacher = "", room = "";
        for (String token : text.split("[\\s\\t]+")) {
            if (token.isBlank()) {
                continue;
            }
            if (isRoomToken(token)) {
                if (room.isBlank()) {
                    room = token;
                }
            } else if (name.isBlank()) {
                name = token;
            } else if (teacher.isBlank()) {
                teacher = token;
            } else {
                // 多余 token 追加到课程名（如课程名含空格）
                name = name + " " + token;
            }
        }
        if (name.isBlank()) {
            return null;
        }
        CourseEntity c = CourseEntity.create(null, name, teacher,
                day, startPeriod, endPeriod,
                room, week.startWeek(), week.endWeek(), week.weekType());
        c.setWeekPattern(week.weekPattern());
        return c;
    }

    private boolean isRoomToken(String token) {
        for (String marker : ROOM_MARKERS) {
            if (token.contains(marker)) {
                return true;
            }
        }
        // 纯字母+数字教室编码，如 A101 / C3
        return token.matches("[A-Za-z]?\\d{1,4}");
    }

    private boolean isPracticeMarker(String text) {
        if (text == null) {
            return false;
        }
        for (String marker : PRACTICE_MARKERS) {
            if (text.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private int parseDayOfWeek(String text) {
        if (text == null) {
            return 0;
        }
        for (int d = 1; d <= 7; d++) {
            if (text.contains("星期" + DAY_NAMES[d]) || text.contains("周" + DAY_NAMES[d])) {
                return d;
            }
        }
        return 0;
    }
}
