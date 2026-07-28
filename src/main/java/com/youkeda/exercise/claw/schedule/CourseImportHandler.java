package com.youkeda.exercise.claw.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.ai.file.FileParseService;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.ai.vision.VisionService;
import com.youkeda.exercise.claw.agent.memory.ContextStore;
import com.youkeda.exercise.claw.wechat.client.WechatILinkClient;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
            + "- day_of_week: 星期几，1=周一~7=周日（必填）\n"
            + "- start_period: 开始节次，从1开始（必填）\n"
            + "- end_period: 结束节次（必填）\n"
            + "- classroom: 教室/地点（可选）\n"
            + "- start_week: 开始周，默认1\n"
            + "- end_week: 结束周，默认20\n"
            + "- week_type: ALL=全部周, ODD=单周, EVEN=双周（默认ALL）\n"
            + "请确保 day_of_week 用数字表示，不要用中文。";

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
            + "字段同上说明。";

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

    public CourseImportHandler(WechatILinkClient wechatClient,
                               VisionService visionService,
                               CourseParser courseParser,
                               CourseImportStateManager importStateManager,
                               CourseRepository courseRepository,
                               SemesterConfig semesterConfig,
                               FileParseService fileParseService,
                               LLMClient llmClient,
                               ContextStore contextStore,
                               ObjectMapper objectMapper) {
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

        if (isExcelFile(fileName, mimeType)) {
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
        importStateManager.setWaitingConfirm(userId, llmResult);

        contextStore.append("user", "[课表导入文档解析完成] " + fileName + "，共 " + courses.size() + " 门课程");
        contextStore.append("assistant", buildPreviewText(userId, courses));

        return WechatReply.text(buildPreview(userId, courses));
    }

    // ==================== 预览构建 ====================

    private String buildPreview(String userId, List<CourseEntity> courses) {
        int currentWeek = semesterConfig.getCurrentWeek();
        String weekInfo = currentWeek > 0 ? "（当前第 " + currentWeek + " 周）" : "";

        StringBuilder sb = new StringBuilder();
        sb.append("📋 已识别出以下 ").append(courses.size()).append(" 门课程").append(weekInfo).append("：\n\n");

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

    private String buildPreviewText(String userId, List<CourseEntity> courses) {
        return "已识别 " + courses.size() + " 门课程等待确认导入："
                + courses.stream().map(CourseEntity::getCourseName).reduce((a, b) -> a + "、" + b).orElse("");
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

    private boolean isImageFile(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }
}