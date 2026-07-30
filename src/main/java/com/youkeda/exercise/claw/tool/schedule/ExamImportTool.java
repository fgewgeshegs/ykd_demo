package com.youkeda.exercise.claw.tool.schedule;
import com.youkeda.exercise.claw.schedule.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.runtime.ToolExecutionContext;
import com.youkeda.exercise.claw.agent.runtime.Tool;
import com.youkeda.exercise.claw.agent.runtime.ToolRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExamImportTool implements Tool {
    private static final Logger log = LoggerFactory.getLogger(ExamImportTool.class);
    private final ObjectMapper om;
    private final ToolRegistry registry;
    private final ExamService examService;
    private final ExamRepository examRepository;

    public ExamImportTool(ObjectMapper om, ToolRegistry registry, ExamService examService, ExamRepository examRepository) {
        this.om = om; this.registry = registry; this.examService = examService; this.examRepository = examRepository;
    }

    @PostConstruct
    public void init() { registry.register(this); log.info("ExamImportTool 已注册到 ToolRegistry"); }

    @Override
    public String getName() { return "exam_schedule"; }

    @Override
    public String getDescription() {
        return "考试安排管理。管理用户的考试安排数据（以userId隔离持久化到SQLite）。\n"
                + "支持操作：\n"
                + "- 查询：query_all（全部考试）、query_upcoming（即将到来的考试）\n"
                + "         query_date（指定日期的考试，格式yyyy-MM-dd）\n"
                + "- 导入：使用 courses 参数传入考试JSON数组（覆盖导入）\n"
                + "- 管理：delete（单条删除，需id）、update（修改）、clear（清空全部）\n"
                + "适用于：用户问\"什么时候考试\"\"最近的考试\"\"期末考试安排\"\"导入考试\"等场景。";
    }

    @Override
    public JsonNode getParameters() {
        ObjectNode s = om.createObjectNode(); s.put("type", "object");
        ObjectNode p = s.putObject("properties");
        ObjectNode action = p.putObject("action");
        action.put("type", "string");
        action.put("description", "操作类型");
        action.putArray("enum").add("query_all").add("query_upcoming").add("query_date").add("import").add("delete").add("update").add("clear");
        p.putObject("exam_id").put("type", "integer").put("description", "考试ID，用于 delete/update");
        p.putObject("exam_date").put("type", "string").put("description", "考试日期 yyyy-MM-dd，用于 query_date");
        p.putObject("courses").put("type", "array").put("description", "考试安排数组，用于 import");
        p.putObject("course_name").put("type", "string").put("description", "考试科目（用于 update）");
        p.putObject("new_exam_date").put("type", "string").put("description", "新的考试日期（用于 update）");
        p.putObject("start_time").put("type", "string").put("description", "开始时间 HH:mm（用于 update）");
        p.putObject("end_time").put("type", "string").put("description", "结束时间 HH:mm（用于 update）");
        p.putObject("location").put("type", "string").put("description", "考试地点（用于 update）");
        p.putObject("exam_type").put("type", "string").put("description", "考试类型：MIDTERM/FINAL/MAKEUP（用于 update）");
        s.putArray("required").add("action");
        return s;
    }

    @Override
    public String execute(String argumentsJson) {
        return "{\"error\": \"缺少用户上下文\"}";
    }

    @Override
    public String execute(String argumentsJson, ToolExecutionContext ctx) {
        String userId = ctx.userId();
        if (userId == null || userId.isBlank()) return "{\"error\":\"缺少用户ID\"}";
        JsonNode args;
        try {
            args = om.readTree(argumentsJson);
        } catch (Exception e) {
            return "{\"error\":\"参数解析失败\"}";
        }
        String action = args.path("action").asText("");
        try {
            return switch (action) {
                case "query_all" -> handleAll(userId);
                case "query_upcoming" -> handleUpcoming(userId);
                case "query_date" -> handleDate(args, userId);
                case "import" -> handleImport(args, userId);
                case "delete" -> handleDelete(args, userId);
                case "update" -> handleUpdate(args, userId);
                case "clear" -> handleClear(userId);
                default -> "{\"error\":\"未知操作\",\"action\":\"" + action + "\"}";
            };
        } catch (Exception e) {
            log.error("考试操作异常 | userId={} | action={}", userId, action, e);
            return "{\"error\":\"系统异常\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    private String buildResult(String action, List<ExamEntity> exams, String title) {
        ObjectNode r = om.createObjectNode();
        r.put("action", action); r.put("title", title); r.put("count", exams.size());
        ArrayNode arr = r.putArray("exams");
        for (ExamEntity e : exams) {
            ObjectNode item = arr.addObject();
            item.put("id", e.getId()); item.put("course_name", e.getCourseName());
            item.put("exam_date", e.getExamDate()); item.put("date_display", e.getDateDisplay());
            item.put("time_display", e.getTimeDisplay()); item.put("start_time", e.getStartTime());
            item.put("end_time", e.getEndTime()); item.put("exam_type", e.getExamTypeDisplay());
            if (!e.getLocation().isBlank()) item.put("location", e.getLocation());
            if (!e.getSeatNumber().isBlank()) item.put("seat_number", e.getSeatNumber());
        }
        r.put("message", exams.isEmpty() ? "暂未找到考试安排" : "共 " + exams.size() + " 条考试安排");
        return r.toString();
    }

    private String handleAll(String uid) { return buildResult("query_all", examService.getAllExams(uid), "全部考试"); }
    private String handleUpcoming(String uid) { return buildResult("query_upcoming", examService.getUpcomingExams(uid), "即将到来的考试"); }
    private String handleDate(JsonNode a, String uid) {
        String d = a.path("exam_date").asText(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        return buildResult("query_date", examService.getExamsByDate(uid, d), d + " 考试");
    }
    private String handleImport(JsonNode a, String uid) {
        JsonNode courses = a.path("courses");
        if (courses.isMissingNode() || !courses.isArray() || courses.isEmpty())
            return "{\"action\":\"import\",\"error\":\"缺少考试数据\"}";
        List<ExamEntity> exams = new ArrayList<>();
        for (JsonNode n : courses) { ExamEntity e = parseExam(n); if (e != null) exams.add(e); }
        if (exams.isEmpty()) return "{\"action\":\"import\",\"error\":\"解析失败\"}";
        List<ExamEntity> saved = examService.saveExams(uid, exams);
        return buildResult("import", saved, "已导入 " + saved.size() + " 条考试安排");
    }
    private String handleDelete(JsonNode a, String uid) {
        long id = a.path("exam_id").asLong(-1);
        if (id <= 0) return "{\"action\":\"delete\",\"error\":\"缺少考试ID\"}";
        return examService.deleteExam(id, uid) ? "{\"action\":\"delete\",\"success\":true,\"message\":\"考试已删除\"}"
                : "{\"action\":\"delete\",\"success\":false,\"message\":\"未找到该考试\"}";
    }
    private String handleUpdate(JsonNode a, String uid) {
        long id = a.path("exam_id").asLong(-1);
        if (id <= 0) return "{\"action\":\"update\",\"error\":\"缺少考试ID\"}";
        ExamEntity e = examService.findExamById(id);
        if (e == null || !uid.equals(e.getUserId())) return "{\"action\":\"update\",\"success\":false,\"message\":\"未找到或无权修改\"}";
        if (a.has("course_name")) e.setCourseName(a.get("course_name").asText());
        if (a.has("new_exam_date")) e.setExamDate(a.get("new_exam_date").asText());
        if (a.has("start_time")) e.setStartTime(a.get("start_time").asText());
        if (a.has("end_time")) e.setEndTime(a.get("end_time").asText());
        if (a.has("location")) e.setLocation(a.get("location").asText());
        if (a.has("exam_type")) e.setExamType(a.get("exam_type").asText());
        return examService.updateExam(e) ? "{\"action\":\"update\",\"success\":true,\"message\":\"考试已更新\"}"
                : "{\"action\":\"update\",\"success\":false,\"message\":\"更新失败\"}";
    }
    private String handleClear(String uid) {
        int c = examService.getExamCount(uid); examService.deleteAll(uid);
        return "{\"action\":\"clear\",\"success\":true,\"deleted\":" + c + ",\"message\":\"已清空 " + c + " 条考试安排\"}";
    }
    private ExamEntity parseExam(JsonNode n) {
        String name = getText(n, "course_name","courseName","name","科目");
        String date = getText(n, "exam_date","examDate","date","日期");
        if (name == null || date == null) return null;
        String st = getText(n, "start_time","startTime","开始时间");
        String et = getText(n, "end_time","endTime","结束时间");
        String loc = getText(n, "location","classroom","room","地点");
        String seat = getText(n, "seat_number","seatNumber","座位号");
        String type = parseType(getText(n, "exam_type","examType","type","考试类型"));
        ExamEntity e = new ExamEntity(null, name, date, st, et, loc, type);
        if (seat != null) e.setSeatNumber(seat);
        return e;
    }
    private String getText(JsonNode n, String... keys) { for (String k : keys) { JsonNode v = n.get(k); if (v != null && v.isTextual() && !v.asText().isBlank()) return v.asText().trim(); } return null; }
    private String parseType(String t) { if (t == null) return "FINAL"; String l = t.trim().toLowerCase(); if (l.contains("期中")||l.equals("midterm")) return "MIDTERM"; if (l.contains("补考")||l.equals("makeup")) return "MAKEUP"; return "FINAL"; }
}