package com.youkeda.exercise.claw.feature.schedule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.infrastructure.document.FileParseService;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.ai.vision.VisionService;
import com.youkeda.exercise.claw.agent.memory.ContextStore;
import com.youkeda.exercise.claw.feature.schedule.pdf.PdfTableExtractor;
import com.youkeda.exercise.claw.feature.schedule.pdf.ScheduleCell;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.client.WechatILinkClient;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.model.WechatReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 课表导入文件处理器
 *
 * <p>当用户处于 {@link CourseImportStateManager.Phase#WAITING_FILE} 状态时，
 * 处理用户上传的课表图片、Excel 或 PDF 文件。
 *
 * <p>处理后设置 {@link CourseImportStateManager.Phase#WAITING_CONFIRM} 状态，
 * 后续用户确认后由 {@link CourseImportFunction#handleConfirm} 保存入库。
 * 数据最终写入 SQLite {@code course_schedule} 表。
 */
@Component
public class CourseImportHandler {

    private static final Logger log = LoggerFactory.getLogger(CourseImportHandler.class);

    private static final String COURSE_PDF_CELL_PROMPT =
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

    private static final String COURSE_IMAGE_PROMPT =
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
            + "  只有确认每周都有课才填 ALL\n";

    private static final String COURSE_DOC_PROMPT =
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
            + "  只有确认每周都有课才填 ALL\n";

    private final WechatILinkClient wechatClient;
    private final VisionService visionService;
    private final CourseParser courseParser;
    private final CourseImportStateManager importStateManager;
    private final CourseRepository courseRepository;
    private final SemesterConfig semesterConfig;
    private final FileParseService fileParseService;
    private final LLMClient llmClient;
    private final ContextStore contextStore;
    private final ObjectMapper objectMapper;
    private final PdfTableExtractor pdfTableExtractor;
    private final SemesterDetector semesterDetector;
    private final SemesterService semesterService;

    public CourseImportHandler(WechatILinkClient wechatClient,
                               VisionService visionService,
                               CourseParser courseParser,
                               CourseImportStateManager importStateManager,
                               CourseRepository courseRepository,
                               SemesterConfig semesterConfig,
                               FileParseService fileParseService,
                               LLMClient llmClient,
                               ContextStore contextStore,
                               ObjectMapper objectMapper,
                               PdfTableExtractor pdfTableExtractor,
                               SemesterDetector semesterDetector,
                               SemesterService semesterService) {
        this.wechatClient = wechatClient;
        this.visionService = visionService;
        this.courseParser = courseParser;
        this.importStateManager = importStateManager;
        this.courseRepository = courseRepository;
        this.semesterConfig = semesterConfig;
        this.fileParseService = fileParseService;
        this.llmClient = llmClient;
        this.contextStore = contextStore;
        this.objectMapper = objectMapper;
        this.pdfTableExtractor = pdfTableExtractor;
        this.semesterDetector = semesterDetector;
        this.semesterService = semesterService;
    }

    // ==================== IMAGE 处理 ====================

    /**
     * 处理课表图片：视觉分析 → JSON 解析 → 预览
     */
    public WechatReply handleImage(WechatMessage message) {
        String userId = message.getUserId();
        log.info("课表导入：处理图片 | userId={}", userId);

        if (importStateManager.getPhase(userId) != CourseImportStateManager.Phase.WAITING_FILE) {
            log.warn("用户未处于课表导入状态，跳过图片处理 | userId={}", userId);
            return null;
        }

        String imageDataUrl = downloadImageAsDataUrl(message);
        if (imageDataUrl == null) {
            log.warn("课表导入：图片下载失败 | userId={}", userId);
            return WechatReply.text("图片下载失败，请重新发送。");
        }

        String visionResult = visionService.analyze(imageDataUrl, COURSE_IMAGE_PROMPT);
        if (visionResult == null || visionResult.isBlank()) {
            log.warn("课表导入：图片分析失败 | userId={}", userId);
            return WechatReply.text("无法识别课表图片，请确认图片清晰包含课程信息，或尝试发送 Excel 文件。");
        }

        log.info("课表导入：视觉分析完成 | userId={} | resultLen={}", userId, visionResult.length());

        List<CourseEntity> courses = courseParser.parseFromJson(visionResult);
        if (courses.isEmpty()) {
            log.warn("课表导入：视觉结果无法解析为课程 | userId={}", userId);
            return WechatReply.text("从图片中未能识别出有效的课程信息，请确认图片为课表截图，或尝试发送 Excel 文件。");
        }

        importStateManager.setPendingCourses(userId, courses);

        // 学期检测（从文件名）
        detectAndStoreSemester(userId, null, null);
        importStateManager.setWaitingConfirm(userId, visionResult);

        contextStore.append("user", "[课表导入图片解析完成] 共 " + courses.size() + " 门课程");
        contextStore.append("assistant", buildPreviewText(userId, courses));

        return WechatReply.text(buildPreview(userId, courses));
    }

    // ==================== FILE 处理 ====================

    public WechatReply handleFile(WechatMessage message) {
        String userId = message.getUserId();
        String fileName = message.getFileName() != null ? message.getFileName() : "未知文件";

        log.info("课表导入：处理文件 | userId={} | fileName={}", userId, fileName);

        if (importStateManager.getPhase(userId) != CourseImportStateManager.Phase.WAITING_FILE) {
            log.warn("用户未处于课表导入状态，跳过文件处理 | userId={}", userId);
            return null;
        }

        byte[] fileBytes = downloadFile(message);
        if (fileBytes == null || fileBytes.length == 0) {
            log.warn("课表导入：文件下载失败 | userId={} | fileName={}", userId, fileName);
            return WechatReply.text("文件下载失败，请重新发送。");
        }

        String mimeType = fileParseService.detectMimeType(fileBytes);
        log.info("课表导入：文件类型检测 | fileName={} | mimeType={}", fileName, mimeType);

        if (isPdfFile(fileName, mimeType)) {
            return handlePdfFile(userId, fileBytes, fileName);
        } else if (isExcelFile(fileName, mimeType)) {
            return handleExcelFile(userId, fileBytes, fileName);
        } else if (isImageFile(mimeType)) {
            return handleImageFile(userId, fileBytes, mimeType, fileName);
        } else {
            return handleDocumentFile(userId, fileBytes, fileName);
        }
    }

    private WechatReply handleExcelFile(String userId, byte[] fileBytes, String fileName) {
        log.info("课表导入：Excel 文件 | userId={} | fileName={}", userId, fileName);

        List<CourseEntity> courses = courseParser.parseFromExcel(fileBytes);
        if (courses.isEmpty()) {
            log.warn("课表导入：Excel 解析为空 | userId={}", userId);
            return WechatReply.text("未能从 Excel 文件中识别出课程信息，请确认文件格式正确。"
                    + "支持标准表头格式（课程名称/星期/节次）或课表矩阵格式。");
        }

        importStateManager.setPendingCourses(userId, courses);

        // 学期检测（从文件名）
        detectAndStoreSemester(userId, fileName, null);
        importStateManager.setWaitingConfirm(userId, "[Excel 解析] " + fileName);

        contextStore.append("user", "[课表导入 Excel 解析完成] " + fileName + "，共 " + courses.size() + " 门课程");
        contextStore.append("assistant", buildPreviewText(userId, courses));

        return WechatReply.text(buildPreview(userId, courses));
    }

    private WechatReply handleImageFile(String userId, byte[] fileBytes, String mimeType, String fileName) {
        log.info("课表导入：图片文件（FILE 类型）| userId={} | fileName={}", userId, fileName);

        String base64 = Base64.getEncoder().encodeToString(fileBytes);
        String dataUrl = "data:" + mimeType + ";base64," + base64;

        String visionResult = visionService.analyze(dataUrl, COURSE_IMAGE_PROMPT);
        if (visionResult == null || visionResult.isBlank()) {
            return WechatReply.text("无法识别课表图片，请确认图片清晰包含课程信息。");
        }

        List<CourseEntity> courses = courseParser.parseFromJson(visionResult);
        if (courses.isEmpty()) {
            return WechatReply.text("从图片中未能识别出有效的课程信息。");
        }

        importStateManager.setPendingCourses(userId, courses);

        // 学期检测（从文件名）
        detectAndStoreSemester(userId, fileName, null);
        importStateManager.setWaitingConfirm(userId, visionResult);

        contextStore.append("user", "[课表导入图片解析完成] " + fileName + "，共 " + courses.size() + " 门课程");
        contextStore.append("assistant", buildPreviewText(userId, courses));

        return WechatReply.text(buildPreview(userId, courses));
    }

    private WechatReply handleDocumentFile(String userId, byte[] fileBytes, String fileName) {
        log.info("课表导入：文档文件 | userId={} | fileName={}", userId, fileName);

        FileParseService.FileParseResult result = fileParseService.parse(fileBytes, fileName);
        if (result == null || result.text() == null || result.text().isBlank()) {
            log.warn("课表导入：文档文本提取失败 | userId={}", userId);
            return WechatReply.text("无法读取文件内容，请确认文件为 PDF/Excel 格式的课表。");
        }

        log.info("课表导入：文档文本提取完成 | userId={} | textLen={}", userId, result.text().length());

        String docContent = "文件名：" + fileName + "\n\n文件内容：\n" + result.text();
        String llmResult = llmClient.chatWithSystemPrompt(COURSE_DOC_PROMPT, docContent);
        if (llmResult == null || llmResult.isBlank()) {
            log.warn("课表导入：LLM 提取课程信息失败 | userId={}", userId);
            return WechatReply.text("无法从文档中提取课程信息，请尝试发送 Excel 文件或清晰的课表截图。");
        }

        log.info("课表导入：LLM 提取完成 | userId={} | resultLen={}", userId, llmResult.length());

        List<CourseEntity> courses = courseParser.parseFromJson(llmResult);
        if (courses.isEmpty()) {
            log.warn("课表导入：LLM 结果无法解析为课程 | userId={}", userId);
            return WechatReply.text("未能从文档中识别出有效的课程信息。");
        }

        importStateManager.setPendingCourses(userId, courses);

        // 学期检测（从文件名和内容）
        String contentPreview = result != null ? result.text() : null;
        detectAndStoreSemester(userId, fileName, contentPreview);
        importStateManager.setWaitingConfirm(userId, llmResult);

        contextStore.append("user", "[课表导入文档解析完成] " + fileName + "，共 " + courses.size() + " 门课程");
        contextStore.append("assistant", buildPreviewText(userId, courses));

        return WechatReply.text(buildPreview(userId, courses));
    }

    // ==================== PDF 处理（PdfTableExtractor + LLM） ====================

    private WechatReply handlePdfFile(String userId, byte[] fileBytes, String fileName) {
        log.info("课表导入：PDF 文件（PdfTableExtractor）| userId={} | fileName={}", userId, fileName);

        // 1. PdfTableExtractor 恢复表格结构，获取正确 dayOfWeek + period
        List<ScheduleCell> cells = pdfTableExtractor.extract(fileBytes);
        if (cells.isEmpty()) {
            log.warn("课表导入：PDF 表格恢复为空 | userId={}", userId);
            return handleDocumentFile(userId, fileBytes, fileName);
        }
        log.info("课表导入：PDF 表格恢复完成 | userId={} | cells={}", userId, cells.size());

        // 2. 构建结构化的 LLM Prompt（包含正确的 dayOfWeek 和 period）
        StringBuilder cellText = new StringBuilder();
        cellText.append("共有 ").append(cells.size()).append(" 个课表单元格：\n\n");
        for (int i = 0; i < cells.size(); i++) {
            ScheduleCell cell = cells.get(i);
            String dayName = getDayName(cell.getDayOfWeek());
            cellText.append("【单元格").append(i).append("】\n");
            cellText.append("星期: ").append(cell.getDayOfWeek()).append("(").append(dayName).append(")\n");
            cellText.append("节次: ").append(cell.getPeriod()).append("\n");
            cellText.append("文本: ").append(cell.getContent()).append("\n\n");
        }

        String llmResult = llmClient.chatWithSystemPrompt(COURSE_PDF_CELL_PROMPT, cellText.toString());
        if (llmResult == null || llmResult.isBlank()) {
            log.warn("课表导入：LLM 提取失败 | userId={}", userId);
            return WechatReply.text("无法从PDF中提取课程信息，请尝试发送清晰的课表截图。");
        }

        log.info("课表导入：LLM 提取完成 | userId={} | resultLen={}", userId, llmResult.length());

        // 3. 用 ScheduleCell 的 dayOfWeek 和 period 覆盖 LLM 结果
        List<CourseEntity> courses = buildCoursesFromCells(cells, llmResult);
        if (courses.isEmpty()) {
            log.warn("课表导入：构建课程列表为空 | userId={}", userId);
            return WechatReply.text("未能从PDF中识别出有效的课程信息。");
        }

        importStateManager.setPendingCourses(userId, courses);

        // 学期检测（从文件名）
        detectAndStoreSemester(userId, fileName, null);
        importStateManager.setWaitingConfirm(userId, "[PDF 解析] " + fileName);

        contextStore.append("user", "[课表导入 PDF 解析完成] " + fileName + "，共 " + courses.size() + " 门课程");
        contextStore.append("assistant", buildPreviewText(userId, courses));

        return WechatReply.text(buildPreview(userId, courses));
    }

    /**
     * 将 ScheduleCell 和 LLM 提取结果合并为 CourseEntity 列表
     * <p>dayOfWeek 和 period 以 ScheduleCell 为准，覆盖 LLM 返回值。</p>
     */
    private List<CourseEntity> buildCoursesFromCells(List<ScheduleCell> cells, String llmResult) {
        List<CourseEntity> courses = new ArrayList<>();
        try {
            JsonNode results = objectMapper.readTree(llmResult);
            if (!results.isArray()) {
                // 尝试解析 {courses:[...]} 格式
                results = results.get("courses");
                if (results == null || !results.isArray()) return courses;
            }

            for (JsonNode item : results) {
                int cellIndex = item.path("cell_index").asInt(-1);
                if (cellIndex < 0 || cellIndex >= cells.size()) continue;

                ScheduleCell cell = cells.get(cellIndex);
                String name = item.path("course_name").asText("");
                if (name.isBlank()) continue;

                String teacher = item.path("teacher").asText("");
                String classroom = item.path("classroom").asText("");
                int startWeek = item.path("start_week").asInt(1);
                int endWeek = item.path("end_week").asInt(20);
                String weekType = item.path("week_type").asText("ALL");

                // 从 cell 获取正确的 dayOfWeek 和 period
                String[] periodParts = cell.getPeriod().split("-");
                int startPeriod = Integer.parseInt(periodParts[0]);
                int endPeriod = periodParts.length > 1 ? Integer.parseInt(periodParts[1]) : startPeriod;

                // 单双周兜底检测（复用 CourseParser 的逻辑）
                if ("ALL".equals(weekType)) {
                    String detected = detectOddEvenFromContent(cell.getContent());
                    if (detected != null) weekType = detected;
                }

                courses.add(new CourseEntity(null, name, teacher,
                        cell.getDayOfWeek(), startPeriod, endPeriod,
                        classroom, startWeek, endWeek, weekType));
            }
        } catch (Exception e) {
            log.error("合并 ScheduleCell 与 LLM 结果失败", e);
        }
        return courses;
    }

    /**
     * 从原始文本中检测单双周标记
     */
    private String detectOddEvenFromContent(String content) {
        if (content == null) return null;
        if (content.contains("(双)") || content.contains("（双）") || content.contains("双周")) {
            return "EVEN";
        }
        if (content.contains("(单)") || content.contains("（单）") || content.contains("单周")) {
            return "ODD";
        }
        return null;
    }

    private String getDayName(int dayOfWeek) {
        String[] names = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return dayOfWeek >= 1 && dayOfWeek <= 7 ? names[dayOfWeek] : "周" + dayOfWeek;
    }

    // ==================== 预览构建 ====================

    private String buildPreview(String userId, List<CourseEntity> courses) {
        int currentWeek = resolveCurrentWeek(userId);
        String weekInfo = currentWeek > 0 ? "（当前第 " + currentWeek + " 周）" : "";

        // 获取学期信息
        SemesterEntity pendingSemester = importStateManager.getPendingSemester(userId);
        String semesterInfo = "";
        if (pendingSemester != null) {
            semesterInfo = "【" + pendingSemester.getDisplayName() + "】\n"
                    + "第1周：" + pendingSemester.getStartDateDisplay() + "\n\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📋 已识别出以下 ").append(courses.size()).append(" 门课程").append(weekInfo).append("：\n\n");
        if (!semesterInfo.isEmpty()) {
            sb.append(semesterInfo);
        }

        for (int i = 0; i < courses.size(); i++) {
            CourseEntity c = courses.get(i);
            sb.append(i + 1).append(". ");
            sb.append("【").append(c.getCourseName()).append("】");
            sb.append(c.getDayDisplay()).append(" ");
            sb.append(c.getPeriodDisplay()).append("节");
            if (!c.getClassroom().isBlank()) sb.append(" ").append(c.getClassroom());
            if (!c.getTeacher().isBlank()) sb.append(" ").append(c.getTeacher());
            sb.append(" (").append(c.getWeekDisplay()).append(")");
            sb.append("\n");
        }

        sb.append("\n✅ 回复「确认」保存课表，回复「取消」丢弃。");
        sb.append("\n💡 也可以告诉我需要修改的地方，如「把高数改成周一1-2节」。");
        return sb.toString();
    }

    /**
     * 解析用户当前教学周
     *
     * <p>优先使用用户自身的 {@link SemesterService#getCurrentWeek(String)} 计算结果；
     * 无学期记录时回退 {@link SemesterConfig#getCurrentWeek()}。
     */
    private int resolveCurrentWeek(String userId) {
        int week = semesterService.getCurrentWeek(userId);
        if (week > 0) {
            return week;
        }
        return semesterConfig.getCurrentWeek();
    }

    private String buildPreviewText(String userId, List<CourseEntity> courses) {
        return "已识别 " + courses.size() + " 门课程等待确认导入："
                + courses.stream().map(CourseEntity::getCourseName).reduce((a, b) -> a + "、" + b).orElse("");
    }

    /**
     * 从文件名和内容检测学期并存储到状态管理器
     *
     * @param userId         用户标识
     * @param fileName       文件名
     * @param contentPreview 文件内容预览（可为 null）
     */
    private void detectAndStoreSemester(String userId, String fileName, String contentPreview) {
        SemesterEntity detected = semesterDetector.detectFromFile(userId, fileName, contentPreview);
        if (detected == null) {
            // 无法从文件信息检测，使用自动推算作为 fallback
            detected = semesterDetector.detectAuto(userId);
            log.info("自动推算学期作为 fallback | userId={} | display={}", userId, detected.getDisplayName());
        }
        importStateManager.setPendingSemester(userId, detected);
    }

    // ==================== 文件下载 ====================

    private String downloadImageAsDataUrl(WechatMessage message) {
        String encryptParam = message.getEncryptQueryParam();
        String aesKey = message.getAesKey();

        if (encryptParam != null && !encryptParam.isEmpty()
                && aesKey != null && !aesKey.isEmpty()) {
            byte[] bytes = wechatClient.downloadMedia(encryptParam, aesKey);
            if (bytes != null && bytes.length > 0) {
                return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes);
            }
        }

        if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
            return message.getImageUrl();
        }

        return null;
    }

    private byte[] downloadFile(WechatMessage message) {
        String encryptParam = message.getFileEncryptQueryParam();
        String aesKey = message.getFileAesKey();

        if (encryptParam == null || encryptParam.isEmpty()
                || aesKey == null || aesKey.isEmpty()) {
            log.warn("课表导入：文件 CDN 参数不完整 | userId={}", message.getUserId());
            return null;
        }

        return wechatClient.downloadMedia(encryptParam, aesKey);
    }

    // ==================== 类型检测 ====================

    private boolean isExcelFile(String fileName, String mimeType) {
        if (mimeType != null && (mimeType.contains("spreadsheet")
                || mimeType.contains("excel")
                || mimeType.contains("xls"))) {
            return true;
        }
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            return lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(".csv");
        }
        return false;
    }

    private boolean isPdfFile(String fileName, String mimeType) {
        if (mimeType != null && (mimeType.contains("pdf") || mimeType.contains("application"))) {
            return true;
        }
        if (fileName != null) {
            return fileName.toLowerCase().endsWith(".pdf");
        }
        return false;
    }

    private boolean isImageFile(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }
}