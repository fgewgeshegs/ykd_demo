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
 * 后续用户确认后由 {@link CourseImportTool#handleConfirm} 保存入库。
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
            "你是一位课表结构解析器。请从这张课表图片中提取所有课程信息。\n"
            + "课表本质是【星期 × 节次】的二维网格，每个格子最多容纳一门课。\n"
            + "\n"
            + "【工作流程】\n"
            + "1. 先在脑内重建完整网格：最左列是节次（第1节、第2节...），最上一行是星期。\n"
            + "2. 逐格（每星期 × 每节次）判定该格是否有课，然后输出。\n"
            + "3. 严格按图片里网格的结构走，禁止按文字出现的先后顺序流水账式输出。\n"
            + "\n"
            + "【铁律】\n"
            + "1. 空白格子不输出任何课程。禁止把已识别的课程复制、臆造、补到其他格子。\n"
            + "2. 禁止在空白格/其他课所在的格子里凭空造一门课（如把「高等数学」复制到周三1-2）。\n"
            + "3. 输出完成后自检：若两个对象占用相同的(day_of_week, start_period, end_period)，\n"
            + "   则必然有一处是错的，删除多余的那个，只保留图片中真实存在的一门。\n"
            + "4. 一门课若跨连续多节（如第3-5节），start_period=3、end_period=5，禁止截断成3-4。\n"
            + "5. 单元格内形如「周一周三4,5节(第1-17周)(单周)」的文字是排课信息，必须解析进\n"
            + "   day_of_week / start_period / end_period / start_week / end_week / week_type 字段，\n"
            + "   禁止整串塞入 classroom 或 teacher。\n"
            + "6. 节次以课表最左列「第N节」表头为准，禁止默认从1递增。\n"
            + "   例：最左列表头是「第10节」，则 start_period=10，禁止写成11。\n"
            + "\n"
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

    private static final String COURSE_VERIFY_PROMPT =
            "你是课表校验器。下面是「第一轮」从同一张课表图片中提取出的课程清单，"
            + "请逐条与图片核对，找出清单与图片不符之处，只报告差异。\n"
            + "\n"
            + "【核对规则】（以图片为准）\n"
            + "1. 图片左列「第N节」表头决定真实节次范围；列标题决定星期。\n"
            + "2. 清单里出现在空白格子中的课程必须加入 deletions（第一轮常把空白格补成幻觉课程）。\n"
            + "3. 跨多行合并单元格（如第10-12节一整块）的课程，start_period/end_period 必须覆盖整块，"
            + "禁止偏移或只取其中一段。\n"
            + "4. 星期/周次/单双周不对的，用 corrections 改正。\n"
            + "5. 图片中存在但清单里没有的课程，加入 additions。\n"
            + "6. 核对无误的条目不要出现在任何数组里；全部无误则只返回 {\"ok\":true}。\n"
            + "\n"
            + "请严格返回如下 JSON 对象，不要添加任何说明文字：\n"
            + "{\n"
            + "  \"deletions\": [清单索引...],\n"
            + "  \"corrections\": [\n"
            + "    {\"index\": 清单索引, \"course_name\":\"...\", \"teacher\":\"...\", \"day_of_week\":n,"
            + " \"start_period\":n, \"end_period\":n, \"classroom\":\"...\", \"start_week\":n, \"end_week\":n,"
            + " \"week_type\":\"ALL|ODD|EVEN\"}\n"
            + "  ],\n"
            + "  \"additions\": [\n"
            + "    {\"course_name\":\"...\", \"teacher\":\"...\", \"day_of_week\":n,"
            + " \"start_period\":n, \"end_period\":n, \"classroom\":\"...\", \"start_week\":n, \"end_week\":n,"
            + " \"week_type\":\"ALL|ODD|EVEN\"}\n"
            + "  ]\n"
            + "}\n"
            + "字段说明：\n"
            + "- corrections 是覆盖式，只列出需要改的字段即可，其余保持原值。\n"
            + "- day_of_week: 星期一=1, 星期二=2, ..., 星期日=7。\n"
            + "- week_type: 图片中出现(单)、单周填 ODD；(双)、双周填 EVEN；每周都有才填 ALL。\n";

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
    private final SemesterProperties semesterConfig;
    private final FileParseService fileParseService;
    private final LLMClient llmClient;
    private final ContextStore contextStore;
    private final ObjectMapper objectMapper;
    private final PdfTableExtractor pdfTableExtractor;
    private final SemesterDetector semesterDetector;
    private final SemesterService semesterService;
    private final CourseMessageFormatter messageFormatter;

    public CourseImportHandler(WechatILinkClient wechatClient,
                               VisionService visionService,
                               CourseParser courseParser,
                               CourseImportStateManager importStateManager,
                               CourseRepository courseRepository,
                               SemesterProperties semesterConfig,
                               FileParseService fileParseService,
                               LLMClient llmClient,
                               ContextStore contextStore,
                               ObjectMapper objectMapper,
                               PdfTableExtractor pdfTableExtractor,
                               SemesterDetector semesterDetector,
                               SemesterService semesterService,
                               CourseMessageFormatter messageFormatter) {
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
        this.messageFormatter = messageFormatter;
    }

    // ==================== IMAGE 处理 ====================

    /**
     * 处理课表图片：视觉分析 → JSON 解析 → 预览
     */
    public WechatReply handleImage(WechatMessage message) {
        String userId = message.getUserId();
        log.info("课表导入：处理图片 | userId={}", userId);

        CourseImportStateManager.Phase phase = importStateManager.getPhase(userId);
        if (phase == CourseImportStateManager.Phase.NONE) {
            log.warn("用户未处于课表导入状态，跳过图片处理 | userId={}", userId);
            return null;
        }
        // 导入中途（等学期确认/等确认）收到新图片 = 重新开始导入，重置为等文件
        if (phase != CourseImportStateManager.Phase.WAITING_FILE) {
            log.info("导入中途收到新图片，重置为等待文件 | userId={} | phase={}", userId, phase);
            importStateManager.setWaitingFile(userId);
        }

        String imageDataUrl = downloadImageAsDataUrl(message);
        if (imageDataUrl == null) {
            log.warn("课表导入：图片下载失败 | userId={}", userId);
            return WechatReply.text("图片下载失败，请重新发送。");
        }

        // 图片内容处理与 handleImageFile 共用（批次 3 去重）
        return processImageContent(userId, imageDataUrl, null);
    }

    // ==================== FILE 处理 ====================

    public WechatReply handleFile(WechatMessage message) {
        String userId = message.getUserId();
        String fileName = message.getFileName() != null ? message.getFileName() : "未知文件";

        log.info("课表导入：处理文件 | userId={} | fileName={}", userId, fileName);

        CourseImportStateManager.Phase phase = importStateManager.getPhase(userId);
        if (phase == CourseImportStateManager.Phase.NONE) {
            log.warn("用户未处于课表导入状态，跳过文件处理 | userId={}", userId);
            return null;
        }
        // 导入中途收到新文件 = 重新开始导入，重置为等文件
        if (phase != CourseImportStateManager.Phase.WAITING_FILE) {
            log.info("导入中途收到新文件，重置为等待文件 | userId={} | phase={}", userId, phase);
            importStateManager.setWaitingFile(userId);
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

        return finalizePendingImport(userId, courses, "[Excel 解析] " + fileName, fileName, null,
                "[课表导入 Excel 解析完成] " + fileName);
    }

    private WechatReply handleImageFile(String userId, byte[] fileBytes, String mimeType, String fileName) {
        log.info("课表导入：图片文件（FILE 类型）| userId={} | fileName={}", userId, fileName);

        String base64 = Base64.getEncoder().encodeToString(fileBytes);
        String dataUrl = "data:" + mimeType + ";base64," + base64;

        // 图片内容处理与 handleImage 共用（批次 3 去重）
        return processImageContent(userId, dataUrl, fileName);
    }

    /**
     * 图片课表内容处理（视觉分析 → JSON 解析 → 两轮校验 → 暂存 → 预览）。
     *
     * <p>批次 3：抽取自 handleImage / handleImageFile 的重复流水线。二者仅图片来源不同
     * （CDN 下载 vs 文件字节 base64），解析/暂存/预览完全一致。fileName 为空表示纯 IMAGE
     * 消息（无文件名），日志与学期检测据此省略文件名。
     */
    private WechatReply processImageContent(String userId, String dataUrl, String fileName) {
        boolean hasFileName = fileName != null && !fileName.isBlank();

        String visionResult = visionService.analyze(dataUrl, COURSE_IMAGE_PROMPT);
        if (visionResult == null || visionResult.isBlank()) {
            log.warn("课表导入：图片分析失败 | userId={}", userId);
            return WechatReply.text("无法识别课表图片，请确认图片清晰包含课程信息，或尝试发送 Excel 文件。");
        }

        log.info("课表导入：视觉分析完成 | userId={} | resultLen={} | raw={}",
                userId, visionResult.length(), visionResult);

        List<CourseEntity> courses = courseParser.parseFromJson(visionResult);
        if (courses.isEmpty()) {
            log.warn("课表导入：视觉结果无法解析为课程 | userId={}", userId);
            return WechatReply.text("从图片中未能识别出有效的课程信息，请确认图片为课表截图，或尝试发送 Excel 文件。");
        }

        // 方案5 第二轮校验：第一轮结果整理成清单，连同原图让模型逐条「挑错」
        courses = verifyCoursesWithImage(dataUrl, courses, userId);

        String logText = hasFileName
                ? "[课表导入图片解析完成] " + fileName
                : "[课表导入图片解析完成]";
        return finalizePendingImport(userId, courses, visionResult, hasFileName ? fileName : null, null, logText);
    }

    // ==================== 方案5：两轮校验（提取 → 对账） ====================

    /**
     * 第二轮校验：把第一轮提取的课程整理成清单，连同原图再喂视觉模型逐条「挑错」。
     *
     * <p>第一轮开放提取容易出错（空白格幻觉补全、合并单元格节次偏移/截断、漏课）。
     * 校验轮是「给具体条目对照图片挑错」任务，成功率远高于从零重数一遍。
     * 校验采用 temperature=0 提高确定性；任何异常（调用失败/返回空/结果无法解析）
     * 都降级回第一轮结果，不阻塞导入流程。</p>
     *
     * @param imageDataUrl 原始课表图片（data URL）
     * @param firstPass    第一轮提取结果
     * @param userId       用户标识（仅用于日志）
     * @return 校验合并后的课程列表；异常时原样返回 firstPass
     */
    private List<CourseEntity> verifyCoursesWithImage(String imageDataUrl, List<CourseEntity> firstPass, String userId) {
        if (firstPass == null || firstPass.isEmpty()) {
            return firstPass;
        }
        try {
            String checklist = buildVerifyChecklist(firstPass);
            String verifyResult = visionService.analyze(imageDataUrl, COURSE_VERIFY_PROMPT + "\n\n" + checklist, 0.0);
            if (verifyResult == null || verifyResult.isBlank()) {
                log.warn("课表导入：校验轮返回空，降级使用第一轮 | userId={}", userId);
                return firstPass;
            }

            log.info("课表导入：校验轮完成 | userId={} | resultLen={} | raw={}",
                    userId, verifyResult.length(), verifyResult);

            List<CourseEntity> verified = courseParser.applyVisionCorrections(firstPass, verifyResult);
            if (verified == null) {
                log.warn("课表导入：校验结果无法解析，降级使用第一轮 | userId={}", userId);
                return firstPass;
            }
            if (verified.size() != firstPass.size()) {
                log.info("课表导入：校验调整课程数量 | userId={} | before={} | after={}",
                        userId, firstPass.size(), verified.size());
            }
            return verified;
        } catch (Exception e) {
            log.error("课表导入：校验轮异常，降级使用第一轮 | userId={}", userId, e);
            return firstPass;
        }
    }

    /**
     * 构建待校验清单：带索引的课程明细，供校验轮逐条对照图片挑错。
     * <p>索引与 applyVisionCorrections 的 deletions/corrections.index 一一对应。</p>
     */
    private String buildVerifyChecklist(List<CourseEntity> courses) {
        StringBuilder sb = new StringBuilder("【待核对清单】（索引 + 课程信息，索引从0开始）：\n");
        for (int i = 0; i < courses.size(); i++) {
            CourseEntity c = courses.get(i);
            sb.append(i).append(". ").append(c.getCourseName());
            sb.append(" | ").append(c.getDayDisplay()).append(" ");
            sb.append(c.getStartPeriod()).append("-").append(c.getEndPeriod()).append("节");
            sb.append(" | ").append(c.getWeekDisplay());
            if (!c.getClassroom().isBlank()) sb.append(" | 教室:").append(c.getClassroom());
            if (!c.getTeacher().isBlank()) sb.append(" | 教师:").append(c.getTeacher());
            sb.append("\n");
        }
        return sb.toString();
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

        // 学期检测从文件名和内容
        String contentPreview = result != null ? result.text() : null;
        return finalizePendingImport(userId, courses, llmResult, fileName, contentPreview,
                "[课表导入文档解析完成] " + fileName);
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

        return finalizePendingImport(userId, courses, "[PDF 解析] " + fileName, fileName, null,
                "[课表导入 PDF 解析完成] " + fileName);
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

        SemesterEntity pendingSemester = importStateManager.getPendingSemester(userId);
        String semesterInfo = "";
        if (pendingSemester != null) {
            semesterInfo = "【" + pendingSemester.getDisplayName() + "】\n"
                    + "第1周：" + pendingSemester.getStartDateDisplay() + "\n\n";
        }

        List<String> internalConflicts = detectInternalDayConflicts(courses);
        if (!internalConflicts.isEmpty()) {
            log.warn("课表导入：识别结果存在同天同时段冲突 | userId={} | conflicts={}",
                    userId, internalConflicts);
        }

        return messageFormatter.formatPendingImportPreview(courses, semesterInfo, currentWeek, internalConflicts);
    }

    /**
     * 解析用户当前教学周
     *
     * <p>优先使用用户自身的 {@link SemesterService#getCurrentWeek(String)} 计算结果；
     * 无学期记录时回退 {@link SemesterProperties#getCurrentWeek()}。
     */
    private int resolveCurrentWeek(String userId) {
        int week = semesterService.getCurrentWeek(userId);
        if (week > 0) {
            return week;
        }
        return semesterConfig.getCurrentWeek();
    }

    private String buildPreviewText(String userId, List<CourseEntity> courses) {
        return buildPreview(userId, courses);
    }

    /**
     * 检测新解析的课程列表内部是否存在同天同时段冲突
     *
     * <p>同一用户同一 day_of_week 的同一时间段出现多门课程 → day_of_week / 节次很可能分配错误
     * （图片识别的图案补全幻觉、或拆分错误）。只记录并展示 warning，不修改数据。</p>
     */
    private List<String> detectInternalDayConflicts(List<CourseEntity> courses) {
        List<String> conflicts = new ArrayList<>();
        for (int i = 0; i < courses.size(); i++) {
            for (int j = i + 1; j < courses.size(); j++) {
                CourseEntity a = courses.get(i);
                CourseEntity b = courses.get(j);
                if (a.getDayOfWeek() == b.getDayOfWeek()
                        && a.getStartPeriod() <= b.getEndPeriod()
                        && b.getStartPeriod() <= a.getEndPeriod()) {
                    String desc = String.format("%s %s节：「%s」与「%s」冲突",
                            a.getDayDisplay(), a.getPeriodDisplay(),
                            a.getCourseName(), b.getCourseName());
                    conflicts.add(desc);
                }
            }
        }
        return conflicts;
    }

    /**
     * 合并导入收尾流水线（Excel/图片/PDF/文档四路共用，批次 4 去重）：
     * 暂存课程 → 学期检测 → 置等待确认 → 写上下文 → 返回预览。
     *
     * @param userId           用户标识
     * @param courses          解析出的课程列表
     * @param confirmPayload   等待确认时暂存的内容（原路透传，作为确认校验的凭据）
     * @param fileName         文件名（可为 null，图片 IMAGE 消息无文件名）
     * @param contentPreview   文件内容预览（仅文档/PDF 路径有，可为 null）
     * @param userLog          写入上下文的 user 侧日志前缀（不含课程数，方法内拼接）
     */
    private WechatReply finalizePendingImport(String userId, List<CourseEntity> courses,
                                              String confirmPayload, String fileName,
                                              String contentPreview, String userLog) {
        importStateManager.setPendingCourses(userId, courses);

        // 学期检测
        detectAndStoreSemester(userId, fileName, contentPreview);
        importStateManager.setWaitingConfirm(userId, confirmPayload);

        contextStore.append("user", userLog + "，共 " + courses.size() + " 门课程");
        contextStore.append("assistant", buildPreviewText(userId, courses));

        return WechatReply.text(buildPreview(userId, courses));
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
        // 精确匹配 application/pdf；不能 contains("application")——会把 Excel/octet-stream/JSON 等全判成 PDF
        if (mimeType != null && mimeType.equals("application/pdf")) {
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