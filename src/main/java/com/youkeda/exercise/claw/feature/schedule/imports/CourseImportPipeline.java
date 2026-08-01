package com.youkeda.exercise.claw.feature.schedule.imports;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.ai.vision.VisionService;
import com.youkeda.exercise.claw.feature.schedule.CourseEntity;
import com.youkeda.exercise.claw.feature.schedule.CourseParser;
import com.youkeda.exercise.claw.feature.schedule.pdf.PdfTableExtractor;
import com.youkeda.exercise.claw.feature.schedule.pdf.ScheduleCell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 课程导入统一管线
 *
 * <p>多级导入的编排层，所有数据源（正方文本 / Excel / PDF / 图片 OCR / 文档）汇入同一流程：
 * <pre>
 *   原始输入 → 解析器 → List&lt;CourseEntity&gt; → {@link CourseScheduleValidator}（规范化/合并/冲突）
 *              → 预览 → 用户确认 → 持久化
 * </pre>
 *
 * <p>数据源优先级：{@link SourceType#ZHENGFANG}(最高) → EXCEL → PDF → OCR(兜底)。
 * 解析结果统一标注 {@code source}，便于溯源诊断。
 */
@Component
public class CourseImportPipeline {

    private static final Logger log = LoggerFactory.getLogger(CourseImportPipeline.class);

    /** 图片视觉识别提示词（结构化 JSON 输出） */
    public static final String COURSE_IMAGE_PROMPT =
            "你是一位课表识别专家。请从这张课表图片中提取所有课程信息。\n"
                    + "请严格按照以下 JSON 数组格式返回，不要添加额外说明：\n"
                    + "[\n"
                    + "  {\n"
                    + "    \"course_name\": \"课程名称\",\n"
                    + "    \"teacher\": \"教师姓名\",\n"
                    + "    \"day_of_week\": 1,\n"
                    + "    \"start_period\": 1,\n"
                    + "    \"end_period\": 2,\n"
                    + "    \"classroom\": \"教室\",\n"
                    + "    \"start_week\": 1,\n"
                    + "    \"end_week\": 16,\n"
                    + "    \"week_type\": \"ALL\"\n"
                    + "  }\n"
                    + "]\n"
                    + "字段说明：\n"
                    + "- course_name: 课程名称（必填）\n"
                    + "- teacher: 授课教师（可选，无则填空字符串）\n"
                    + "- day_of_week: 星期几，必填数字。映射规则：\n"
                    + "  星期一=1, 星期二=2, 星期三=3, 星期四=4, 星期五=5, 星期六=6, 星期日=7\n"
                    + "  判断方法：原始课表是星期列结构，请根据列标题确定每门课的星期：\n"
                    + "  - 列标题为【星期一】时 → day_of_week=1\n"
                    + "  - 列标题为【星期二】时 → day_of_week=2\n"
                    + "  - 以此类推...\n"
                    + "  ⚠️ 禁止：不要根据课程在文本中出现的顺序依次分配1,2,3,4,5,6,7。\n"
                    + "  每门课的 day_of_week 只由它在课表原始表格中的列决定。\n"
                    + "- start_period: 开始节次，从1开始（必填）\n"
                    + "- end_period: 结束节次（必填）\n"
                    + "- classroom: 教室/地点（可选）\n"
                    + "- start_week: 开始周，默认1\n"
                    + "- end_week: 结束周，默认20\n"
                    + "- week_type: ALL=全部周, ODD=单周, EVEN=双周（必填，禁止默认ALL）\n"
                    + "  如果课表文本中出现(单)、单周，则填 ODD\n"
                    + "  如果出现(双)、双周，则填 EVEN\n"
                    + "  只有确认每周都有课才填 ALL\n"
                    + "如果某门课是实践课/无固定时间课程（无星期、无固定节次），返回 \"is_practice\": true 且省略 day_of_week/start_period/end_period。";

    /** PDF 课表单元格提取提示词（dayOfWeek/period 已由 PdfTableExtractor 确定） */
    public static final String COURSE_PDF_CELL_PROMPT =
            "你是一位课表识别专家。以下是从PDF课表中提取的课程单元格，"
                    + "每个单元格的星期(day_of_week)和节次(period)已由PDF表格分析正确确定。\n"
                    + "请从每个单元格的文本中提取课程信息，返回JSON数组。\n"
                    + "每个单元格返回一个对象，包含：\n"
                    + "- \"cell_index\": 单元格序号（与输入对应）\n"
                    + "- \"course_name\": 课程名称（必填）\n"
                    + "- \"teacher\": 授课教师（可选，无则填空字符串）\n"
                    + "- \"classroom\": 教室/地点（可选，无则填空字符串）\n"
                    + "- \"start_week\": 开始周（必填）\n"
                    + "- \"end_week\": 结束周（必填）\n"
                    + "- \"week_type\": \"ALL\"或\"ODD\"或\"EVEN\"（必填）\n"
                    + "  - 如果课表文本中出现(双)、双周则填 EVEN\n"
                    + "  - 如果出现(单)、单周则填 ODD\n"
                    + "  - 只有确认每周都有课才填 ALL\n"
                    + "格式示例：\n"
                    + "[\n"
                    + "  {\"cell_index\":0,\"course_name\":\"概率统计\",\"teacher\":\"常远\","
                    + "\"classroom\":\"C3敏学楼404\",\"start_week\":1,\"end_week\":16,\"week_type\":\"EVEN\"}\n"
                    + "]\n"
                    + "注意：▲标记表示课程名称所在行。";

    /** 通用文档/文本课表提取提示词 */
    public static final String COURSE_DOC_PROMPT =
            "你是一位课表识别专家。以下是从课表文档中提取的文本内容，"
                    + "请从中提取所有课程信息。\n"
                    + "请严格按照以下 JSON 数组格式返回，不要添加额外说明：\n"
                    + "[\n"
                    + "  {\n"
                    + "    \"course_name\": \"课程名称\",\n"
                    + "    \"teacher\": \"教师姓名\",\n"
                    + "    \"day_of_week\": 1,\n"
                    + "    \"start_period\": 1,\n"
                    + "    \"end_period\": 2,\n"
                    + "    \"classroom\": \"教室\",\n"
                    + "    \"start_week\": 1,\n"
                    + "    \"end_week\": 16,\n"
                    + "    \"week_type\": \"ALL\"\n"
                    + "  }\n"
                    + "]\n"
                    + "字段说明：\n"
                    + "- course_name: 课程名称（必填）\n"
                    + "- teacher: 授课教师（可选，无则填空字符串）\n"
                    + "- day_of_week: 星期几，必填数字。映射规则：\n"
                    + "  星期一=1, 星期二=2, 星期三=3, 星期四=4, 星期五=5, 星期六=6, 星期日=7\n"
                    + "  判断方法：原始课表是星期列结构，请根据列标题确定每门课的星期：\n"
                    + "  - 列标题为【星期一】时 → day_of_week=1\n"
                    + "  - 列标题为【星期二】时 → day_of_week=2\n"
                    + "  - 以此类推...\n"
                    + "  ⚠️ 禁止：不要根据课程在文本中出现的顺序依次分配1,2,3,4,5,6,7。\n"
                    + "  每门课的 day_of_week 只由它在课表原始表格中的列决定。\n"
                    + "- start_period: 开始节次，从1开始（必填）\n"
                    + "- end_period: 结束节次（必填）\n"
                    + "- classroom: 教室/地点（可选）\n"
                    + "- start_week: 开始周，默认1\n"
                    + "- end_week: 结束周，默认20\n"
                    + "- week_type: ALL=全部周, ODD=单周, EVEN=双周（必填，禁止默认ALL）\n"
                    + "  如果课表文本中出现(单)、单周，则填 ODD\n"
                    + "  如果出现(双)、双周，则填 EVEN\n"
                    + "  只有确认每周都有课才填 ALL\n"
                    + "如果某门课是实践课/无固定时间课程（无星期、无固定节次），返回 \"is_practice\": true 且省略 day_of_week/start_period/end_period。";

    private final CourseScheduleValidator validator;
    private final ZhengFangTextParser zhengFangTextParser;
    private final CourseParser courseParser;
    private final PdfTableExtractor pdfTableExtractor;
    private final VisionService visionService;
    private final LLMClient llmClient;
    private final ObjectMapper objectMapper;

    public CourseImportPipeline(CourseScheduleValidator validator,
                                ZhengFangTextParser zhengFangTextParser,
                                CourseParser courseParser,
                                PdfTableExtractor pdfTableExtractor,
                                VisionService visionService,
                                LLMClient llmClient,
                                ObjectMapper objectMapper) {
        this.validator = validator;
        this.zhengFangTextParser = zhengFangTextParser;
        this.courseParser = courseParser;
        this.pdfTableExtractor = pdfTableExtractor;
        this.visionService = visionService;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 导入批次结果
     *
     * @param courses  规范化后的课程列表（含 source）
     * @param warnings 校验告警（冲突、缺失信息等，不阻断）
     */
    public record ImportBatch(List<CourseEntity> courses, List<String> warnings) {
        public boolean isEmpty() {
            return courses == null || courses.isEmpty();
        }
    }

    // ==================== 通用校验 ====================

    /**
     * 校验并标注来源
     */
    public ImportBatch process(List<CourseEntity> rawCourses, SourceType source) {
        if (rawCourses == null) {
            rawCourses = List.of();
        }
        rawCourses.forEach(c -> c.setSource(source.code()));
        CourseScheduleValidator.ValidationResult r = validator.validate(rawCourses);
        return new ImportBatch(r.courses(), r.warnings());
    }

    // ==================== 正方文本 ====================

    /**
     * 正方教务系统粘贴文本：规则解析为主，LLM 兜底
     */
    public ImportBatch importZhengFangText(String text) {
        List<CourseEntity> courses = zhengFangTextParser.parseByRules(text);
        if (courses.isEmpty()) {
            log.info("正方文本规则解析为空，回退 LLM 结构化提取 | textLen={}", text == null ? 0 : text.length());
            String llmResult = llmClient.chatWithSystemPrompt(COURSE_DOC_PROMPT, text);
            if (llmResult != null && !llmResult.isBlank()) {
                courses = courseParser.parseFromJson(llmResult);
            }
        }
        return process(courses, SourceType.ZHENGFANG);
    }

    // ==================== 通用文档（doc/txt） ====================

    /**
     * 通用文档课表：Tika 文本 → LLM 结构化提取 → 校验
     */
    public ImportBatch importDocText(String text, String fileName) {
        String docContent = "文件名：" + (fileName != null ? fileName : "未知") + "\n\n文件内容：\n" + text;
        String llmResult = llmClient.chatWithSystemPrompt(COURSE_DOC_PROMPT, docContent);
        if (llmResult == null || llmResult.isBlank()) {
            log.warn("课表导入：文档 LLM 提取失败");
            return new ImportBatch(List.of(), List.of("无法从文档中提取课程信息"));
        }
        List<CourseEntity> courses = courseParser.parseFromJson(llmResult);
        return process(courses, SourceType.DOC);
    }

    // ==================== Excel ====================

    /**
     * Excel 课表：Apache POI 解析
     */
    public ImportBatch importExcel(byte[] bytes) {
        List<CourseEntity> courses = courseParser.parseFromExcel(bytes);
        return process(courses, SourceType.EXCEL);
    }

    // ==================== PDF ====================

    /**
     * PDF 课表：PdfTableExtractor 恢复 (星期, 节次) 结构 → LLM 提取课程信息
     */
    public ImportBatch importPdf(byte[] bytes) {
        List<ScheduleCell> cells = pdfTableExtractor.extract(bytes);
        if (cells.isEmpty()) {
            log.warn("PDF 表格恢复为空 | 返回空批次，调用方可回退文档解析");
            return new ImportBatch(List.of(), List.of("PDF 表格结构恢复失败"));
        }

        String cellText = buildCellsPromptText(cells);
        String llmResult = llmClient.chatWithSystemPrompt(COURSE_PDF_CELL_PROMPT, cellText);
        if (llmResult == null || llmResult.isBlank()) {
            return new ImportBatch(List.of(), List.of("PDF 课程信息提取失败"));
        }

        List<CourseEntity> courses = buildCoursesFromCells(cells, llmResult);
        return process(courses, SourceType.PDF);
    }

    // ==================== 图片 OCR（兜底） ====================

    /**
     * 图片课表：视觉模型识别 → JSON → 校验
     *
     * @param dataUrl 图片 data URL
     * @param userId  用户标识（仅用于调试日志）
     */
    public ImportBatch importImage(String dataUrl, String userId) {
        String visionResult = visionService.analyze(dataUrl, COURSE_IMAGE_PROMPT);
        if (visionResult == null || visionResult.isBlank()) {
            log.warn("课表导入：图片分析失败 | userId={}", userId);
            return new ImportBatch(List.of(), List.of("无法识别课表图片"));
        }
        log.info("课表导入：视觉分析完成 | userId={} | resultLen={}", userId, visionResult.length());
        // [调试] 将视觉模型原始输出写入文件，用于排查课表识别错位问题（定位后删除）
        writeVisionDebug(userId, visionResult);

        List<CourseEntity> courses = courseParser.parseFromJson(visionResult);
        return process(courses, SourceType.OCR);
    }

    // ==================== 重新识别 ====================

    /**
     * 按来源重跑解析管线（「重新识别」）
     *
     * @param source 原始来源
     * @param raw    原始输入：文本 / dataUrl / 文件字节的 Base64
     */
    public ImportBatch reprocess(SourceType source, String raw) {
        if (raw == null || raw.isBlank()) {
            return new ImportBatch(List.of(), List.of("缺少原始输入，无法重新识别"));
        }
        return switch (source) {
            case ZHENGFANG -> importZhengFangText(raw);
            case OCR -> importImage(raw, "reprocess");
            case PDF -> importPdf(base64Decode(raw));
            case EXCEL -> importExcel(base64Decode(raw));
            default -> new ImportBatch(List.of(), List.of("不支持该来源的重新识别: " + source));
        };
    }

    // ==================== PDF 单元格构建 ====================

    /** 构建包含正确 (星期, 节次) 的单元格文本供 LLM 提取 */
    private String buildCellsPromptText(List<ScheduleCell> cells) {
        StringBuilder sb = new StringBuilder();
        sb.append("共有 ").append(cells.size()).append(" 个课表单元格：\n\n");
        for (int i = 0; i < cells.size(); i++) {
            ScheduleCell cell = cells.get(i);
            sb.append("【单元格").append(i).append("】\n");
            sb.append("星期: ").append(cell.getDayOfWeek()).append("(").append(getDayName(cell.getDayOfWeek())).append(")\n");
            sb.append("节次: ").append(cell.getPeriod()).append("\n");
            sb.append("文本: ").append(cell.getContent()).append("\n\n");
        }
        return sb.toString();
    }

    /** 将 ScheduleCell 与 LLM 提取结果合并为课程列表（day/period 以单元格为准） */
    private List<CourseEntity> buildCoursesFromCells(List<ScheduleCell> cells, String llmResult) {
        List<CourseEntity> courses = new ArrayList<>();
        try {
            JsonNode results = objectMapper.readTree(llmResult);
            if (!results.isArray()) {
                results = results.get("courses");
                if (results == null || !results.isArray()) {
                    return courses;
                }
            }
            for (JsonNode item : results) {
                int cellIndex = item.path("cell_index").asInt(-1);
                if (cellIndex < 0 || cellIndex >= cells.size()) {
                    continue;
                }
                ScheduleCell cell = cells.get(cellIndex);
                String name = item.path("course_name").asText("");
                if (name.isBlank()) {
                    continue;
                }
                String teacher = item.path("teacher").asText("");
                String classroom = item.path("classroom").asText("");
                int startWeek = item.path("start_week").asInt(1);
                int endWeek = item.path("end_week").asInt(20);
                String weekType = item.path("week_type").asText("ALL");

                String[] periodParts = cell.getPeriod() != null ? cell.getPeriod().split("-") : new String[0];
                Integer dayOfWeek = cell.getDayOfWeek();
                Integer startPeriod = null;
                Integer endPeriod = null;
                if (periodParts.length > 0) {
                    try {
                        startPeriod = Integer.parseInt(periodParts[0]);
                        endPeriod = periodParts.length > 1 ? Integer.parseInt(periodParts[1]) : startPeriod;
                    } catch (NumberFormatException ignored) {
                    }
                }

                if (CourseEntity.WEEK_ALL.equals(weekType)) {
                    String detected = detectOddEvenFromContent(cell.getContent());
                    if (detected != null) {
                        weekType = detected;
                    }
                }

                courses.add(CourseEntity.create(null, name, teacher,
                        dayOfWeek, startPeriod, endPeriod,
                        classroom, startWeek, endWeek, weekType));
            }
        } catch (Exception e) {
            log.error("合并 ScheduleCell 与 LLM 结果失败", e);
        }
        return courses;
    }

    /** 从原始文本中检测单双周标记 */
    private String detectOddEvenFromContent(String content) {
        if (content == null) {
            return null;
        }
        if (content.contains("(双)") || content.contains("（双）") || content.contains("双周")) {
            return CourseEntity.WEEK_EVEN;
        }
        if (content.contains("(单)") || content.contains("（单）") || content.contains("单周")) {
            return CourseEntity.WEEK_ODD;
        }
        return null;
    }

    private String getDayName(Integer dayOfWeek) {
        String[] names = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        if (dayOfWeek == null) {
            return "未知";
        }
        return dayOfWeek >= 1 && dayOfWeek <= 7 ? names[dayOfWeek] : "周" + dayOfWeek;
    }

    // ==================== 工具 ====================

    /**
     * [调试] 将视觉模型原始输出写入 data/vision_debug/ 目录，便于排查识别错位问题。定位后删除。
     */
    private void writeVisionDebug(String userId, String result) {
        try {
            java.io.File dir = new java.io.File("data/vision_debug");
            if (!dir.exists() && !dir.mkdirs()) {
                log.warn("课表导入：调试目录创建失败");
                return;
            }
            String safeUser = userId.replaceAll("[^a-zA-Z0-9_-]", "_");
            String fileName = "data/vision_debug/vision_" + safeUser + "_" + System.currentTimeMillis() + ".json";
            java.nio.file.Files.writeString(java.nio.file.Path.of(fileName), result, StandardCharsets.UTF_8);
            log.info("课表导入：视觉原始输出已写入文件 | path={}", fileName);
        } catch (Exception e) {
            log.warn("课表导入：视觉原始输出写文件失败", e);
        }
    }

    private byte[] base64Decode(String raw) {
        try {
            return Base64.getDecoder().decode(raw);
        } catch (IllegalArgumentException e) {
            return new byte[0];
        }
    }
}
