package com.youkeda.exercise.claw.feature.schedule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 课表导入流程操作（import → parse → confirm/cancel，含学期确认）。
 *
 * <p>从 {@code CourseImportTool} 拆出的导入分组委托类（批次 4）。
 * 仅处理与导入状态机相关的 action；查询/管理/学校操作见
 * {@code CourseQueryActions} / {@code CourseSchoolActions}。
 */
@Component
public class CourseImportFlowActions {

    private static final Logger log = LoggerFactory.getLogger(CourseImportFlowActions.class);

    private final CourseService courseService;
    private final CourseRepository courseRepository;
    private final SemesterProperties semesterConfig;
    private final CourseImportStateManager importStateManager;
    private final SemesterDetector semesterDetector;
    private final SemesterRepository semesterRepository;
    private final SemesterService semesterService;
    private final CourseMessageFormatter messageFormatter;
    private final ObjectMapper objectMapper;

    public CourseImportFlowActions(CourseService courseService,
                                   CourseRepository courseRepository,
                                   SemesterProperties semesterConfig,
                                   CourseImportStateManager importStateManager,
                                   SemesterDetector semesterDetector,
                                   SemesterRepository semesterRepository,
                                   SemesterService semesterService,
                                   CourseMessageFormatter messageFormatter,
                                   ObjectMapper objectMapper) {
        this.courseService = courseService;
        this.courseRepository = courseRepository;
        this.semesterConfig = semesterConfig;
        this.importStateManager = importStateManager;
        this.semesterDetector = semesterDetector;
        this.semesterRepository = semesterRepository;
        this.semesterService = semesterService;
        this.messageFormatter = messageFormatter;
        this.objectMapper = objectMapper;
    }

    public String handleStartImport(String userId) {
        int existingCount = courseService.getCourseCount(userId);
        if (existingCount > 0) {
            courseService.deleteAll(userId);
            log.info("导入新课表：已清除旧课表 | userId={} | count={}", userId, existingCount);
        }

        importStateManager.setWaitingFile(userId);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("action", "import");
        result.put("status", "waiting_file");
        String msg = existingCount > 0
                ? "已清除旧课表（共 " + existingCount + " 门课程），请发送新课表截图、PDF或Excel文件。"
                : "请发送课表截图、PDF或Excel文件，我会帮你导入课表。";
        result.put("message", msg);
        if (existingCount > 0) {
            result.put("cleared_count", existingCount);
        }
        return result.toString();
    }

    public String handleParse(JsonNode args, String userId) {
        CourseImportStateManager.Phase phase = importStateManager.getPhase(userId);
        if (phase == CourseImportStateManager.Phase.NONE) {
            log.debug("直接解析课表（无前置 import 状态）| userId={}", userId);
        }

        // 优先使用图片/文件路径已解析并暂存的完整结构化课程（含星期/节次/周次），
        // 避免 Agent 从对话上下文裸重建时丢失结构（历史上曾全部退化成"周一第1节"）。
        List<CourseEntity> pending = importStateManager.getPendingCourses(userId);
        String jsonStr;
        List<CourseEntity> courses;
        if (!pending.isEmpty()) {
            courses = pending;
            jsonStr = null;
            log.info("parse 使用图片/文件解析的待确认课程 | userId={} | count={}", userId, pending.size());
        } else {
            JsonNode coursesNode = args.get("courses");
            if (coursesNode == null || !coursesNode.isArray() || coursesNode.isEmpty()) {
                return "{\"action\":\"parse\",\"status\":\"error\","
                        + "\"message\":\"请从对话上下文中的文件/图片分析结果里提取课程信息后重新调用。\"}";
            }

            jsonStr = coursesNode.toString();
            courses = courseService.parseOnly(userId, jsonStr);

            if (courses.isEmpty()) {
                return "{\"action\":\"parse\",\"status\":\"error\","
                        + "\"message\":\"无法从提供的数据中识别出有效的课程信息。请重新上传课表图片/文件；"
                        + "若使用文字导入，每条课程必须包含星期(day_of_week)和节次(start_period/end_period)。\"}";
            }
        }

        // 内部冲突检测：检查新解析出的课程间是否有同天同时段冲突
        // 这种冲突通常表示 LLM 的 day_of_week 分配有误
        List<String> internalConflicts = detectInternalDayConflicts(courses);
        for (String conflict : internalConflicts) {
            log.warn("新导入课程间存在同天同时段冲突 | userId={} | {}", userId, conflict);
        }

        // 冲突检测（与已有课表）
        List<CourseService.ConflictInfo> conflicts = courseService.detectConflicts(userId, courses);

        // ====================  Semester 检测 ====================
        // 从 LLM 参数中尝试提取学期信息
        int academicYear = args.path("academic_year").asInt(0);
        String term = args.path("term").asText("");

        SemesterEntity detectedSemester = null;
        if (academicYear > 0 && !term.isBlank()) {
            detectedSemester = semesterDetector.detectFromParams(userId, academicYear, term);
        }
        if (detectedSemester == null) {
            // LLM 未提供学期信息，尝试自动推算
            detectedSemester = semesterDetector.detectAuto(userId);
            if (detectedSemester != null) {
                log.info("parse 时自动推算学期 | userId={} | display={}",
                        userId, detectedSemester.getDisplayName());
            }
        }

        // 存储待确认的学期（未持久化）
        if (detectedSemester != null) {
            importStateManager.setPendingSemester(userId, detectedSemester);
        }
        importStateManager.setPendingCourses(userId, courses);
        importStateManager.setWaitingConfirm(userId, jsonStr);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("action", "parse");
        result.put("status", "preview");
        result.put("count", courses.size());

        int currentWeek = resolveCurrentWeek(userId);
        result.put("current_week", currentWeek);
        result.put("current_week_display", currentWeek > 0 ? "第" + currentWeek + "周" : "学期未开始");

        var array = result.putArray("courses");
        for (CourseEntity c : courses) {
            ObjectNode item = array.addObject();
            item.put("course_name", c.getCourseName());
            item.put("day", c.getDayDisplay());
            item.put("period", c.getPeriodDisplay());
            item.put("weeks", c.getWeekDisplay());
            if (!c.getClassroom().isBlank()) item.put("classroom", c.getClassroom());
            if (!c.getTeacher().isBlank()) item.put("teacher", c.getTeacher());
        }

        // Semester 信息
        if (detectedSemester != null) {
            ObjectNode semesterInfo = result.putObject("semester");
            semesterInfo.put("academic_year", detectedSemester.getAcademicYear());
            semesterInfo.put("term", detectedSemester.getTerm());
            semesterInfo.put("start_date", detectedSemester.getStartDateString());
            semesterInfo.put("start_date_display", detectedSemester.getStartDateDisplay());
            semesterInfo.put("source", detectedSemester.getSource());
            semesterInfo.put("source_display", detectedSemester.getSourceDisplay());
            semesterInfo.put("display_name", detectedSemester.getDisplayName());
            result.put("semester_display", detectedSemester.getDisplayName()
                    + "（第1周：" + detectedSemester.getStartDateDisplay() + "）");
        }

        // 冲突信息
        if (!conflicts.isEmpty()) {
            ArrayNode conflictArray = result.putArray("conflicts");
            for (CourseService.ConflictInfo cf : conflicts) {
                ObjectNode item = conflictArray.addObject();
                item.put("existing_course", cf.existingCourse().getCourseName());
                item.put("new_course", cf.newCourse().getCourseName());
                item.put("description", cf.description());
            }
            result.put("warning", "检测到 " + conflicts.size() + " 个时间冲突，确认后冲突课程将被覆盖");
        }

        // 与 modify_pending / 文件导入路径统一走带组内编号的预览，确保 course_index 始终有编号可对应
        String semesterInfo = "";
        if (detectedSemester != null) {
            semesterInfo = "【" + detectedSemester.getDisplayName() + "】\n"
                    + "第1周：" + detectedSemester.getStartDateDisplay() + "\n\n";
        }
        result.put("formatted_preview", messageFormatter.formatPendingImportPreview(
                courses, semesterInfo, currentWeek, conflicts));

        String conflictSuffix = conflicts.isEmpty() ? "" : "，" + conflicts.size() + " 个时间冲突";
        result.put("message", "已识别出以下 " + courses.size() + " 门课程" + conflictSuffix
                + "，请确认是否导入？（回复「确认」或「取消」）");
        return result.toString();
    }

    /**
     * 检测新解析的课程列表内部是否存在同天同时段冲突
     * <p>同一用户同一 day_of_week 的同一时间段出现多门课程 → day_of_week 很可能分配错误。
     * 只记录 warning，不修改数据。</p>
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
                    String desc = String.format("day=%d period=%d-%d: 「%s」与「%s」冲突",
                            a.getDayOfWeek(), a.getStartPeriod(), a.getEndPeriod(),
                            a.getCourseName(), b.getCourseName());
                    conflicts.add(desc);
                }
            }
        }
        return conflicts;
    }

    public String handleConfirm(String userId) {
        List<CourseEntity> pending = importStateManager.getPendingCourses(userId);
        if (pending.isEmpty()) {
            return "{\"action\":\"confirm\",\"status\":\"error\","
                    + "\"message\":\"没有待确认的课程数据，请先上传课表。\"}";
        }

        List<CourseEntity> saved;
        SemesterEntity pendingSemester = importStateManager.getPendingSemester(userId);

        if (pendingSemester != null) {
            // 有学期信息：检查是否已存在同一学期 → 复用，避免重复创建
            SemesterEntity existing = semesterService.findExistingSemester(
                    userId, pendingSemester.getAcademicYear(), pendingSemester.getTerm()).orElse(null);
            SemesterEntity savedSemester;
            if (existing != null) {
                savedSemester = existing;
                log.info("学期已存在，复用 | id={} | display={}", savedSemester.getId(), savedSemester.getDisplayName());
            } else {
                savedSemester = semesterRepository.save(pendingSemester);
                log.info("学期已创建 | id={} | display={}", savedSemester.getId(), savedSemester.getDisplayName());
            }

            // 为每条课程绑定 semesterId 并按学期保存
            pending.forEach(c -> c.setSemesterId(savedSemester.getId()));
            saved = courseRepository.replaceAllBySemester(userId, savedSemester.getId(), pending);

            importStateManager.clear(userId);

            ObjectNode result = objectMapper.createObjectNode();
            result.put("action", "confirm");
            result.put("status", "success");
            result.put("count", saved.size());
            result.put("semester_id", savedSemester.getId());
            result.put("semester_display", savedSemester.getDisplayName());

            int currentWeek = resolveCurrentWeek(userId);
            result.put("current_week", currentWeek);
            result.put("current_week_display", currentWeek > 0 ? "第" + currentWeek + "周" : "学期未开始");

            result.put("message", "课表导入成功！共 " + saved.size() + " 门课程（"
                    + savedSemester.getDisplayName() + "）。"
                    + "你可以问我「今天有什么课」来查看今日课程。");
            return result.toString();
        } else {
            // 无学期信息：保持旧行为（兼容）
            saved = courseService.saveCourses(userId, pending);
            importStateManager.clear(userId);

            ObjectNode result = objectMapper.createObjectNode();
            result.put("action", "confirm");
            result.put("status", "success");
            result.put("count", saved.size());

            int currentWeek = resolveCurrentWeek(userId);
            result.put("current_week", currentWeek);
            result.put("current_week_display", currentWeek > 0 ? "第" + currentWeek + "周" : "学期未开始");

            result.put("message", "课表导入成功！共 " + saved.size() + " 门课程。"
                    + "你可以问我「今天有什么课」来查看今日课程。");
            return result.toString();
        }
    }

    public String handleCancel(String userId) {
        importStateManager.clear(userId);
        return "{\"action\":\"cancel\",\"status\":\"success\",\"message\":\"已取消课表导入。\"}";
    }

    /**
     * 预览确认阶段修改待确认课程（支持一次多门）。
     *
     * <p>入参 courses 数组，每项：day_of_week(必填，仅用于定位该课当前所在星期) + course_index(该天第几个,1-based)/course_name 定位
     * + 可改字段(week_type/start_period/end_period/classroom/teacher/start_week/end_week)。
     * 只改提供的字段，其余保持。改完重新生成预览，仍需用户确认才落库。
     * day_of_week 仅用于定位（该课当前所在星期），不支持跨天移动；如需挪天请删课重加。
     */
    public String handleModifyPending(JsonNode args, String userId) {
        List<CourseEntity> pending = new ArrayList<>(importStateManager.getPendingCourses(userId));
        if (pending.isEmpty()) {
            return errorJson("没有待确认的课程数据，请先上传课表。");
        }

        JsonNode coursesNode = args.get("courses");
        if (coursesNode == null || !coursesNode.isArray() || coursesNode.isEmpty()) {
            return errorJson("请提供要修改的课程（courses 数组，每项含 day_of_week 和 course_index/course_name）。");
        }

        // 两遍处理：先全部定位+校验（不 set），全部通过后再统一应用，避免校验失败污染 pending 实体
        // add=true 表示新增一门课（newCourse 非空，无需定位）；delete=true 表示删除；否则为修改字段
        record ModifyOp(CourseEntity target, CourseEntity newCourse, boolean add, boolean delete,
                        String weekType, Integer startPeriod, Integer endPeriod,
                        String classroom, String teacher, Integer startWeek, Integer endWeek) {}
        List<ModifyOp> ops = new ArrayList<>();

        for (JsonNode item : coursesNode) {
            // 新增标记：add=true 时构建新课（需完整课程信息），跳过对已存在课程的定位
            boolean add = item.path("add").asBoolean(false);
            if (add) {
                String newName = item.path("course_name").asText("");
                int newDay = item.path("day_of_week").asInt(0);
                int newStartPeriod = item.path("start_period").asInt(0);
                int newEndPeriod = item.path("end_period").asInt(0);
                if (newName.isBlank()) {
                    return errorJson("新增课程缺少 course_name。");
                }
                if (newDay < 1 || newDay > 7) {
                    return errorJson("新增课程 day_of_week 必须为 1-7（1=周一 ~ 7=周日）。");
                }
                if (newStartPeriod < 1 || newEndPeriod < newStartPeriod) {
                    return errorJson("新增课程「" + newName + "」节次非法：start_period 须 >=1 且 end_period 须 >= start_period。");
                }
                int newStartWeek = item.has("start_week") ? item.path("start_week").asInt(1) : 1;
                int newEndWeek = item.has("end_week") ? item.path("end_week").asInt(20) : 20;
                if (newStartWeek < 1 || newEndWeek < newStartWeek) {
                    return errorJson("新增课程「" + newName + "」周次非法：start_week 须 >=1 且 end_week 须 >= start_week。");
                }
                String newWeekType = item.path("week_type").asText("ALL");
                if (!"ALL".equals(newWeekType) && !"ODD".equals(newWeekType) && !"EVEN".equals(newWeekType)) {
                    return errorJson("新增课程「" + newName + "」week_type 只能为 ALL/ODD/EVEN。");
                }
                CourseEntity newCourse = new CourseEntity(
                        userId, newName,
                        item.path("teacher").asText(""),
                        newDay, newStartPeriod, newEndPeriod,
                        item.path("classroom").asText(""),
                        newStartWeek, newEndWeek, newWeekType);
                ops.add(new ModifyOp(null, newCourse, true, false, null, null, null, null, null, null, null));
                continue;
            }

            int dayOfWeek = item.path("day_of_week").asInt(0);
            if (dayOfWeek < 1 || dayOfWeek > 7) {
                return errorJson("day_of_week 必须为 1-7（1=周一 ~ 7=周日）。");
            }

            // 定位该天的课程（day_of_week 仅用于定位过滤，不可修改）
            List<CourseEntity> dayCourses = new ArrayList<>();
            for (CourseEntity c : pending) {
                if (c.getDayOfWeek() == dayOfWeek) dayCourses.add(c);
            }
            if (dayCourses.isEmpty()) {
                return errorJson("第 " + dayOfWeek + " 天没有待确认课程。");
            }

            int index = item.path("course_index").asInt(0);
            CourseEntity target = null;
            if (index > 0) {
                if (index > dayCourses.size()) {
                    return errorJson("第 " + dayOfWeek + " 天只有 " + dayCourses.size() + " 门课，无法定位第 " + index + " 门。");
                }
                target = dayCourses.get(index - 1);
            }
            String nameHint = item.path("course_name").asText("");
            if (!nameHint.isBlank()) {
                CourseEntity byName = dayCourses.stream()
                        .filter(c -> nameHint.equals(c.getCourseName()))
                        .findFirst().orElse(null);
                if (byName == null) {
                    return errorJson("第 " + dayOfWeek + " 天没有名为「" + nameHint + "」的课程。");
                }
                // 引用比较：双定位符（index + name）必须指向同一门课，否则报错，避免同名课程静默改错
                if (target != null && byName != target) {
                    return errorJson("存在同名课程，course_index=" + index + " 与 course_name「" + nameHint + "」定位不一致，请只用 course_index 精确指定。");
                }
                target = byName;
            }
            if (target == null) {
                return errorJson("请提供 course_index（该天第几个）或 course_name 来指定要修改或删除的课程。");
            }

            // 删除标记：delete=true 时只定位+移除，不做字段修改
            boolean delete = item.path("delete").asBoolean(false);
            if (delete) {
                ops.add(new ModifyOp(target, null, false, true, null, null, null, null, null, null, null));
                continue;
            }

            // 解析待设值（先不 set，全部校验通过后统一应用）
            String weekType = item.has("week_type") ? item.get("week_type").asText() : null;
            Integer startPeriod = item.has("start_period") ? item.get("start_period").asInt() : null;
            Integer endPeriod = item.has("end_period") ? item.get("end_period").asInt() : null;
            String classroom = item.has("classroom") ? item.get("classroom").asText() : null;
            String teacher = item.has("teacher") ? item.get("teacher").asText() : null;
            Integer startWeek = item.has("start_week") ? item.get("start_week").asInt() : null;
            Integer endWeek = item.has("end_week") ? item.get("end_week").asInt() : null;

            // 数值字段校验：节次/周次须为正且区间不反转（用应用后的有效值判断）
            int newStartPeriod = startPeriod != null ? startPeriod : target.getStartPeriod();
            int newEndPeriod = endPeriod != null ? endPeriod : target.getEndPeriod();
            if (newStartPeriod < 1 || newEndPeriod < newStartPeriod) {
                return errorJson("课程「" + target.getCourseName() + "」节次非法：start_period 须 >=1 且 end_period 须 >= start_period。");
            }
            int newStartWeek = startWeek != null ? startWeek : target.getStartWeek();
            int newEndWeek = endWeek != null ? endWeek : target.getEndWeek();
            if (newStartWeek < 1 || newEndWeek < newStartWeek) {
                return errorJson("课程「" + target.getCourseName() + "」周次非法：start_week 须 >=1 且 end_week 须 >= start_week。");
            }
            // week_type 取值校验
            if (weekType != null && !"ALL".equals(weekType) && !"ODD".equals(weekType) && !"EVEN".equals(weekType)) {
                return errorJson("week_type 只能为 ALL/ODD/EVEN。");
            }

            ops.add(new ModifyOp(target, null, false, false, weekType, startPeriod, endPeriod, classroom, teacher, startWeek, endWeek));
        }

        // 全部校验通过后统一应用：先新增、再删除、最后修改，避免删除/新增影响后续定位
        List<String> applied = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        List<String> added = new ArrayList<>();
        // 先新增（追加到 pending，新课不参与删除/修改的定位）
        for (ModifyOp op : ops) {
            if (op.add()) {
                pending.add(op.newCourse());
                added.add(op.newCourse().getCourseName());
            }
        }
        // 再删除（按 pending 中的对象移除）
        for (ModifyOp op : ops) {
            if (op.delete()) {
                boolean removedFlag = pending.removeIf(c -> c == op.target());
                if (removedFlag) {
                    removed.add(op.target().getCourseName());
                }
            }
        }
        // 最后修改（此时 pending 已增/删完，剩余对象应用字段）
        for (ModifyOp op : ops) {
            if (op.delete() || op.add()) continue;
            CourseEntity target = op.target();
            if (op.weekType() != null) target.setWeekType(op.weekType());
            if (op.startPeriod() != null) target.setStartPeriod(op.startPeriod());
            if (op.endPeriod() != null) target.setEndPeriod(op.endPeriod());
            if (op.classroom() != null) target.setClassroom(op.classroom());
            if (op.teacher() != null) target.setTeacher(op.teacher());
            if (op.startWeek() != null) target.setStartWeek(op.startWeek());
            if (op.endWeek() != null) target.setEndWeek(op.endWeek());
            applied.add(target.getCourseName());
        }

        importStateManager.setPendingCourses(userId, pending);

        List<String> internalConflicts = detectInternalDayConflicts(pending);
        for (String conflict : internalConflicts) {
            log.warn("modify_pending 后存在同天同时段冲突 | userId={} | {}", userId, conflict);
        }

        SemesterEntity pendingSemester = importStateManager.getPendingSemester(userId);
        String semesterInfo = "";
        if (pendingSemester != null) {
            semesterInfo = "【" + pendingSemester.getDisplayName() + "】\n"
                    + "第1周：" + pendingSemester.getStartDateDisplay() + "\n\n";
        }
        int currentWeek = resolveCurrentWeek(userId);

        StringBuilder msg = new StringBuilder();
        if (!added.isEmpty()) msg.append("已新增 ").append(added.size()).append(" 门课（").append(String.join("、", added)).append("）");
        if (!removed.isEmpty()) {
            if (!msg.isEmpty()) msg.append("，");
            msg.append("已删除 ").append(removed.size()).append(" 门课（").append(String.join("、", removed)).append("）");
        }
        if (!applied.isEmpty()) {
            if (!msg.isEmpty()) msg.append("，");
            msg.append("已修改 ").append(applied.size()).append(" 门课");
        }
        if (msg.isEmpty()) msg.append("未做任何修改");
        msg.append("。请确认预览后回复「确认」保存。");

        ObjectNode result = objectMapper.createObjectNode();
        result.put("action", "modify_pending");
        result.put("status", "preview");
        result.put("count", pending.size());
        result.put("added", String.join("、", added));
        result.put("modified", String.join("、", applied));
        result.put("removed", String.join("、", removed));
        result.put("formatted_preview", messageFormatter.formatPendingImportPreview(
                pending, semesterInfo, currentWeek, internalConflicts));
        result.put("message", msg.toString());
        return result.toString();
    }

    private String errorJson(String message) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("action", "modify_pending");
        node.put("status", "error");
        node.put("message", message);
        return node.toString();
    }

    /**
     * 用户确认系统自动检测的学期
     *
     * <p>学期信息已通过 handleParse 或 handler 保存为 pendingSemester，
     * 用户确认后转移到 WAITING_CONFIRM 状态，等待最终确认导入。
     */
    public String handleConfirmSemester(String userId) {
        if (importStateManager.getPhase(userId) != CourseImportStateManager.Phase.WAITING_SEMESTER) {
            return "{\"action\":\"confirm_semester\",\"status\":\"error\","
                    + "\"message\":\"当前状态不需要确认学期。\"}";
        }

        SemesterEntity pending = importStateManager.getPendingSemester(userId);
        if (pending == null) {
            return "{\"action\":\"confirm_semester\",\"status\":\"error\","
                    + "\"message\":\"没有待确认的学期信息，请先上传课表。\"}";
        }

        List<CourseEntity> pendingCourses = importStateManager.getPendingCourses(userId);
        if (pendingCourses.isEmpty()) {
            return "{\"action\":\"confirm_semester\",\"status\":\"error\","
                    + "\"message\":\"没有待确认的课程数据，请先上传课表。\"}";
        }

        // 学期信息不需要持久化（等 confirm 时一起处理），直接进入 WAITING_CONFIRM
        importStateManager.setWaitingConfirm(userId, "semester_confirmed");

        ObjectNode result = objectMapper.createObjectNode();
        result.put("action", "confirm_semester");
        result.put("status", "success");
        result.put("semester_display", pending.getDisplayName());
        result.put("start_date", pending.getStartDateString());
        result.put("course_count", pendingCourses.size());
        result.put("message", "已确认" + pending.getDisplayName() + "（第1周：" + pending.getStartDateDisplay() + "），"
                + "共 " + pendingCourses.size() + " 门课程等待导入。回复「确认」保存课表。");
        return result.toString();
    }

    /**
     * 用户手动指定学期信息
     *
     * <p>当系统无法自动检测学期时，用户可通过此操作指定学期。
     * 支持参数：academic_year、term、start_date（可选）
     */
    public String handleSetSemester(JsonNode args, String userId) {
        int academicYear = args.path("academic_year").asInt(0);
        String term = args.path("term").asText("");

        if (academicYear <= 0 || term.isBlank()) {
            return "{\"action\":\"set_semester\",\"status\":\"error\","
                    + "\"message\":\"请提供学年和学期信息，如：academic_year=2026, term=FALL\"}";
        }

        // 尝试从用户传入的学期参数检测
        SemesterEntity semester = semesterDetector.detectFromParams(userId, academicYear, term);

        // 如果用户提供了 start_date，则覆盖
        String startDateStr = args.path("start_date").asText("");
        if (semester != null && !startDateStr.isBlank()) {
            try {
                semester.setStartDateFromString(startDateStr);
                log.info("用户指定学期起始日期 | startDate={}", startDateStr);
            } catch (Exception e) {
                return "{\"action\":\"set_semester\",\"status\":\"error\","
                        + "\"message\":\"日期格式错误，请使用 yyyy-MM-dd 格式，如 2026-09-07\"}";
            }
        } else if (semester == null) {
            return "{\"action\":\"set_semester\",\"status\":\"error\","
                    + "\"message\":\"无法识别的学期参数，请提供正确的学年（如2026）和学期（SPRING/FALL）\"}";
        }

        // 保存待确认学期
        importStateManager.setPendingSemester(userId, semester);

        List<CourseEntity> pendingCourses = importStateManager.getPendingCourses(userId);

        // 如果已有待确认课程，直接进入 WAITING_CONFIRM
        if (!pendingCourses.isEmpty()) {
            importStateManager.setWaitingConfirm(userId, "semester_set");

            ObjectNode result = objectMapper.createObjectNode();
            result.put("action", "set_semester");
            result.put("status", "success");
            result.put("semester_display", semester.getDisplayName());
            result.put("start_date", semester.getStartDateString());
            result.put("course_count", pendingCourses.size());
            result.put("message", "已设置" + semester.getDisplayName() + "（第1周：" + semester.getStartDateDisplay() + "），"
                    + "共 " + pendingCourses.size() + " 门课程等待导入。回复「确认」保存课表。");
            return result.toString();
        } else {
            // 无待确认课程，进入 WAITING_SEMESTER
            importStateManager.setWaitingSemester(userId);

            ObjectNode result = objectMapper.createObjectNode();
            result.put("action", "set_semester");
            result.put("status", "semester_ready");
            result.put("semester_display", semester.getDisplayName());
            result.put("start_date", semester.getStartDateString());
            result.put("message", "已设置" + semester.getDisplayName() + "（第1周：" + semester.getStartDateDisplay() + "）。"
                    + "请发送课表文件或图片。");
            return result.toString();
        }
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
}
