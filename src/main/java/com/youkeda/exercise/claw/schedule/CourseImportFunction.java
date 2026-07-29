package com.youkeda.exercise.claw.schedule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.tool.FunctionExecutionContext;
import com.youkeda.exercise.claw.agent.tool.LLMFunction;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 课程表管理 LLM Function
 *
 * <p>注册名称：{@code course_schedule}
 *
 * <p>处理用户课表的导入（多步确认）、查询、修改、删除等操作。
 * 数据持久化通过 {@link CourseRepository} 写入 SQLite {@code course_schedule} 表，
 * 以 {@code userId} 作为数据隔离键。
 *
 * <h3>导入流程（三步确认）：</h3>
 * <ol>
 *   <li>用户说「导入课表」→ 调用 {@code import} → 系统等待文件</li>
 *   <li>用户发送课表图片/文件后 → LLM 从对话上下文中提取课程信息
 *       → 调用 {@code parse} 传入提取的课程数据 → 系统返回预览</li>
 *   <li>用户确认 → 调用 {@code confirm} → 保存入库</li>
 *   <li>用户取消 → 调用 {@code cancel} → 丢弃</li>
 * </ol>
 */
@Component
public class CourseImportFunction implements LLMFunction {

    private static final Logger log = LoggerFactory.getLogger(CourseImportFunction.class);

    private static final String[] DAY_NAMES = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private final ObjectMapper objectMapper;
    private final LLMFunctionRegistry functionRegistry;
    private final CourseService courseService;
    private final CourseRepository courseRepository;
    private final SemesterConfig semesterConfig;
    private final CourseImportStateManager importStateManager;

    public CourseImportFunction(ObjectMapper objectMapper,
                                LLMFunctionRegistry functionRegistry,
                                CourseService courseService,
                                CourseRepository courseRepository,
                                SemesterConfig semesterConfig,
                                CourseImportStateManager importStateManager) {
        this.objectMapper = objectMapper;
        this.functionRegistry = functionRegistry;
        this.courseService = courseService;
        this.courseRepository = courseRepository;
        this.semesterConfig = semesterConfig;
        this.importStateManager = importStateManager;
    }

    @PostConstruct
    public void init() {
        functionRegistry.register(this);
        log.info("CourseImportFunction 已注册到 LLMFunctionRegistry");
    }

    @Override
    public String getName() {
        return "course_schedule";
    }

    @Override
    public String getDescription() {
        return "课程表管理。管理用户的个人课程表数据（以userId隔离持久化到SQLite）。\n"
                + "支持操作：\n"
                + "- 导入：使用 import -> parse -> confirm 三步流程导入课表（图片/PDF/Excel/直接JSON）\n"
                + "- 查询：query_today（今日课程，自动过滤学期周次和单双周）\n"
                + "         query_weekday（指定星期几的课程，如\"周一\"->day_of_week=1）\n"
                + "         query_all（全部课程列表）\n"
                + "         query_free_time（今日空闲时间段）\n"
                + "- 管理：delete（单条删除，需id）、update（修改）、clear（清空全部）\n"
                + "适用于：用户问\"今天有什么课\"\"明天课表\"\"导入课表\"\"帮我加一门课\"\"删除高数\"等场景。";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("type", "object");

        ObjectNode properties = params.putObject("properties");

        ObjectNode action = properties.putObject("action");
        action.put("type", "string");
        action.put("description", "操作类型：import(开始导入), parse(解析并预览), confirm(确认保存), "
                + "cancel(取消), query_today(今日课程), query_free_time(空闲时间), "
                + "query_all(全部课程), query_weekday(指定星期), delete(删除), update(修改), clear(清空)");
        action.putArray("enum").add("import").add("parse").add("confirm").add("cancel")
                .add("query_today").add("query_free_time").add("query_all").add("query_weekday")
                .add("delete").add("update").add("clear");

        ObjectNode courses = properties.putObject("courses");
        courses.put("type", "array");
        courses.put("description", "课程列表（parse 时必填，update 时可选）。每门课包含以下字段：");
        ObjectNode courseItem = courses.putObject("items");
        courseItem.put("type", "object");

        ObjectNode itemProps = courseItem.putObject("properties");

        itemProps.putObject("course_name").put("type", "string")
                .put("description", "课程名称，如「高等数学」");
        itemProps.putObject("teacher").put("type", "string")
                .put("description", "授课教师姓名");
        itemProps.putObject("day_of_week").put("type", "integer")
                .put("description", "星期几：1=周一 2=周二 3=周三 4=周四 5=周五 6=周六 7=周日");
        itemProps.putObject("start_period").put("type", "integer")
                .put("description", "开始节次（第几节课开始，从1开始）");
        itemProps.putObject("end_period").put("type", "integer")
                .put("description", "结束节次（第几节课结束，>= start_period）");
        itemProps.putObject("classroom").put("type", "string")
                .put("description", "上课教室/地点");
        itemProps.putObject("start_week").put("type", "integer")
                .put("description", "开始教学周（默认1）");
        itemProps.putObject("end_week").put("type", "integer")
                .put("description", "结束教学周（默认20）");
        ObjectNode weekType = itemProps.putObject("week_type");
        weekType.put("type", "string");
        weekType.put("description", "单双周：ALL=全部周(默认), ODD=单周, EVEN=双周");
        weekType.putArray("enum").add("ALL").add("ODD").add("EVEN");

        ObjectNode courseId = properties.putObject("course_id");
        courseId.put("type", "integer");
        courseId.put("description", "课程 ID（delete 和 update 时必填）。调用 delete 前请先通过 query_all 获取课程 ID。");

        ObjectNode dayOfWeek = properties.putObject("day_of_week");
        dayOfWeek.put("type", "integer");
        dayOfWeek.put("description", "星期几：1=周一 2=周二 3=周三 4=周四 5=周五 6=周六 7=周日");

        params.putArray("required").add("action");

        return params;
    }

    @Override
    public String execute(String argumentsJson) {
        return "{\"error\": \"缺少用户上下文\"}";
    }

    @Override
    public String execute(String argumentsJson, FunctionExecutionContext context) {
        try {
            JsonNode args = objectMapper.readTree(argumentsJson);
            String actionStr = args.path("action").asText("");
            String userId = context.userId();

            if (userId == null || userId.isBlank()) {
                return "{\"error\": \"缺少用户ID\"}";
            }

            log.info("CourseImportFunction 执行 | action={} | userId={}", actionStr, userId);

            return switch (actionStr) {
                case "import" -> handleStartImport(userId);
                case "parse" -> handleParse(args, userId);
                case "confirm" -> handleConfirm(userId);
                case "cancel" -> handleCancel(userId);
                case "delete" -> handleDelete(args, userId);
                case "update" -> handleUpdate(args, userId);
                case "clear" -> handleClear(userId);
                case "query_today" -> handleQueryToday(userId);
                case "query_free_time" -> handleQueryFreeTime(userId);
                case "query_all" -> handleQueryAll(userId);
                case "query_weekday" -> handleQueryWeekday(args, userId);
                default -> errorJson("不支持的 action: " + actionStr);
            };
        } catch (Exception e) {
            log.error("CourseImportFunction 执行失败 | args={}", argumentsJson, e);
            return errorJson(e.getMessage());
        }
    }

    // ==================== 导入流程（三步确认） ====================

    private String handleStartImport(String userId) {
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

    private String handleParse(JsonNode args, String userId) {
        CourseImportStateManager.Phase phase = importStateManager.getPhase(userId);
        if (phase == CourseImportStateManager.Phase.NONE) {
            log.debug("直接解析课表（无前置 import 状态）| userId={}", userId);
        }

        JsonNode coursesNode = args.get("courses");
        if (coursesNode == null || !coursesNode.isArray() || coursesNode.isEmpty()) {
            return "{\"action\":\"parse\",\"status\":\"error\","
                    + "\"message\":\"请从对话上下文中的文件/图片分析结果里提取课程信息后重新调用。\"}";
        }

        String jsonStr = coursesNode.toString();
        List<CourseEntity> courses = courseService.parseOnly(userId, jsonStr);

        if (courses.isEmpty()) {
            return "{\"action\":\"parse\",\"status\":\"error\","
                    + "\"message\":\"无法从提供的数据中识别出有效的课程信息，请检查格式或重新上传课表。\"}";
        }

        // 内部冲突检测：检查新解析出的课程间是否有同天同时段冲突
        // 这种冲突通常表示 LLM 的 day_of_week 分配有误
        List<String> internalConflicts = detectInternalDayConflicts(courses);
        for (String conflict : internalConflicts) {
            log.warn("新导入课程间存在同天同时段冲突 | userId={} | {}", userId, conflict);
        }

        // 冲突检测（与已有课表）
        List<CourseService.ConflictInfo> conflicts = courseService.detectConflicts(userId, courses);

        importStateManager.setWaitingConfirm(userId, jsonStr);
        importStateManager.setPendingCourses(userId, courses);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("action", "parse");
        result.put("status", "preview");
        result.put("count", courses.size());

        int currentWeek = semesterConfig.getCurrentWeek();
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

        result.put("formatted_preview",
                CourseMessageFormatter.formatImportPreview(courses, conflicts, currentWeek));

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

    private String handleConfirm(String userId) {
        List<CourseEntity> pending = importStateManager.getPendingCourses(userId);
        if (pending.isEmpty()) {
            return "{\"action\":\"confirm\",\"status\":\"error\","
                    + "\"message\":\"没有待确认的课程数据，请先上传课表。\"}";
        }

        // 保存到数据库（通过 CourseRepository 写入 course_schedule 表）
        List<CourseEntity> saved = courseService.saveCourses(userId, pending);
        importStateManager.clear(userId);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("action", "confirm");
        result.put("status", "success");
        result.put("count", saved.size());

        int currentWeek = semesterConfig.getCurrentWeek();
        result.put("current_week", currentWeek);
        result.put("current_week_display", currentWeek > 0 ? "第" + currentWeek + "周" : "学期未开始");

        result.put("message", "课表导入成功！共 " + saved.size() + " 门课程。"
                + "你可以问我「今天有什么课」来查看今日课程。");
        return result.toString();
    }

    private String handleCancel(String userId) {
        importStateManager.clear(userId);
        return "{\"action\":\"cancel\",\"status\":\"success\",\"message\":\"已取消课表导入。\"}";
    }

    // ==================== 课程管理 ====================

    private String handleDelete(JsonNode args, String userId) {
        long courseId = args.path("course_id").asLong(0);
        if (courseId <= 0) {
            return errorJson("请提供要删除的课程 ID（course_id 参数）");
        }

        CourseEntity course = courseService.findCourseById(courseId);
        if (course == null) {
            return "{\"action\":\"delete\",\"status\":\"error\",\"message\":\"未找到 ID 为 " + courseId + " 的课程\"}";
        }
        if (!userId.equals(course.getUserId())) {
            return "{\"action\":\"delete\",\"status\":\"error\",\"message\":\"无权删除该课程\"}";
        }

        boolean deleted = courseService.deleteCourse(courseId, userId);
        if (deleted) {
            return "{\"action\":\"delete\",\"status\":\"success\","
                    + "\"deleted_course\":\"" + course.getCourseName() + "\","
                    + "\"message\":\"已删除课程「" + course.getCourseName() + "」\"}";
        }
        return "{\"action\":\"delete\",\"status\":\"error\",\"message\":\"删除失败\"}";
    }

    private String handleUpdate(JsonNode args, String userId) {
        long courseId = args.path("course_id").asLong(0);
        if (courseId <= 0) {
            return errorJson("请提供要修改的课程 ID（course_id 参数）");
        }

        CourseEntity existing = courseService.findCourseById(courseId);
        if (existing == null) {
            return "{\"action\":\"update\",\"status\":\"error\",\"message\":\"未找到 ID 为 " + courseId + " 的课程\"}";
        }
        if (!userId.equals(existing.getUserId())) {
            return "{\"action\":\"update\",\"status\":\"error\",\"message\":\"无权修改该课程\"}";
        }

        JsonNode coursesNode = args.get("courses");
        if (coursesNode != null && coursesNode.isArray() && !coursesNode.isEmpty()) {
            JsonNode updateSrc = coursesNode.get(0);
            if (updateSrc.has("course_name")) existing.setCourseName(updateSrc.get("course_name").asText());
            if (updateSrc.has("teacher")) existing.setTeacher(updateSrc.get("teacher").asText());
            if (updateSrc.has("day_of_week")) existing.setDayOfWeek(updateSrc.get("day_of_week").asInt());
            if (updateSrc.has("start_period")) existing.setStartPeriod(updateSrc.get("start_period").asInt());
            if (updateSrc.has("end_period")) existing.setEndPeriod(updateSrc.get("end_period").asInt());
            if (updateSrc.has("classroom")) existing.setClassroom(updateSrc.get("classroom").asText());
            if (updateSrc.has("start_week")) existing.setStartWeek(updateSrc.get("start_week").asInt());
            if (updateSrc.has("end_week")) existing.setEndWeek(updateSrc.get("end_week").asInt());
            if (updateSrc.has("week_type")) existing.setWeekType(updateSrc.get("week_type").asText());
        }

        boolean updated = courseService.updateCourse(existing);
        if (updated) {
            ObjectNode result = objectMapper.createObjectNode();
            result.put("action", "update");
            result.put("status", "success");
            result.put("course_id", existing.getId());
            result.put("course_name", existing.getCourseName());
            result.put("day", existing.getDayDisplay());
            result.put("period", existing.getPeriodDisplay());
            result.put("weeks", existing.getWeekDisplay());
            result.put("message", "已更新课程「" + existing.getCourseName() + "」");
            return result.toString();
        }
        return "{\"action\":\"update\",\"status\":\"error\",\"message\":\"更新失败\"}";
    }

    private String handleClear(String userId) {
        int count = courseService.getCourseCount(userId);
        if (count == 0) {
            return "{\"action\":\"clear\",\"status\":\"success\",\"message\":\"课表已经是空的啦～\"}";
        }

        courseService.deleteAll(userId);
        return "{\"action\":\"clear\",\"status\":\"success\","
                + "\"deleted_count\":" + count + ","
                + "\"message\":\"已清空全部 " + count + " 门课程。\"}";
    }

    // ==================== 查询 ====================

    private String handleQueryToday(String userId) {
        int currentWeek = semesterConfig.getCurrentWeek();
        if (currentWeek <= 0) {
            return buildQueryResult("query_today", List.of(),
                    "学期尚未开始（当前日期早于学期起始日）", currentWeek);
        }

        List<CourseEntity> todayCourses = courseService.getTodayCourses(userId);

        if (todayCourses.isEmpty()) {
            List<CourseEntity> allDayCourses = courseRepository.findByUserIdAndDay(
                    userId, semesterConfig.getCurrentDayOfWeek());
            if (allDayCourses.isEmpty()) {
                return buildQueryResult("query_today", List.of(),
                        "今天没有安排课程，好好休息吧！😊", currentWeek);
            } else {
                return buildQueryResult("query_today", List.of(),
                        "今天虽然有课，但不在当前教学周，所以没有课程安排。当前是第 " + currentWeek + " 周。", currentWeek);
            }
        }

        return buildQueryResult("query_today", todayCourses,
                "今日课程共 " + todayCourses.size() + " 门", currentWeek);
    }

    private String handleQueryFreeTime(String userId) {
        List<CourseService.TimeSlot> freeSlots = courseService.getFreeTimeSlots(userId);

        if (freeSlots.isEmpty()) {
            return "{\"action\":\"query_free_time\",\"slots\":[],\"message\":\"今天全天都有课，没有空闲时间 😅\"}";
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.put("action", "query_free_time");
        var array = result.putArray("slots");
        for (CourseService.TimeSlot slot : freeSlots) {
            array.add(slot.display());
        }
        result.put("message", "今日空闲时间段共 " + freeSlots.size() + " 段");
        result.put("formatted", CourseMessageFormatter.formatFreeTimeSlots(freeSlots));
        return result.toString();
    }

    private String handleQueryAll(String userId) {
        List<CourseEntity> allCourses = courseService.getAllCourses(userId);
        if (allCourses.isEmpty()) {
            return "{\"action\":\"query_all\",\"courses\":[],\"message\":\"你还没有导入课表，快上传课表文件或告诉我课程信息吧！\"}";
        }
        return buildQueryResult("query_all", allCourses,
                "共有 " + allCourses.size() + " 门课程", semesterConfig.getCurrentWeek());
    }

    private String handleQueryWeekday(JsonNode args, String userId) {
        int dayOfWeek = args.path("day_of_week").asInt(0);
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            return "{\"error\":\"无效的 day_of_week 参数，请输入 1（周一）~ 7（周日）\"}";
        }

        int currentWeek = semesterConfig.getCurrentWeek();
        if (currentWeek <= 0) {
            return buildQueryResult("query_weekday", List.of(),
                    "学期尚未开始", currentWeek);
        }

        List<CourseEntity> courses = courseService.getCoursesByDay(userId, dayOfWeek);
        return buildQueryResult("query_weekday", courses,
                DAY_NAMES[dayOfWeek] + "共 " + courses.size() + " 门课", currentWeek);
    }

    // ==================== 工具方法 ====================

    private String buildQueryResult(String action, List<CourseEntity> courses, String message, int currentWeek) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("action", action);
        result.put("current_week", currentWeek);
        result.put("current_week_display", currentWeek > 0 ? "第" + currentWeek + "周" : "假期");

        var array = result.putArray("courses");
        for (CourseEntity c : courses) {
            ObjectNode item = array.addObject();
            item.put("course_name", c.getCourseName());
            item.put("day", c.getDayDisplay());
            item.put("period", c.getPeriodDisplay());
            if (!c.getClassroom().isBlank()) item.put("classroom", c.getClassroom());
            if (!c.getTeacher().isBlank()) item.put("teacher", c.getTeacher());
            item.put("weeks", c.getWeekDisplay());
        }

        result.put("count", courses.size());
        result.put("message", message);

        // 嵌入预格式化的微信消息文本
        String formatted = switch (action) {
            case "query_today" -> CourseMessageFormatter.formatTodayCourses(courses, currentWeek);
            case "query_all" -> CourseMessageFormatter.formatWeekOverview(courses, currentWeek);
            default -> message;
        };
        result.put("formatted", formatted);

        return result.toString();
    }

    private String errorJson(String message) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("error", message);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return "{\"error\":\"" + message + "\"}";
        }
    }
}