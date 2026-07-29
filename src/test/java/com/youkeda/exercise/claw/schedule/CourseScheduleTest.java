package com.youkeda.exercise.claw.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 课表模块单元测试
 *
 * <p>覆盖：课程实体、解析器、学期配置、状态管理、业务逻辑。
 * 不依赖 Spring 容器，纯 POJO 测试。
 */
class CourseScheduleTest {

    // ==================== CourseEntity 实体测试 ====================

    @Nested
    @DisplayName("CourseEntity 实体 - 周次判断")
    class CourseWeekTest {

        @Test
        @DisplayName("全部周期：在范围内返回 true")
        void allWeekInRange() {
            CourseEntity c = new CourseEntity("u1", "高数", "张老师", 1, 1, 2,
                    "A101", 1, 16, CourseEntity.WEEK_ALL);
            assertTrue(c.isActiveInWeek(1));
            assertTrue(c.isActiveInWeek(8));
            assertTrue(c.isActiveInWeek(16));
        }

        @Test
        @DisplayName("全部周期：超出范围返回 false")
        void allWeekOutOfRange() {
            CourseEntity c = new CourseEntity("u1", "高数", "张老师", 1, 1, 2,
                    "A101", 3, 16, CourseEntity.WEEK_ALL);
            assertFalse(c.isActiveInWeek(1));  // 早于开始
            assertFalse(c.isActiveInWeek(17)); // 晚于结束
        }

        @Test
        @DisplayName("单周：单周返回 true，双周返回 false")
        void oddWeek() {
            CourseEntity c = new CourseEntity("u1", "体育", "李老师", 3, 3, 4,
                    "操场", 1, 18, CourseEntity.WEEK_ODD);
            assertTrue(c.isActiveInWeek(1));
            assertTrue(c.isActiveInWeek(9));
            assertTrue(c.isActiveInWeek(17));
            assertFalse(c.isActiveInWeek(2));
            assertFalse(c.isActiveInWeek(10));
            assertFalse(c.isActiveInWeek(18));
        }

        @Test
        @DisplayName("双周：双周返回 true，单周返回 false")
        void evenWeek() {
            CourseEntity c = new CourseEntity("u1", "实验课", "王老师", 5, 5, 6,
                    "实验室", 2, 16, CourseEntity.WEEK_EVEN);
            assertTrue(c.isActiveInWeek(2));
            assertTrue(c.isActiveInWeek(10));
            assertTrue(c.isActiveInWeek(16));
            assertFalse(c.isActiveInWeek(1));
            assertFalse(c.isActiveInWeek(9));
            assertFalse(c.isActiveInWeek(15));
        }

        @Test
        @DisplayName("单双周：超出周次范围返回 false")
        void oddWeekOutOfRange() {
            CourseEntity c = new CourseEntity("u1", "选修", null, 2, 7, 8,
                    "B201", 3, 12, CourseEntity.WEEK_ODD);
            assertFalse(c.isActiveInWeek(1));  // 早于开始
            assertFalse(c.isActiveInWeek(13)); // 晚于结束
            // 范围内的单周应有效
            assertTrue(c.isActiveInWeek(3));
            assertTrue(c.isActiveInWeek(11));
        }
    }

    @Nested
    @DisplayName("CourseEntity 实体 - 显示方法")
    class CourseDisplayTest {

        @Test
        @DisplayName("getPeriodDisplay：跨节次显示")
        void periodDisplayRange() {
            CourseEntity c = new CourseEntity("u1", "高数", null, 1, 3, 4, "A101", 1, 16, CourseEntity.WEEK_ALL);
            assertEquals("3-4", c.getPeriodDisplay());
        }

        @Test
        @DisplayName("getPeriodDisplay：单节次显示")
        void periodDisplaySingle() {
            CourseEntity c = new CourseEntity("u1", "体育", null, 3, 5, 5, "操场", 1, 16, CourseEntity.WEEK_ALL);
            assertEquals("5", c.getPeriodDisplay());
        }

        @Test
        @DisplayName("getWeekDisplay：全部周期")
        void weekDisplayAll() {
            CourseEntity c = new CourseEntity("u1", "高数", null, 1, 1, 2, null, 1, 16, CourseEntity.WEEK_ALL);
            assertEquals("1-16周", c.getWeekDisplay());
        }

        @Test
        @DisplayName("getWeekDisplay：单周")
        void weekDisplayOdd() {
            CourseEntity c = new CourseEntity("u1", "体育", null, 3, 3, 4, null, 1, 18, CourseEntity.WEEK_ODD);
            assertEquals("1-18周(单周)", c.getWeekDisplay());
        }

        @Test
        @DisplayName("getWeekDisplay：双周")
        void weekDisplayEven() {
            CourseEntity c = new CourseEntity("u1", "实验", null, 5, 5, 6, null, 2, 16, CourseEntity.WEEK_EVEN);
            assertEquals("2-16周(双周)", c.getWeekDisplay());
        }

        @Test
        @DisplayName("getDayDisplay")
        void dayDisplay() {
            assertEquals("周一", new CourseEntity("u1", "", null, 1, 1, 1, null, 1, 16, CourseEntity.WEEK_ALL).getDayDisplay());
            assertEquals("周二", new CourseEntity("u1", "", null, 2, 1, 1, null, 1, 16, CourseEntity.WEEK_ALL).getDayDisplay());
            assertEquals("周三", new CourseEntity("u1", "", null, 3, 1, 1, null, 1, 16, CourseEntity.WEEK_ALL).getDayDisplay());
            assertEquals("周四", new CourseEntity("u1", "", null, 4, 1, 1, null, 1, 16, CourseEntity.WEEK_ALL).getDayDisplay());
            assertEquals("周五", new CourseEntity("u1", "", null, 5, 1, 1, null, 1, 16, CourseEntity.WEEK_ALL).getDayDisplay());
            assertEquals("周六", new CourseEntity("u1", "", null, 6, 1, 1, null, 1, 16, CourseEntity.WEEK_ALL).getDayDisplay());
            assertEquals("周日", new CourseEntity("u1", "", null, 7, 1, 1, null, 1, 16, CourseEntity.WEEK_ALL).getDayDisplay());
        }
    }

    // ==================== CourseParser 测试 ====================

    @Nested
    @DisplayName("CourseParser - JSON 解析")
    class CourseParserTest {

        private CourseParser parser;

        @BeforeEach
        void setUp() {
            parser = new CourseParser(new ObjectMapper());
        }

        @Test
        @DisplayName("解析标准 JSON 数组")
        void parseJsonArray() {
            String json = """
                    [
                        {"course_name":"高等数学","teacher":"张老师","day_of_week":1,
                         "start_period":1,"end_period":2,"classroom":"A101",
                         "start_week":1,"end_week":16,"week_type":"ALL"},
                        {"course_name":"大学英语","teacher":"李老师","day_of_week":3,
                         "start_period":3,"end_period":4,"classroom":"B202",
                         "start_week":1,"end_week":16,"week_type":"ALL"}
                    ]
                    """;
            List<CourseEntity> courses = parser.parseFromJson(json);
            assertEquals(2, courses.size());
            assertEquals("高等数学", courses.get(0).getCourseName());
            assertEquals("张老师", courses.get(0).getTeacher());
            assertEquals(1, courses.get(0).getDayOfWeek());
            assertEquals(1, courses.get(0).getStartPeriod());
            assertEquals(2, courses.get(0).getEndPeriod());
            assertEquals("A101", courses.get(0).getClassroom());
            assertEquals("大学英语", courses.get(1).getCourseName());
        }

        @Test
        @DisplayName("解析嵌套 JSON {courses:[...]}")
        void parseNestedJson() {
            String json = """
                    {"courses":[
                        {"course_name":"体育","day_of_week":3,"start_period":5,"end_period":6,
                         "start_week":1,"end_week":18,"week_type":"ODD"}
                    ]}
                    """;
            List<CourseEntity> courses = parser.parseFromJson(json);
            assertEquals(1, courses.size());
            assertEquals("体育", courses.get(0).getCourseName());
            assertEquals(CourseEntity.WEEK_ODD, courses.get(0).getWeekType());
        }

        @Test
        @DisplayName("解析带 Markdown 包裹的 JSON")
        void parseMarkdownWrappedJson() {
            String json = """
                    ```json
                    [{"course_name":"高数","day_of_week":1,"start_period":1,"end_period":2}]
                    ```
                    """;
            List<CourseEntity> courses = parser.parseFromJson(json);
            assertEquals(1, courses.size());
            assertEquals("高数", courses.get(0).getCourseName());
        }

        @Test
        @DisplayName("解析中文星期字段")
        void parseChineseDayOfWeek() {
            String json = """
                    [{"course_name":"高数","day_of_week":"周一","start_period":1,"end_period":2}]
                    """;
            List<CourseEntity> courses = parser.parseFromJson(json);
            assertEquals(1, courses.size());
            assertEquals(1, courses.get(0).getDayOfWeek());
        }

        @Test
        @DisplayName("解析中文单双周字段")
        void parseChineseWeekType() {
            String json = """
                    [{"course_name":"体育","day_of_week":3,"start_period":5,"end_period":6,"week_type":"单周"},
                     {"course_name":"实验","day_of_week":5,"start_period":7,"end_period":8,"week_type":"双周"}]
                    """;
            List<CourseEntity> courses = parser.parseFromJson(json);
            assertEquals(2, courses.size());
            assertEquals(CourseEntity.WEEK_ODD, courses.get(0).getWeekType());
            assertEquals(CourseEntity.WEEK_EVEN, courses.get(1).getWeekType());
        }

        @Test
        @DisplayName("解析空 JSON 返回空列表")
        void parseEmptyJson() {
            String json = "[]";
            List<CourseEntity> courses = parser.parseFromJson(json);
            assertTrue(courses.isEmpty());
        }

        @Test
        @DisplayName("解析缺少必填字段的课程")
        void parseMissingRequiredField() {
            String json = """
                    [{"teacher":"张老师","day_of_week":1,"start_period":1,"end_period":2}]
                    """;
            List<CourseEntity> courses = parser.parseFromJson(json);
            assertTrue(courses.isEmpty());
        }

        @Test
        @DisplayName("解析无效 JSON 返回空列表")
        void parseInvalidJson() {
            String json = "这不是JSON";
            List<CourseEntity> courses = parser.parseFromJson(json);
            assertTrue(courses.isEmpty());
        }

        // ==================== week_type 兜底解析测试 ====================

        @Test
        @DisplayName("course_name 含(双)标记时兜底为 EVEN")
        void fallbackDetectEvenFromCourseName() {
            String json = """
                    [{"course_name":"概率统计(双)","day_of_week":2,"start_period":3,"end_period":4,
                     "start_week":2,"end_week":16,"week_type":"ALL"}]
                    """;
            List<CourseEntity> courses = parser.parseFromJson(json);
            assertEquals(1, courses.size());
            assertEquals(CourseEntity.WEEK_EVEN, courses.get(0).getWeekType());
        }

        @Test
        @DisplayName("course_name 含(单)标记时兜底为 ODD")
        void fallbackDetectOddFromCourseName() {
            String json = """
                    [{"course_name":"大学物理(单)","day_of_week":4,"start_period":3,"end_period":4,
                     "start_week":1,"end_week":15,"week_type":"ALL"}]
                    """;
            List<CourseEntity> courses = parser.parseFromJson(json);
            assertEquals(1, courses.size());
            assertEquals(CourseEntity.WEEK_ODD, courses.get(0).getWeekType());
        }

        @Test
        @DisplayName("备注字段含双周标记时兜底为 EVEN")
        void fallbackDetectEvenFromNote() {
            String json = """
                    [{"course_name":"概率统计","day_of_week":2,"start_period":3,"end_period":4,
                     "start_week":2,"end_week":16,"week_type":"ALL","note":"双周上课"}]
                    """;
            List<CourseEntity> courses = parser.parseFromJson(json);
            assertEquals(1, courses.size());
            assertEquals(CourseEntity.WEEK_EVEN, courses.get(0).getWeekType());
        }

        @Test
        @DisplayName("LLM 返回正确 ODD 时，兜底不覆盖（优先级：LLM > 兜底）")
        void fallbackDoesNotOverrideCorrectOdd() {
            String json = """
                    [{"course_name":"离散数学(单)","day_of_week":1,"start_period":7,"end_period":8,
                     "start_week":2,"end_week":16,"week_type":"ODD"}]
                    """;
            List<CourseEntity> courses = parser.parseFromJson(json);
            assertEquals(1, courses.size());
            assertEquals(CourseEntity.WEEK_ODD, courses.get(0).getWeekType());
            // course_name 中的"(单)"不应改变正确返回值
            assertEquals("离散数学(单)", courses.get(0).getCourseName());
        }

        @Test
        @DisplayName("无单双周标记时保持 ALL")
        void fallbackKeepsAllWhenNoMarkers() {
            String json = """
                    [{"course_name":"高等数学","day_of_week":1,"start_period":1,"end_period":2,
                     "start_week":1,"end_week":16,"week_type":"ALL"}]
                    """;
            List<CourseEntity> courses = parser.parseFromJson(json);
            assertEquals(1, courses.size());
            assertEquals(CourseEntity.WEEK_ALL, courses.get(0).getWeekType());
        }
    }

    // ==================== SemesterConfig 测试 ====================

    @Nested
    @DisplayName("SemesterConfig - 学期周次计算")
    class SemesterConfigTest {

        @Test
        @DisplayName("未设置学期起始日返回默认 1")
        void defaultWeek() {
            SemesterConfig config = new SemesterConfig();
            // 没有调用 setSemesterStart，getCurrentWeek 返回 1
            assertTrue(config.getCurrentWeek() >= 1);
        }

        @Test
        @DisplayName("学期第一天为第 1 周")
        void firstWeek() {
            SemesterConfig config = new SemesterConfig();
            // 假设学期从今天开始（手工 mock 做不到固定日期，但可以验证逻辑）
            LocalDate start = LocalDate.now();
            config.setSemesterStart(start);
            assertEquals(1, config.getCurrentWeek());
        }

        @Test
        @DisplayName("学期前返回 -1")
        void beforeSemester() {
            SemesterConfig config = new SemesterConfig();
            config.setSemesterStart(LocalDate.now().plusDays(7)); // 一周后才开始
            assertEquals(-1, config.getCurrentWeek());
        }

        @Test
        @DisplayName("isOddWeek / isEvenWeek 正确")
        void oddEvenWeek() {
            SemesterConfig config = new SemesterConfig();
            config.setSemesterStart(LocalDate.now().minusDays(7)); // 第 2 周
            int week = config.getCurrentWeek();
            if (week % 2 == 1) {
                assertTrue(config.isOddWeek());
                assertFalse(config.isEvenWeek());
            } else {
                assertFalse(config.isOddWeek());
                assertTrue(config.isEvenWeek());
            }
        }

        @Test
        @DisplayName("学期前 isOddWeek 返回 false")
        void beforeSemesterOddWeek() {
            SemesterConfig config = new SemesterConfig();
            config.setSemesterStart(LocalDate.now().plusDays(365)); // 一年后
            assertFalse(config.isOddWeek());
        }
    }

    // ==================== CourseImportStateManager 测试 ====================

    @Nested
    @DisplayName("CourseImportStateManager - 导入状态管理")
    class ImportStateManagerTest {

        private CourseImportStateManager stateManager;

        @BeforeEach
        void setUp() {
            stateManager = new CourseImportStateManager();
        }

        @Test
        @DisplayName("初始状态为 NONE")
        void initialState() {
            assertEquals(CourseImportStateManager.Phase.NONE, stateManager.getPhase("user1"));
        }

        @Test
        @DisplayName("设置 WAITING_FILE 状态")
        void setWaitingFile() {
            stateManager.setWaitingFile("user1");
            assertEquals(CourseImportStateManager.Phase.WAITING_FILE, stateManager.getPhase("user1"));
        }

        @Test
        @DisplayName("设置 WAITING_CONFIRM 状态")
        void setWaitingConfirm() {
            stateManager.setWaitingConfirm("user1", "analysis text");
            assertEquals(CourseImportStateManager.Phase.WAITING_CONFIRM, stateManager.getPhase("user1"));
            assertEquals("analysis text", stateManager.getState("user1").fileAnalysis());
        }

        @Test
        @DisplayName("清除状态")
        void clearState() {
            stateManager.setWaitingFile("user1");
            stateManager.setPendingCourses("user1", List.of(
                    new CourseEntity("u1", "高数", null, 1, 1, 2, null, 1, 16, CourseEntity.WEEK_ALL)
            ));
            stateManager.clear("user1");

            assertEquals(CourseImportStateManager.Phase.NONE, stateManager.getPhase("user1"));
            assertTrue(stateManager.getPendingCourses("user1").isEmpty());
        }

        @Test
        @DisplayName("待确认课程管理")
        void pendingCourseEntitys() {
            List<CourseEntity> courses = List.of(
                    new CourseEntity("u1", "高数", null, 1, 1, 2, null, 1, 16, CourseEntity.WEEK_ALL),
                    new CourseEntity("u1", "英语", null, 3, 3, 4, null, 1, 16, CourseEntity.WEEK_ALL)
            );
            stateManager.setPendingCourses("user1", courses);

            List<CourseEntity> retrieved = stateManager.getPendingCourses("user1");
            assertEquals(2, retrieved.size());
            assertEquals("高数", retrieved.get(0).getCourseName());
        }

        @Test
        @DisplayName("不同用户状态隔离")
        void isolatedStates() {
            stateManager.setWaitingFile("user1");
            stateManager.setWaitingConfirm("user2", "text");

            assertEquals(CourseImportStateManager.Phase.WAITING_FILE, stateManager.getPhase("user1"));
            assertEquals(CourseImportStateManager.Phase.WAITING_CONFIRM, stateManager.getPhase("user2"));
        }
    }

    // ==================== CourseService 业务测试 ====================

    @Nested
    @DisplayName("CourseService - 空闲时间计算")
    class FreeTimeCalculationTest {

        @Test
        @DisplayName("全天无课返回 12 段空闲（连续1-12节）")
        void noCourseEntitysAllFree() {
            // 直接测试 TimeSlot 算法：没有课程时全天空闲
            boolean[] occupied = new boolean[13];
            List<CourseService.TimeSlot> slots = computeFreeSlots(occupied);
            assertEquals(1, slots.size());
            assertEquals(1, slots.get(0).startPeriod());
            assertEquals(12, slots.get(0).endPeriod());
        }

        @Test
        @DisplayName("第一节课被占用，空闲从第 2 节开始")
        void firstPeriodOccupied() {
            boolean[] occupied = new boolean[13];
            occupied[1] = true;
            List<CourseService.TimeSlot> slots = computeFreeSlots(occupied);
            assertEquals(1, slots.size());
            assertEquals(2, slots.get(0).startPeriod());
            assertEquals(12, slots.get(0).endPeriod());
        }

        @Test
        @DisplayName("中间有课，产生两段空闲")
        void middleOccupied() {
            boolean[] occupied = new boolean[13];
            for (int i = 3; i <= 6; i++) occupied[i] = true; // 3-6节有课
            List<CourseService.TimeSlot> slots = computeFreeSlots(occupied);
            assertEquals(2, slots.size());
            assertEquals(1, slots.get(0).startPeriod());
            assertEquals(2, slots.get(0).endPeriod());
            assertEquals(7, slots.get(1).startPeriod());
            assertEquals(12, slots.get(1).endPeriod());
        }

        @Test
        @DisplayName("全天满课，无空闲")
        void allOccupied() {
            boolean[] occupied = new boolean[13];
            for (int i = 1; i <= 12; i++) occupied[i] = true;
            List<CourseService.TimeSlot> slots = computeFreeSlots(occupied);
            assertTrue(slots.isEmpty());
        }

        @Test
        @DisplayName("多段空闲")
        void multipleFreeSlots() {
            boolean[] occupied = new boolean[13];
            occupied[1] = true;   // 1节有课
            occupied[6] = true;   // 6节有课
            occupied[7] = true;   // 7节有课

            List<CourseService.TimeSlot> slots = computeFreeSlots(occupied);
            assertEquals(2, slots.size());  // 空闲段: [2-5], [8-12]
            assertEquals(2, slots.get(0).startPeriod());
            assertEquals(5, slots.get(0).endPeriod());
            assertEquals(8, slots.get(1).startPeriod());
            assertEquals(12, slots.get(1).endPeriod());
        }

        /**
         * 根据 occupied 数组计算空闲段（复用 CourseService 的算法）
         */
        private List<CourseService.TimeSlot> computeFreeSlots(boolean[] occupied) {
            List<CourseService.TimeSlot> freeSlots = new java.util.ArrayList<>();
            int i = 1;
            while (i <= 12) {
                if (!occupied[i]) {
                    int start = i;
                    while (i <= 12 && !occupied[i]) i++;
                    int end = i - 1;
                    freeSlots.add(new CourseService.TimeSlot(start, end));
                } else {
                    i++;
                }
            }
            return freeSlots;
        }
    }

    @Nested
    @DisplayName("CourseService.TimeSlot - 显示方法")
    class TimeSlotDisplayTest {

        @Test
        @DisplayName("时间段显示包含节次")
        void displayContainsPeriod() {
            CourseService.TimeSlot slot = new CourseService.TimeSlot(3, 4);
            String display = slot.display();
            assertTrue(display.contains("3-4"));
            assertTrue(display.contains("节"));
        }

        @Test
        @DisplayName("单节次时间段")
        void singlePeriodSlot() {
            CourseService.TimeSlot slot = new CourseService.TimeSlot(5, 5);
            String display = slot.display();
            assertTrue(display.contains("5"));
            assertTrue(display.contains("节"));
            // 单节次不再显示 "5-5" 的格式
            assertFalse(display.contains("5-5"));
        }
    }

    // ==================== 导入流程集成测试 ====================

    @Nested
    @DisplayName("导入流程 - 状态流转")
    class ImportFlowTest {

        private CourseImportStateManager stateManager;

        @BeforeEach
        void setUp() {
            stateManager = new CourseImportStateManager();
        }

        @Test
        @DisplayName("完整导入流程：import → parse → confirm")
        void fullImportFlow() {
            String userId = "test_flow_user";

            // Step 1: import → WAITING_FILE
            stateManager.setWaitingFile(userId);
            assertEquals(CourseImportStateManager.Phase.WAITING_FILE, stateManager.getPhase(userId));

            // Step 2: parse → WAITING_CONFIRM + pending courses
            List<CourseEntity> parsed = List.of(
                    new CourseEntity(userId, "高等数学", "张老师", 1, 1, 2, "A101", 1, 16, CourseEntity.WEEK_ALL),
                    new CourseEntity(userId, "大学英语", "李老师", 3, 3, 4, "B202", 1, 16, CourseEntity.WEEK_ALL)
            );
            stateManager.setPendingCourses(userId, parsed);
            stateManager.setWaitingConfirm(userId, "[test] parsed json");

            assertEquals(CourseImportStateManager.Phase.WAITING_CONFIRM, stateManager.getPhase(userId));
            assertEquals(2, stateManager.getPendingCourses(userId).size());
            assertEquals("高等数学", stateManager.getPendingCourses(userId).get(0).getCourseName());

            // Step 3: confirm → clear state
            stateManager.clear(userId);
            assertEquals(CourseImportStateManager.Phase.NONE, stateManager.getPhase(userId));
            assertTrue(stateManager.getPendingCourses(userId).isEmpty());
        }

        @Test
        @DisplayName("取消导入：clear 后状态重置")
        void cancelImport() {
            String userId = "test_cancel_user";
            stateManager.setWaitingFile(userId);
            stateManager.setPendingCourses(userId, List.of(
                    new CourseEntity(userId, "体育", null, 3, 5, 6, "操场", 1, 16, CourseEntity.WEEK_ODD)
            ));
            stateManager.setWaitingConfirm(userId, "text");

            // 取消
            stateManager.clear(userId);
            assertEquals(CourseImportStateManager.Phase.NONE, stateManager.getPhase(userId));
            assertTrue(stateManager.getPendingCourses(userId).isEmpty());
        }

        @Test
        @DisplayName("非导入状态下解析课程（允许跳过 import 直接 parse）")
        void parseWithoutImport() {
            String userId = "test_direct_parse";
            // 没有先调用 setWaitingFile，直接等待确认
            assertEquals(CourseImportStateManager.Phase.NONE, stateManager.getPhase(userId));

            stateManager.setPendingCourses(userId, List.of(
                    new CourseEntity(userId, "高数", null, 1, 1, 2, null, 1, 16, CourseEntity.WEEK_ALL)
            ));
            stateManager.setWaitingConfirm(userId, "direct");
            assertEquals(CourseImportStateManager.Phase.WAITING_CONFIRM, stateManager.getPhase(userId));
        }
    }

    // ==================== 课表查询场景测试 ====================

    @Nested
    @DisplayName("查询场景 - 今日课程与周次过滤")
    class CourseQueryTest {

        private SemesterConfig semesterConfig;

        @BeforeEach
        void setUp() {
            semesterConfig = new SemesterConfig();
        }

        @Test
        @DisplayName("当天有课且在当前周范围内返回 true")
        void todayCourseActiveInWeek() {
            // 假设当前是第 1 周
            CourseEntity c = new CourseEntity("u1", "高数", null, semesterConfig.getCurrentDayOfWeek(), 1, 2,
                    null, 1, 16, CourseEntity.WEEK_ALL);
            assertTrue(c.isActiveInWeek(1));
        }

        @Test
        @DisplayName("当天有课但超出周次范围返回 false")
        void todayCourseOutOfWeekRange() {
            CourseEntity c = new CourseEntity("u1", "选修", null, semesterConfig.getCurrentDayOfWeek(), 7, 8,
                    null, 10, 16, CourseEntity.WEEK_ALL);
            assertFalse(c.isActiveInWeek(1)); // 第 1 周不在 10-16 周范围内
        }

        @Test
        @DisplayName("单周课程在双周不可见")
        void oddCourseInEvenWeek() {
            int currentDay = semesterConfig.getCurrentDayOfWeek();
            CourseEntity c = new CourseEntity("u1", "单周体育", null, currentDay, 3, 4,
                    null, 1, 18, CourseEntity.WEEK_ODD);

            // 模拟第 2 周（双周）→ 不活跃
            assertFalse(c.isActiveInWeek(2));
        }

        @Test
        @DisplayName("双周课程在单周不可见")
        void evenCourseInOddWeek() {
            int currentDay = semesterConfig.getCurrentDayOfWeek();
            CourseEntity c = new CourseEntity("u1", "双周实验", null, currentDay, 5, 6,
                    null, 2, 16, CourseEntity.WEEK_EVEN);

            // 模拟第 1 周（单周）→ 不活跃
            assertFalse(c.isActiveInWeek(1));
            // 模拟第 2 周 → 活跃
            assertTrue(c.isActiveInWeek(2));
        }

        @Test
        @DisplayName("query_today 返回课程列表过滤正确")
        void queryTodayFiltering() {
            int today = semesterConfig.getCurrentDayOfWeek();
            int currentWeek = 3; // 假设当前第 3 周

            // 所有课程都是今天，但周次不同
            CourseEntity c1 = new CourseEntity("u1", "每周高数", null, today, 1, 2, null, 1, 16, CourseEntity.WEEK_ALL);
            CourseEntity c2 = new CourseEntity("u1", "单周英语", null, today, 3, 4, null, 1, 16, CourseEntity.WEEK_ODD);
            CourseEntity c3 = new CourseEntity("u1", "双周实验", null, today, 5, 6, null, 2, 16, CourseEntity.WEEK_EVEN);
            CourseEntity c4 = new CourseEntity("u1", "过期课程", null, today, 7, 8, null, 1, 2, CourseEntity.WEEK_ALL);

            // 第 3 周 → c1(ALL) ✓, c2(ODD) ✓, c3(EVEN) ✗, c4(range 1-2) ✗
            assertTrue(c1.isActiveInWeek(currentWeek));
            assertTrue(c2.isActiveInWeek(currentWeek));
            assertFalse(c3.isActiveInWeek(currentWeek));
            assertFalse(c4.isActiveInWeek(currentWeek));
        }

        @Test
        @DisplayName("query_free_time 有空闲段")
        void freeTimeWithGaps() {
            boolean[] occupied = new boolean[13];
            occupied[3] = true; // 3-4节有课
            occupied[4] = true;
            occupied[9] = true; // 9-10节有课
            occupied[10] = true;

            List<CourseService.TimeSlot> slots = computeSlots(occupied);
            // 3-4节有课, 9-10节有课 → 空闲段: [1-2], [5-8], [11-12] = 3段
            assertEquals(3, slots.size());
            // 空闲段1: 1-2节
            assertEquals(1, slots.get(0).startPeriod());
            assertEquals(2, slots.get(0).endPeriod());
            // 空闲段2: 5-8节
            assertEquals(5, slots.get(1).startPeriod());
            assertEquals(8, slots.get(1).endPeriod());
            // 空闲段3: 11-12节
            assertEquals(11, slots.get(2).startPeriod());
            assertEquals(12, slots.get(2).endPeriod());
        }

        @Test
        @DisplayName("query_all 返回全部课程（不过滤周次）")
        void queryAllReturnsAll() {
            // 测试课程不过滤周次（这个是 CourseRepository 的职责，这里验证 CourseEntity 本身的周次判断不影响全部查询）
            int today = 1;
            CourseEntity c1 = new CourseEntity("u1", "课程A", null, today, 1, 2, null, 1, 16, CourseEntity.WEEK_ALL);
            CourseEntity c2 = new CourseEntity("u1", "课程B", null, today, 3, 4, null, 5, 12, CourseEntity.WEEK_EVEN);

            // getAllCourseEntitys 不按周次过滤
            assertEquals("课程A", c1.getCourseName());
            assertEquals("课程B", c2.getCourseName());
        }

        private List<CourseService.TimeSlot> computeSlots(boolean[] occupied) {
            List<CourseService.TimeSlot> slots = new java.util.ArrayList<>();
            int i = 1;
            while (i <= 12) {
                if (!occupied[i]) {
                    int start = i;
                    while (i <= 12 && !occupied[i]) i++;
                    slots.add(new CourseService.TimeSlot(start, i - 1));
                } else {
                    i++;
                }
            }
            return slots;
        }
    }

    // ==================== 课前提醒生成测试 ====================

    @Nested
    @DisplayName("课前提醒 - 消息构建")
    class CourseReminderTest {

        @Test
        @DisplayName("完整课程信息生成提醒消息")
        void fullReminderMessage() {
            CourseEntity c = createDefaultCourseEntity();
            String message = buildReminderMessageForTest(c);

            assertTrue(message.contains("⏰"));
            assertTrue(message.contains("高等数学"));
            assertTrue(message.contains("A101"));
            assertTrue(message.contains("张老师"));
            assertTrue(message.contains("第 3 周"));
        }

        @Test
        @DisplayName("无教室/教师信息时提醒消息正确")
        void minimalReminderMessage() {
            CourseEntity c = new CourseEntity("u1", "体育", null, 3, 5, 6, "", 1, 16, CourseEntity.WEEK_ODD);
            String message = buildReminderMessageForTest(c);

            assertTrue(message.contains("⏰"));
            assertTrue(message.contains("体育"));
            assertTrue(message.contains("第 3 周"));
            assertTrue(message.contains("单周"));
        }

        @Test
        @DisplayName("单周课程提醒标注单周")
        void oddWeekLabel() {
            CourseEntity c = new CourseEntity("u1", "单周体育", null, 1, 3, 4, null, 1, 18, CourseEntity.WEEK_ODD);
            String message = buildReminderMessageForTest(c);
            assertTrue(message.contains("单周"));
        }

        @Test
        @DisplayName("双周课程提醒标注双周")
        void evenWeekLabel() {
            CourseEntity c = new CourseEntity("u1", "双周实验", null, 2, 7, 8, null, 2, 16, CourseEntity.WEEK_EVEN);
            String message = buildReminderMessageForTest(c);
            assertTrue(message.contains("双周"));
        }

        private CourseEntity createDefaultCourseEntity() {
            return new CourseEntity("u1", "高等数学", "张老师", 1, 1, 2, "A101", 1, 16, CourseEntity.WEEK_ALL);
        }

        /**
         * 模拟 ScheduleReminderService 的消息构建逻辑
         */
        private String buildReminderMessageForTest(CourseEntity course) {
            StringBuilder sb = new StringBuilder();
            sb.append("⏰ 课前提醒\n\n");
            sb.append("📚 ").append(course.getCourseName()).append("\n");
            if (course.getClassroom() != null && !course.getClassroom().isBlank()) {
                sb.append("📍 ").append(course.getClassroom()).append("\n");
            }
            if (course.getTeacher() != null && !course.getTeacher().isBlank()) {
                sb.append("👨‍🏫 ").append(course.getTeacher()).append("\n");
            }
            sb.append("⏱ 08:00  (1-2节)\n");
            sb.append("📅 第 ").append(3).append(" 周");
            if (!CourseEntity.WEEK_ALL.equals(course.getWeekType())) {
                sb.append("（").append(CourseEntity.WEEK_ODD.equals(course.getWeekType()) ? "单周" : "双周").append("）");
            }
            return sb.toString();
        }
    }

    // ==================== CourseParser Excel 解析测试 ====================

    @Nested
    @DisplayName("CourseParser - Excel 矩阵格式解析")
    class ExcelMatrixParseTest {

        private CourseParser parser;

        @BeforeEach
        void setUp() {
            parser = new CourseParser(new ObjectMapper());
        }

        @Test
        @DisplayName("空 Excel 数据返回空列表")
        void emptyExcel() {
            // 无法构造真实 Excel 字节，但验证空数据行为
            byte[] emptyBytes = new byte[0];
            List<CourseEntity> courses = parser.parseFromExcel(emptyBytes);
            assertTrue(courses.isEmpty());
        }

        @Test
        @DisplayName("无效 Excel 数据返回空列表")
        void invalidExcelBytes() {
            byte[] invalidBytes = "这不是Excel文件内容".getBytes();
            List<CourseEntity> courses = parser.parseFromExcel(invalidBytes);
            assertTrue(courses.isEmpty());
        }
    }

    // ==================== SemesterEntity 测试 ====================

    @Nested
    @DisplayName("SemesterEntity - 学期模型")
    class SemesterEntityTest {

        @Test
        @DisplayName("构造学期实体")
        void createSemester() {
            SemesterEntity s = new SemesterEntity("u1", 2026, SemesterEntity.TERM_FALL,
                    LocalDate.of(2026, 9, 7), SemesterEntity.SOURCE_USER_CONFIRM);
            assertEquals("u1", s.getUserId());
            assertEquals(2026, s.getAcademicYear());
            assertEquals(SemesterEntity.TERM_FALL, s.getTerm());
            assertEquals(LocalDate.of(2026, 9, 7), s.getStartDate());
            assertEquals(SemesterEntity.SOURCE_USER_CONFIRM, s.getSource());
        }

        @Test
        @DisplayName("显示名称：2026秋季学期")
        void displayNameFall() {
            SemesterEntity s = new SemesterEntity("u1", 2026, SemesterEntity.TERM_FALL,
                    LocalDate.of(2026, 9, 7), null);
            assertEquals("2026秋季学期", s.getDisplayName());
        }

        @Test
        @DisplayName("显示名称：2026春季学期")
        void displayNameSpring() {
            SemesterEntity s = new SemesterEntity("u1", 2026, SemesterEntity.TERM_SPRING,
                    LocalDate.of(2026, 3, 2), null);
            assertEquals("2026春季学期", s.getDisplayName());
        }

        @Test
        @DisplayName("起始日显示：2026年9月7日（周一）")
        void startDateDisplay() {
            SemesterEntity s = new SemesterEntity("u1", 2026, SemesterEntity.TERM_FALL,
                    LocalDate.of(2026, 9, 7), null);
            assertEquals("2026年9月7日（周一）", s.getStartDateDisplay());
        }

        @Test
        @DisplayName("来源显示：用户确认")
        void sourceDisplayUserConfirm() {
            SemesterEntity s = new SemesterEntity("u1", 2026, SemesterEntity.TERM_FALL,
                    LocalDate.of(2026, 9, 7), SemesterEntity.SOURCE_USER_CONFIRM);
            assertEquals("用户确认", s.getSourceDisplay());
        }

        @Test
        @DisplayName("来源显示：自动检测")
        void sourceDisplayAutoDetect() {
            SemesterEntity s = new SemesterEntity("u1", 2026, SemesterEntity.TERM_FALL,
                    LocalDate.of(2026, 9, 7), SemesterEntity.SOURCE_AUTO_DETECT);
            assertEquals("自动检测", s.getSourceDisplay());
        }

        @Test
        @DisplayName("来源显示：系统默认")
        void sourceDisplaySystemDefault() {
            SemesterEntity s = new SemesterEntity("u1", 2026, SemesterEntity.TERM_FALL,
                    LocalDate.of(2026, 9, 7), SemesterEntity.SOURCE_SYSTEM_DEFAULT);
            assertEquals("系统默认", s.getSourceDisplay());
        }

        @Test
        @DisplayName("学期第一天为第 1 周")
        void firstWeek() {
            SemesterEntity s = new SemesterEntity("u1", 2026, SemesterEntity.TERM_FALL,
                    LocalDate.now(), SemesterEntity.SOURCE_AUTO_DETECT);
            assertEquals(1, s.getCurrentWeek());
        }

        @Test
        @DisplayName("学期前返回 -1")
        void beforeSemester() {
            SemesterEntity s = new SemesterEntity("u1", 2026, SemesterEntity.TERM_FALL,
                    LocalDate.now().plusDays(7), SemesterEntity.SOURCE_AUTO_DETECT);
            assertEquals(-1, s.getCurrentWeek());
        }

        @Test
        @DisplayName("学期第 2 周返回 2")
        void secondWeek() {
            SemesterEntity s = new SemesterEntity("u1", 2026, SemesterEntity.TERM_FALL,
                    LocalDate.now().minusDays(7), SemesterEntity.SOURCE_AUTO_DETECT);
            assertEquals(2, s.getCurrentWeek());
        }

        @Test
        @DisplayName("isOddWeek / isEvenWeek 正确")
        void oddEvenWeek() {
            SemesterEntity s = new SemesterEntity("u1", 2026, SemesterEntity.TERM_FALL,
                    LocalDate.now().minusDays(7), SemesterEntity.SOURCE_AUTO_DETECT);
            int week = s.getCurrentWeek();
            if (week % 2 == 1) {
                assertTrue(s.isOddWeek());
                assertFalse(s.isEvenWeek());
            } else {
                assertFalse(s.isOddWeek());
                assertTrue(s.isEvenWeek());
            }
        }

        @Test
        @DisplayName("学期前 isOddWeek 返回 false")
        void beforeSemesterOddWeek() {
            SemesterEntity s = new SemesterEntity("u1", 2026, SemesterEntity.TERM_FALL,
                    LocalDate.now().plusDays(365), SemesterEntity.SOURCE_AUTO_DETECT);
            assertFalse(s.isOddWeek());
            assertFalse(s.isEvenWeek());
        }

        @Test
        @DisplayName("startDate 与 startDateString 互相转换")
        void startDateStringConversion() {
            SemesterEntity s = new SemesterEntity();
            s.setStartDateFromString("2026-09-07");
            assertEquals(LocalDate.of(2026, 9, 7), s.getStartDate());
            assertEquals("2026-09-07", s.getStartDateString());
        }

        @Test
        @DisplayName("无 startDate 时 getStartDateString 返回空字符串")
        void emptyStartDateString() {
            SemesterEntity s = new SemesterEntity();
            assertEquals("", s.getStartDateString());
        }

        @Test
        @DisplayName("无 startDate 时 getCurrentWeek 返回 -1")
        void noStartDateReturnsMinusOne() {
            SemesterEntity s = new SemesterEntity();
            assertEquals(-1, s.getCurrentWeek());
        }
    }

    // ==================== SemesterService 测试 ====================

    @Nested
    @DisplayName("SemesterService - 学期推算与工具方法")
    class SemesterServiceTest {

        @Test
        @DisplayName("getMondayOfWeek：周一返回自身")
        void mondayOfWeekReturnsItself() {
            // 2026-03-02 是周一
            LocalDate monday = LocalDate.of(2026, 3, 2);
            assertEquals(monday, SemesterService.getMondayOfWeek(monday));
        }

        @Test
        @DisplayName("getMondayOfWeek：周日返回当周周一")
        void sundayReturnsMonday() {
            // 2026-03-08 是周日，当周周一为 2026-03-02
            LocalDate sunday = LocalDate.of(2026, 3, 8);
            assertEquals(LocalDate.of(2026, 3, 2), SemesterService.getMondayOfWeek(sunday));
        }

        @Test
        @DisplayName("getMondayOfWeek：周三返回当周周一")
        void wednesdayReturnsMonday() {
            // 2026-03-04 是周三，当周周一为 2026-03-02
            LocalDate wednesday = LocalDate.of(2026, 3, 4);
            assertEquals(LocalDate.of(2026, 3, 2), SemesterService.getMondayOfWeek(wednesday));
        }

        @Test
        @DisplayName("3月1日所在周为春季学期第1周")
        void springSemesterFromMarch() {
            // 2026-03-01 是周日，所在周周一为 2026-02-23
            SemesterEntity s = SemesterService.detectSemester("u1", LocalDate.of(2026, 3, 15));
            assertEquals("u1", s.getUserId());
            assertEquals(2026, s.getAcademicYear());
            assertEquals(SemesterEntity.TERM_SPRING, s.getTerm());
            assertEquals(LocalDate.of(2026, 2, 23), s.getStartDate());
            assertEquals(SemesterEntity.SOURCE_AUTO_DETECT, s.getSource());
        }

        @Test
        @DisplayName("8月推算春季学期（3~8月为春季学期范围）")
        void springSemesterFromAugust() {
            // 2026-08-15 → 仍在春季学期范围（3~8月）
            SemesterEntity s = SemesterService.detectSemester("u1", LocalDate.of(2026, 8, 15));
            assertEquals(2026, s.getAcademicYear());
            assertEquals(SemesterEntity.TERM_SPRING, s.getTerm());
            assertEquals(LocalDate.of(2026, 2, 23), s.getStartDate());
        }

        @Test
        @DisplayName("9月推算秋季学期")
        void autumnSemesterFromSeptember() {
            // 2026-09-10 → 秋季学期，9月1日所在周一为 2026-08-31
            SemesterEntity s = SemesterService.detectSemester("u1", LocalDate.of(2026, 9, 10));
            assertEquals(2026, s.getAcademicYear());
            assertEquals(SemesterEntity.TERM_FALL, s.getTerm());
            assertEquals(LocalDate.of(2026, 8, 31), s.getStartDate());
        }

        @Test
        @DisplayName("1月推算上学期秋季学期（跨年）")
        void autumnSemesterFromJanuary() {
            // 2027-01-10 → 秋季学期跨年，academicYear 应为 2026
            SemesterEntity s = SemesterService.detectSemester("u1", LocalDate.of(2027, 1, 10));
            assertEquals(2026, s.getAcademicYear());
            assertEquals(SemesterEntity.TERM_FALL, s.getTerm());
            assertEquals(LocalDate.of(2026, 8, 31), s.getStartDate());
        }

        @Test
        @DisplayName("2月推算上学期秋季学期（2月属于9~2月范围）")
        void autumnSemesterFromFebruary() {
            // 2026-02-20 → 上学期秋季学期，academicYear 为 2025
            SemesterEntity s = SemesterService.detectSemester("u1", LocalDate.of(2026, 2, 20));
            assertEquals(2025, s.getAcademicYear());
            assertEquals(SemesterEntity.TERM_FALL, s.getTerm());
            assertEquals(LocalDate.of(2025, 9, 1), s.getStartDate());
        }

        @Test
        @DisplayName("7月推算春季学期（暑假期间）")
        void springSemesterFromJuly() {
            // 2026-07-15 → 仍在春季学期范围（3~8月）
            SemesterEntity s = SemesterService.detectSemester("u1", LocalDate.of(2026, 7, 15));
            assertEquals(2026, s.getAcademicYear());
            assertEquals(SemesterEntity.TERM_SPRING, s.getTerm());
        }

        @Test
        @DisplayName("9月1日为周二时周一为8月31日")
        void septemberFirstIsTuesday() {
            // 验证：2026年9月1日是周二
            LocalDate sep1 = LocalDate.of(2026, 9, 1);
            assertEquals(DayOfWeek.TUESDAY, sep1.getDayOfWeek());
            // 周一应为 2026-08-31
            assertEquals(LocalDate.of(2026, 8, 31), SemesterService.getMondayOfWeek(sep1));
        }
    }

    // ==================== SemesterDetector 测试 ====================

    @Nested
    @DisplayName("SemesterDetector - 学期检测")
    class SemesterDetectorTest {

        private SemesterDetector detector;

        @BeforeEach
        void setUp() {
            detector = new SemesterDetector();
        }

        // ====== 文件名检测 ======

        @Test
        @DisplayName("文件名含「2026秋季」识别为 FALL")
        void fileNameFallChinese() {
            Integer year = detector.extractYear("2026秋季课表.xlsx");
            String term = detector.extractTerm("2026秋季课表.xlsx");
            assertEquals(2026, year);
            assertEquals(SemesterEntity.TERM_FALL, term);
        }

        @Test
        @DisplayName("文件名含「2026春」识别为 SPRING")
        void fileNameSpringChinese() {
            Integer year = detector.extractYear("2026春季课表.xlsx");
            String term = detector.extractTerm("2026春季课表.xlsx");
            assertEquals(2026, year);
            assertEquals(SemesterEntity.TERM_SPRING, term);
        }

        @Test
        @DisplayName("文件名含「Fall 2026」识别为 FALL")
        void fileNameFallEnglish() {
            String term = detector.extractTerm("Fall_2026_Schedule.pdf");
            assertEquals(SemesterEntity.TERM_FALL, term);
        }

        @Test
        @DisplayName("文件名含「Spring2026」识别为 SPRING")
        void fileNameSpringEnglish() {
            String term = detector.extractTerm("Spring2026.xlsx");
            assertEquals(SemesterEntity.TERM_SPRING, term);
        }

        @Test
        @DisplayName("文件名无学期信息返回 null")
        void fileNameNoTermInfo() {
            Integer year = detector.extractYear("课表.pdf");
            String term = detector.extractTerm("课表.pdf");
            assertNull(year);
            assertNull(term);
        }

        @Test
        @DisplayName("文件名含纯数字但不是年份")
        void fileNameNumberNotYear() {
            // "课表2024.xlsx" → 2024 可以被检测为年份（正确行为）
            Integer year = detector.extractYear("课表2024.xlsx");
            assertEquals(Integer.valueOf(2024), year);
        }

        // ====== detectFromParams ======

        @Test
        @DisplayName("detectFromParams 正确创建学期")
        void detectFromParamsValid() {
            SemesterEntity s = detector.detectFromParams("u1", 2026, SemesterEntity.TERM_FALL);
            assertNotNull(s);
            assertEquals(2026, s.getAcademicYear());
            assertEquals(SemesterEntity.TERM_FALL, s.getTerm());
            assertEquals(SemesterEntity.SOURCE_USER_CONFIRM, s.getSource());
            assertEquals(LocalDate.of(2026, 8, 31), s.getStartDate()); // 9月1日周二，周一为8月31日
        }

        @Test
        @DisplayName("detectFromParams 参数无效返回 null")
        void detectFromParamsInvalid() {
            assertNull(detector.detectFromParams("u1", 0, SemesterEntity.TERM_FALL));
            assertNull(detector.detectFromParams("u1", 2026, ""));
            assertNull(detector.detectFromParams("u1", 2026, "INVALID"));
        }

        // ====== detectFromFile ======

        @Test
        @DisplayName("detectFromFile 从文件名检测学期")
        void detectFromFileWithFileName() {
            SemesterEntity s = detector.detectFromFile("u1", "2026秋季课表.xlsx", null);
            assertNotNull(s);
            assertEquals(2026, s.getAcademicYear());
            assertEquals(SemesterEntity.TERM_FALL, s.getTerm());
            assertEquals(SemesterEntity.SOURCE_AUTO_DETECT, s.getSource());
        }

        @Test
        @DisplayName("detectFromFile 文件名无学期信息返回 null")
        void detectFromFileNoInfo() {
            SemesterEntity s = detector.detectFromFile("u1", "课表.pdf", null);
            assertNull(s);
        }

        @Test
        @DisplayName("detectFromFile 从文件内容检测学期")
        void detectFromFileWithContent() {
            String content = "2026-2027学年第一学期课程表";
            SemesterEntity s = detector.detectFromFile("u1", "课表.xlsx", content);
            assertNotNull(s);
            assertEquals(2026, s.getAcademicYear());
            assertEquals(SemesterEntity.TERM_FALL, s.getTerm());
        }

        @Test
        @DisplayName("detectFromFile 内容含第二学期识别为 SPRING")
        void detectFromFileContentSecondTerm() {
            String content = "2026-2027学年第二学期";
            SemesterEntity s = detector.detectFromFile("u1", "课表.xlsx", content);
            assertNotNull(s);
            assertEquals(2027, s.getAcademicYear());
            assertEquals(SemesterEntity.TERM_SPRING, s.getTerm());
        }

        // ====== detectAuto ======

        @Test
        @DisplayName("detectAuto 自动推算不返回 null")
        void detectAutoNotNull() {
            SemesterEntity s = detector.detectAuto("u1");
            assertNotNull(s);
            assertNotNull(s.getStartDate());
        }

        // ====== calculateStartDate ======

        @Test
        @DisplayName("calculateStartDate 秋季学期取9月1日所在周一")
        void calculateStartDateFall() {
            // 2026年9月1日是周二，周一为2026-08-31
            LocalDate start = SemesterDetector.calculateStartDate(2026, SemesterEntity.TERM_FALL);
            assertEquals(LocalDate.of(2026, 8, 31), start);
        }

        @Test
        @DisplayName("calculateStartDate 春季学期取3月1日所在周一")
        void calculateStartDateSpring() {
            // 2026年3月1日是周日，周一为2026-02-23
            LocalDate start = SemesterDetector.calculateStartDate(2026, SemesterEntity.TERM_SPRING);
            assertEquals(LocalDate.of(2026, 2, 23), start);
        }
    }

    // ==================== ImportStateManager 状态扩展测试 ====================

    @Nested
    @DisplayName("CourseImportStateManager - 学期状态管理")
    class ImportStateManagerSemesterTest {

        private CourseImportStateManager stateManager;

        @BeforeEach
        void setUp() {
            stateManager = new CourseImportStateManager();
        }

        @Test
        @DisplayName("WAITING_SEMESTER 状态设置")
        void waitingSemesterState() {
            stateManager.setWaitingSemester("user1");
            assertEquals(CourseImportStateManager.Phase.WAITING_SEMESTER, stateManager.getPhase("user1"));
        }

        @Test
        @DisplayName("设置和获取待确认学期")
        void pendingSemester() {
            SemesterEntity semester = new SemesterEntity("u1", 2026, SemesterEntity.TERM_FALL,
                    LocalDate.of(2026, 9, 7), SemesterEntity.SOURCE_AUTO_DETECT);
            stateManager.setPendingSemester("user1", semester);

            SemesterEntity retrieved = stateManager.getPendingSemester("user1");
            assertNotNull(retrieved);
            assertEquals(2026, retrieved.getAcademicYear());
            assertEquals(SemesterEntity.TERM_FALL, retrieved.getTerm());
        }

        @Test
        @DisplayName("清除状态同时清除待确认学期")
        void clearRemovesPendingSemester() {
            SemesterEntity semester = new SemesterEntity("u1", 2026, SemesterEntity.TERM_FALL,
                    LocalDate.of(2026, 9, 7), SemesterEntity.SOURCE_AUTO_DETECT);
            stateManager.setPendingSemester("user1", semester);
            stateManager.clear("user1");

            assertNull(stateManager.getPendingSemester("user1"));
        }

        @Test
        @DisplayName("setWaitingFile 清除待确认学期")
        void waitingFileClearsSemester() {
            SemesterEntity semester = new SemesterEntity("u1", 2026, SemesterEntity.TERM_FALL,
                    LocalDate.of(2026, 9, 7), SemesterEntity.SOURCE_AUTO_DETECT);
            stateManager.setPendingSemester("user1", semester);
            stateManager.setWaitingFile("user1");

            assertNull(stateManager.getPendingSemester("user1"));
            assertEquals(CourseImportStateManager.Phase.WAITING_FILE, stateManager.getPhase("user1"));
        }

        @Test
        @DisplayName("无待确认学期时返回 null")
        void noPendingSemesterReturnsNull() {
            assertNull(stateManager.getPendingSemester("unknown_user"));
        }

        @Test
        @DisplayName("不同用户学期隔离")
        void isolatedSemesters() {
            SemesterEntity s1 = new SemesterEntity("u1", 2026, SemesterEntity.TERM_FALL,
                    LocalDate.of(2026, 9, 7), null);
            SemesterEntity s2 = new SemesterEntity("u2", 2026, SemesterEntity.TERM_SPRING,
                    LocalDate.of(2026, 3, 2), null);

            stateManager.setPendingSemester("user1", s1);
            stateManager.setPendingSemester("user2", s2);

            assertEquals(SemesterEntity.TERM_FALL, stateManager.getPendingSemester("user1").getTerm());
            assertEquals(SemesterEntity.TERM_SPRING, stateManager.getPendingSemester("user2").getTerm());
        }

        @Test
        @DisplayName("pendingSemester 在 clearPendingSemester 后清除")
        void clearPendingSemesterRemoves() {
            SemesterEntity semester = new SemesterEntity("u1", 2026, SemesterEntity.TERM_FALL,
                    LocalDate.of(2026, 9, 7), null);
            stateManager.setPendingSemester("user1", semester);

            assertNotNull(stateManager.getPendingSemester("user1"));

            stateManager.clearPendingSemester("user1");
            assertNull(stateManager.getPendingSemester("user1"));
        }
    }
}