package com.youkeda.exercise.claw.feature.schedule;

import com.youkeda.exercise.claw.feature.schedule.imports.CourseImportPipeline;
import com.youkeda.exercise.claw.feature.schedule.imports.CourseImportPipeline.ImportBatch;
import com.youkeda.exercise.claw.feature.schedule.imports.SourceType;
import com.youkeda.exercise.claw.agent.memory.ContextStore;
import com.youkeda.exercise.claw.infrastructure.document.FileParseService;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.client.WechatILinkClient;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.model.WechatReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;

/**
 * 课表导入文件处理器
 *
 * <p>当用户处于 {@link CourseImportStateManager.Phase#WAITING_FILE} 状态时，
 * 处理用户上传的课表图片、Excel、PDF 或粘贴的正方课表文本。
 *
 * <p>所有数据源统一交给 {@link CourseImportPipeline} 解析并校验（规范化/合并/冲突检测），
 * 解析结果存入状态机等待确认，确认后由 {@link CourseImportTool#confirm} 保存入库
 * （SQLite {@code course_schedule} 表）。
 */
@Component
public class CourseImportHandler {

    private static final Logger log = LoggerFactory.getLogger(CourseImportHandler.class);

    private final WechatILinkClient wechatClient;
    private final CourseImportPipeline importPipeline;
    private final CourseImportStateManager importStateManager;
    private final SemesterConfig semesterConfig;
    private final FileParseService fileParseService;
    private final ContextStore contextStore;
    private final SemesterDetector semesterDetector;
    private final SemesterService semesterService;

    public CourseImportHandler(WechatILinkClient wechatClient,
                               CourseImportPipeline importPipeline,
                               CourseImportStateManager importStateManager,
                               SemesterConfig semesterConfig,
                               FileParseService fileParseService,
                               ContextStore contextStore,
                               SemesterDetector semesterDetector,
                               SemesterService semesterService) {
        this.wechatClient = wechatClient;
        this.importPipeline = importPipeline;
        this.importStateManager = importStateManager;
        this.semesterConfig = semesterConfig;
        this.fileParseService = fileParseService;
        this.contextStore = contextStore;
        this.semesterDetector = semesterDetector;
        this.semesterService = semesterService;
    }

    // ==================== IMAGE 处理 ====================

    /**
     * 处理课表图片：视觉模型识别 → 校验 → 预览
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

        ImportBatch batch = importPipeline.importImage(imageDataUrl, userId);
        return processImport(userId, batch, imageDataUrl, SourceType.OCR,
                "图片识别", null, null);
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

    // ==================== TEXT 处理（正方粘贴文本） ====================

    /**
     * 处理粘贴的正方教务课表文本（MessageRouter 在 WAITING_FILE 状态启发式路由）
     */
    public WechatReply handleText(WechatMessage message) {
        String userId = message.getUserId();
        if (importStateManager.getPhase(userId) != CourseImportStateManager.Phase.WAITING_FILE) {
            log.warn("用户未处于课表导入状态，跳过文本处理 | userId={}", userId);
            return null;
        }
        String text = message.getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        log.info("课表导入：处理粘贴文本 | userId={} | textLen={}", userId, text.length());
        ImportBatch batch = importPipeline.importZhengFangText(text);
        return processImport(userId, batch, text, SourceType.ZHENGFANG,
                "正方文本解析", null, null);
    }

    // ==================== 各来源处理 ====================

    private WechatReply handleExcelFile(String userId, byte[] fileBytes, String fileName) {
        log.info("课表导入：Excel 文件 | userId={} | fileName={}", userId, fileName);
        ImportBatch batch = importPipeline.importExcel(fileBytes);
        return processImport(userId, batch, Base64.getEncoder().encodeToString(fileBytes),
                SourceType.EXCEL, "[Excel 解析] " + fileName, fileName, null);
    }

    private WechatReply handleImageFile(String userId, byte[] fileBytes, String mimeType, String fileName) {
        log.info("课表导入：图片文件（FILE 类型）| userId={} | fileName={}", userId, fileName);
        String base64 = Base64.getEncoder().encodeToString(fileBytes);
        String dataUrl = "data:" + mimeType + ";base64," + base64;
        ImportBatch batch = importPipeline.importImage(dataUrl, userId);
        return processImport(userId, batch, dataUrl, SourceType.OCR,
                "图片识别", fileName, null);
    }

    private WechatReply handlePdfFile(String userId, byte[] fileBytes, String fileName) {
        log.info("课表导入：PDF 文件（PdfTableExtractor + LLM）| userId={} | fileName={}", userId, fileName);
        ImportBatch batch = importPipeline.importPdf(fileBytes);
        if (batch.isEmpty()) {
            log.warn("课表导入：PDF 解析为空，回退文档解析 | userId={}", userId);
            return handleDocumentFile(userId, fileBytes, fileName);
        }
        return processImport(userId, batch, Base64.getEncoder().encodeToString(fileBytes),
                SourceType.PDF, "[PDF 解析] " + fileName, fileName, null);
    }

    private WechatReply handleDocumentFile(String userId, byte[] fileBytes, String fileName) {
        log.info("课表导入：文档文件 | userId={} | fileName={}", userId, fileName);

        FileParseService.FileParseResult result = fileParseService.parse(fileBytes, fileName);
        if (result == null || result.text() == null || result.text().isBlank()) {
            log.warn("课表导入：文档文本提取失败 | userId={}", userId);
            return WechatReply.text("无法读取文件内容，请确认文件为 PDF/Excel 格式的课表。");
        }
        log.info("课表导入：文档文本提取完成 | userId={} | textLen={}", userId, result.text().length());

        ImportBatch batch = importPipeline.importDocText(result.text(), fileName);
        return processImport(userId, batch, result.text(), SourceType.DOC,
                "[文档解析] " + fileName, fileName, result.text());
    }

    /**
     * 统一处理导入批次：空则报错，否则暂存待确认并返回预览
     */
    private WechatReply processImport(String userId, ImportBatch batch, String rawInput,
                                      SourceType source, String phaseLabel,
                                      String fileName, String contentPreview) {
        if (batch == null || batch.isEmpty()) {
            log.warn("课表导入：解析为空 | userId={} | source={}", userId, source);
            String detail = (batch != null && batch.warnings() != null && !batch.warnings().isEmpty())
                    ? String.join("；", batch.warnings())
                    : "未能识别出有效的课程信息";
            return WechatReply.text(detail + "，请确认内容为课表，或尝试发送 Excel 文件。");
        }

        importStateManager.setPendingCourses(userId, batch.courses());
        importStateManager.setPendingRawInput(userId, rawInput);
        importStateManager.setPendingSource(userId, source.code());

        // 学期检测（从文件名和内容）
        detectAndStoreSemester(userId, fileName, contentPreview);
        importStateManager.setWaitingConfirm(userId, phaseLabel);

        contextStore.append("user", "[课表导入" + source.getDisplayName() + "解析完成] 共 " + batch.courses().size() + " 门课程");
        contextStore.append("assistant", buildPreviewText(userId, batch.courses()));

        return WechatReply.text(buildPreview(userId, batch));
    }

    // ==================== 预览构建 ====================

    private String buildPreview(String userId, ImportBatch batch) {
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
        sb.append("📋 已识别出以下 ").append(batch.courses().size()).append(" 门课程").append(weekInfo).append("：\n\n");
        if (!semesterInfo.isEmpty()) {
            sb.append(semesterInfo);
        }

        List<CourseEntity> courses = batch.courses();
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

        if (batch.warnings() != null && !batch.warnings().isEmpty()) {
            sb.append("\n⚠️ 校验提示 ").append(batch.warnings().size()).append(" 条：\n");
            for (String w : batch.warnings()) {
                sb.append("  • ").append(w).append("\n");
            }
        }

        sb.append("\n✅ 回复「确认保存」保存课表");
        sb.append("\n🔄 回复「重新识别」重新识别");
        sb.append("\n✏️ 也可以告诉我需要修改的地方，如「把高数改成周一1-2节」。");
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
