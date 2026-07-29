package com.youkeda.exercise.claw.schedule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 课表解析器
 *
 * <p>支持三种输入格式：
 * <ul>
 *   <li>图片课表 → LLM 视觉提取结构化 JSON → 解析</li>
 *   <li>PDF 课表 → Tika 提取文本 → LLM 提取结构化 JSON → 解析</li>
 *   <li>Excel 课表 → Apache POI 逐行读取 → 直接构造 CourseEntity</li>
 * </ul>
 *
 * <p>解析结果可通过 {@link #parseAndSave(String, String, CourseRepository)} 直接持久化。
 */
@Component
public class CourseParser {

    private static final Logger log = LoggerFactory.getLogger(CourseParser.class);

    private final ObjectMapper objectMapper;

    public CourseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ==================== 解析 + 持久化（一步完成） ====================

    /**
     * 解析 JSON 并直接保存到数据库（解析 + 持久化一步完成）
     *
     * @param userId     用户标识
     * @param jsonText   LLM 提取的 JSON 文本
     * @param repository 课表仓库
     * @return 保存后的课程列表（含 ID）
     */
    public List<CourseEntity> parseAndSave(String userId, String jsonText, CourseRepository repository) {
        List<CourseEntity> courses = parseFromJson(jsonText);
        if (courses.isEmpty()) {
            log.warn("课表解析为空，跳过保存 | userId={}", userId);
            return List.of();
        }
        return repository.replaceAll(userId, courses);
    }

    /**
     * 解析 Excel 并直接保存到数据库（解析 + 持久化一步完成）
     *
     * @param userId     用户标识
     * @param excelBytes Excel 文件字节
     * @param repository 课表仓库
     * @return 保存后的课程列表（含 ID）
     */
    public List<CourseEntity> parseAndSaveFromExcel(String userId, byte[] excelBytes, CourseRepository repository) {
        List<CourseEntity> courses = parseFromExcel(excelBytes);
        if (courses.isEmpty()) {
            log.warn("Excel 课表解析为空，跳过保存 | userId={}", userId);
            return List.of();
        }
        return repository.replaceAll(userId, courses);
    }

    // ==================== 纯解析（不持久化，用于导入预览） ====================

    /**
     * 从 LLM 返回的结构化 JSON 文本解析课程列表
     *
     * @param jsonText LLM 提取的 JSON 数组文本（也可能是嵌套 JSON）
     * @return 课程列表（不含 userId，需调用者设置）
     */
    public List<CourseEntity> parseFromJson(String jsonText) {
        List<CourseEntity> courses = new ArrayList<>();
        try {
            String clean = jsonText.trim();
            if (clean.startsWith("```json")) {
                clean = clean.substring(7);
            } else if (clean.startsWith("```")) {
                clean = clean.substring(3);
            }
            if (clean.endsWith("```")) {
                clean = clean.substring(0, clean.length() - 3);
            }
            clean = clean.trim();

            JsonNode root = objectMapper.readTree(clean);

            // 可能是 { "courses": [...] } 或直接是 [...]
            JsonNode arr = root.isArray() ? root : root.get("courses");
            if (arr == null || !arr.isArray()) {
                log.warn("课表 JSON 格式异常，缺少 courses 数组 | text={}", truncate(clean, 200));
                return courses;
            }

            for (JsonNode node : arr) {
                try {
                    CourseEntity c = parseSingleCourse(node);
                    if (c != null) {
                        courses.add(c);
                    }
                } catch (Exception e) {
                    log.warn("解析单条课程失败 | node={}", node, e);
                }
            }
        } catch (Exception e) {
            log.error("课表 JSON 解析失败 | text={}", truncate(jsonText, 200), e);
        }
        return courses;
    }

    /**
     * 从 Excel 字节数据解析课表
     *
     * <p>支持两种格式：
     * <ol>
     *   <li>标准表头格式：列名包含 课程名称/教师/星期/节次/教室/周次 等</li>
     *   <li>课表矩阵格式：行为节次、列为星期几的矩阵</li>
     * </ol>
     *
     * @param excelBytes Excel 文件字节
     * @return 课程列表（不含 userId）
     */
    public List<CourseEntity> parseFromExcel(byte[] excelBytes) {
        List<CourseEntity> courses = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() == 0) {
                return courses;
            }

            if (isMatrixFormat(sheet)) {
                return parseMatrixSheet(sheet);
            }

            return parseRowSheet(sheet);
        } catch (Exception e) {
            log.error("Excel 课表解析失败", e);
            return courses;
        }
    }

    // ==================== 单条课程 JSON 解析 ====================

    private CourseEntity parseSingleCourse(JsonNode node) {
        String name = getTextField(node, "course_name", "courseName", "name", "课程名称", "课程名");
        if (name == null || name.isBlank()) {
            return null;
        }

        String teacher = getTextField(node, "teacher", "教师", "授课教师");
        // day_of_week: 先尝试整数解析（LLM 按要求返回 1-7 数字），再降级到中文解析（如"周一"）
        int dayOfWeek = parseIntField(node, "day_of_week", "dayOfWeek", "weekday", "星期", "星期几");
        if (dayOfWeek < 0) {
            String dayText = getTextField(node, "day_of_week", "dayOfWeek", "weekday", "星期", "星期几");
            dayOfWeek = parseDayOfWeek(dayText);
        }
        int startPeriod = parseIntField(node, "start_period", "startPeriod", "start", "开始节次", "节次开始");
        int endPeriod = parseIntField(node, "end_period", "endPeriod", "end", "结束节次", "节次结束");
        String classroom = getTextField(node, "classroom", "class_room", "room", "教室", "地点");
        int startWeek = parseIntField(node, "start_week", "startWeek", "week_start", "开始周", "起始周");
        int endWeek = parseIntField(node, "end_week", "endWeek", "week_end", "结束周", "终止周");
        String weekType = parseWeekType(getTextField(node, "week_type", "weekType", "单双周", "周类型"));

        // 兜底：如果 LLM 返回 ALL，检查所有文本字段中的单双周标记
        if (CourseEntity.WEEK_ALL.equals(weekType)) {
            String detected = detectOddEvenFromNodeFields(node);
            if (detected != null) {
                weekType = detected;
                log.debug("解析器兜底修正单双周 | course={} | detected={}", name, weekType);
            }
        }

        if (dayOfWeek < 1 || dayOfWeek > 7) dayOfWeek = 1;
        if (startPeriod < 1) startPeriod = 1;
        if (endPeriod < startPeriod) endPeriod = startPeriod;
        if (startWeek < 1) startWeek = 1;
        if (endWeek < startWeek) endWeek = startWeek;

        return new CourseEntity(null, name, teacher, dayOfWeek, startPeriod, endPeriod,
                classroom, startWeek, endWeek, weekType);
    }

    // ==================== Excel 矩阵格式解析 ====================

    private boolean isMatrixFormat(Sheet sheet) {
        Row firstRow = sheet.getRow(0);
        if (firstRow == null) return false;

        Row secondRow = sheet.getRow(1);
        if (secondRow == null) return false;

        Cell firstCell = secondRow.getCell(0);
        if (firstCell == null) return false;

        try {
            double val = getNumericCellValue(firstCell);
            return val >= 1 && val <= 12;
        } catch (Exception e) {
            return false;
        }
    }

    private List<CourseEntity> parseMatrixSheet(Sheet sheet) {
        List<CourseEntity> courses = new ArrayList<>();

        Row headerRow = sheet.getRow(0);
        int[] colToDay = new int[20];
        for (int c = 1; c <= 7; c++) {
            String header = getCellString(headerRow, c);
            colToDay[c] = parseDayOfWeek(header);
            if (colToDay[c] == 0) colToDay[c] = c;
        }

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            int period = (int) getNumericCellValue(row.getCell(0));
            if (period < 1 || period > 30) continue;

            for (int c = 1; c <= 7; c++) {
                String cellText = getCellString(row, c);
                if (cellText == null || cellText.isBlank()) continue;

                int dayOfWeek = colToDay[c];
                if (dayOfWeek < 1 || dayOfWeek > 7) continue;

                String[] parts = cellText.split("\\n|\\n|@|\\|");
                String courseName = parts[0].trim();
                if (courseName.isBlank()) continue;

                String teacher = parts.length > 1 ? parts[1].trim() : "";
                String classroom = parts.length > 2 ? parts[2].trim() : "";

                CourseEntity course = new CourseEntity(null, courseName, teacher,
                        dayOfWeek, period, period, classroom,
                        1, 20, CourseEntity.WEEK_ALL);
                courses.add(course);
            }
        }

        log.info("矩阵格式课表解析完成 | 共 {} 条课程", courses.size());
        return courses;
    }

    // ==================== Excel 行格式解析 ====================

    private List<CourseEntity> parseRowSheet(Sheet sheet) {
        List<CourseEntity> courses = new ArrayList<>();

        Row headerRow = sheet.getRow(0);
        if (headerRow == null) return courses;

        int[] colMap = detectColumnMapping(headerRow);

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String name = getCellString(row, colMap[0]);
            if (name == null || name.isBlank()) continue;

            String teacher = getCellString(row, colMap[1]);
            int dayOfWeek = parseDayOfWeek(getCellString(row, colMap[2]));
            int startPeriod = (int) getNumericCellValue(row.getCell(colMap[3]));
            int endPeriod = (int) getNumericCellValue(row.getCell(colMap[4]));
            String classroom = getCellString(row, colMap[5]);
            int startWeek = colMap[6] >= 0 ? (int) getNumericCellValue(row.getCell(colMap[6])) : 1;
            int endWeek = colMap[7] >= 0 ? (int) getNumericCellValue(row.getCell(colMap[7])) : 20;
            String weekType = colMap[8] >= 0 ? parseWeekType(getCellString(row, colMap[8])) : CourseEntity.WEEK_ALL;

            if (dayOfWeek < 1 || dayOfWeek > 7) dayOfWeek = 1;
            if (startPeriod < 1) startPeriod = 1;
            if (endPeriod < startPeriod) endPeriod = startPeriod;
            if (startWeek < 1) startWeek = 1;
            if (endWeek < startWeek) endWeek = startWeek;

            CourseEntity course = new CourseEntity(null, name, teacher, dayOfWeek,
                    startPeriod, endPeriod, classroom, startWeek, endWeek, weekType);
            courses.add(course);
        }

        log.info("行格式课表解析完成 | 共 {} 条课程", courses.size());
        return courses;
    }

    private int[] detectColumnMapping(Row headerRow) {
        int[] map = new int[9];
        for (int i = 0; i < 9; i++) map[i] = -1;

        for (int c = 0; c <= headerRow.getLastCellNum(); c++) {
            String header = getCellString(headerRow, c);
            if (header == null) continue;
            String h = header.trim().toLowerCase();

            if (matchesAny(h, "课程名称", "课程名", "课程", "course_name", "coursename", "name", "科目")) {
                map[0] = c;
            } else if (matchesAny(h, "教师", "授课教师", "老师", "teacher", "授课老师")) {
                map[1] = c;
            } else if (matchesAny(h, "星期", "星期几", "星期数", "day_of_week", "weekday", "day", "周几")) {
                map[2] = c;
            } else if (matchesAny(h, "开始节次", "节次开始", "start_period", "startperiod", "节次")) {
                map[3] = c;
            } else if (matchesAny(h, "结束节次", "节次结束", "end_period", "endperiod", "end")) {
                map[4] = c;
            } else if (matchesAny(h, "教室", "上课地点", "地点", "classroom", "room", "位置")) {
                map[5] = c;
            } else if (matchesAny(h, "开始周", "开始周次", "起始周", "start_week", "startweek", "week_start")) {
                map[6] = c;
            } else if (matchesAny(h, "结束周", "结束周次", "终止周", "end_week", "endweek", "week_end")) {
                map[7] = c;
            } else if (matchesAny(h, "单双周", "周类型", "week_type", "weektype", "odd/even")) {
                map[8] = c;
            }
        }

        if (map[2] < 0) map[2] = 2;
        if (map[3] < 0) map[3] = 3;
        if (map[4] < 0) map[4] = 4;

        return map;
    }

    // ==================== 辅助方法 ====================

    private String getTextField(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode v = node.get(key);
            if (v != null && v.isTextual() && !v.asText().isBlank()) {
                return v.asText().trim();
            }
        }
        return null;
    }

    private int parseIntField(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode v = node.get(key);
            if (v != null) {
                if (v.isInt()) return v.asInt();
                if (v.isTextual()) {
                    try {
                        return Integer.parseInt(v.asText().trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return -1;
    }

    private int parseDayOfWeek(String text) {
        if (text == null || text.isBlank()) return 0;
        String t = text.trim();
        if (t.contains("一") || t.equals("1") || t.contains("mon")) return 1;
        if (t.contains("二") || t.equals("2") || t.contains("tue")) return 2;
        if (t.contains("三") || t.equals("3") || t.contains("wed")) return 3;
        if (t.contains("四") || t.equals("4") || t.contains("thu")) return 4;
        if (t.contains("五") || t.equals("5") || t.contains("fri")) return 5;
        if (t.contains("六") || t.equals("6") || t.contains("sat")) return 6;
        if (t.contains("日") || t.contains("天") || t.equals("7") || t.contains("sun")) return 7;
        try {
            int n = Integer.parseInt(t);
            if (n >= 1 && n <= 7) return n;
        } catch (NumberFormatException ignored) {
        }
        return 0;
    }

    private String parseWeekType(String text) {
        if (text == null || text.isBlank()) return CourseEntity.WEEK_ALL;
        String t = text.trim().toLowerCase();
        if (t.contains("单") || t.equals("odd")) return CourseEntity.WEEK_ODD;
        if (t.contains("双") || t.equals("even")) return CourseEntity.WEEK_EVEN;
        if (t.equals("all") || t.equals("全部") || t.contains("全")) return CourseEntity.WEEK_ALL;
        return CourseEntity.WEEK_ALL;
    }

    /**
     * 扫描 JSON 节点所有文本字段，检测单双周标记
     * <p>兜底机制：当 LLM 返回的 week_type=ALL 时，尝试从其他字段中提取单双周信息。
     * 优先级：原始文本标记 &gt; LLM 返回值 &gt; ALL</p>
     */
    private String detectOddEvenFromNodeFields(JsonNode node) {
        Iterator<java.util.Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            JsonNode value = fields.next().getValue();
            if (value != null && value.isTextual()) {
                String text = value.asText();
                if (text.contains("(双)") || text.contains("（双）") || text.contains("双周")) {
                    return CourseEntity.WEEK_EVEN;
                }
                if (text.contains("(单)") || text.contains("（单）") || text.contains("单周")) {
                    return CourseEntity.WEEK_ODD;
                }
            }
        }
        return null;
    }

    private String getCellString(Row row, int col) {
        if (row == null || col < 0) return "";
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                yield v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf((int) cell.getNumericCellValue());
                } catch (Exception e) {
                    try {
                        yield cell.getStringCellValue();
                    } catch (Exception e2) {
                        yield "";
                    }
                }
            }
            default -> "";
        };
    }

    private double getNumericCellValue(Cell cell) {
        if (cell == null) return 0;
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> cell.getNumericCellValue();
                case STRING -> {
                    try {
                        yield Double.parseDouble(cell.getStringCellValue().trim());
                    } catch (NumberFormatException e) {
                        yield 0;
                    }
                }
                case FORMULA -> cell.getNumericCellValue();
                default -> 0;
            };
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean matchesAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}