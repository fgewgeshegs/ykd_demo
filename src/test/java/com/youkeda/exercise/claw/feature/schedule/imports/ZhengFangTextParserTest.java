package com.youkeda.exercise.claw.feature.schedule.imports;

import com.youkeda.exercise.claw.feature.schedule.CourseEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 正方粘贴文本解析测试
 */
class ZhengFangTextParserTest {

    private final ZhengFangTextParser parser = new ZhengFangTextParser();

    @Nested
    @DisplayName("块格式：时间描述行 + 课程信息行")
    class BlockFormatTest {

        @Test
        @DisplayName("单双周 + 常规课程")
        void oddEvenBlocks() {
            String text = "1-16周(单) 星期一 第3-4节\n"
                    + "高等数学 段代凤 教2-203\n"
                    + "\n"
                    + "2-18周(双) 星期二 第1-2节\n"
                    + "离散数学 张伟 教2-516";
            List<CourseEntity> courses = parser.parseByRules(text);
            assertEquals(2, courses.size());

            CourseEntity high = courses.get(0);
            assertEquals("高等数学", high.getCourseName());
            assertEquals("段代凤", high.getTeacher());
            assertEquals("教2-203", high.getClassroom());
            assertEquals(1, high.getDayOfWeek());
            assertEquals(3, high.getStartPeriod());
            assertEquals(4, high.getEndPeriod());
            assertEquals(1, high.getStartWeek());
            assertEquals(16, high.getEndWeek());
            assertEquals(CourseEntity.WEEK_ODD, high.getWeekType());

            CourseEntity discrete = courses.get(1);
            assertEquals("离散数学", discrete.getCourseName());
            assertEquals(2, discrete.getDayOfWeek());
            assertEquals(CourseEntity.WEEK_EVEN, discrete.getWeekType());
        }

        @Test
        @DisplayName("复杂周次 1,3,5")
        void complexWeekPattern() {
            String text = "第1,3,5周 周三 第5-6节\n大学英语 李婷 C3敏学楼404";
            List<CourseEntity> courses = parser.parseByRules(text);
            assertEquals(1, courses.size());
            CourseEntity c = courses.get(0);
            assertEquals("1,3,5", c.getWeekPattern());
            assertEquals(3, c.getDayOfWeek());
            assertEquals(5, c.getStartPeriod());
        }

        @Test
        @DisplayName("同一行内联格式")
        void inlineFormat() {
            String text = "高等数学A（1）下 段代凤 教2-203 1-17周 星期一 第1-2节";
            List<CourseEntity> courses = parser.parseByRules(text);
            assertEquals(1, courses.size());
            CourseEntity c = courses.get(0);
            assertEquals("高等数学A（1）下", c.getCourseName());
            assertEquals("段代凤", c.getTeacher());
            assertEquals("教2-203", c.getClassroom());
            assertEquals(1, c.getDayOfWeek());
            assertEquals(1, c.getStartPeriod());
            assertEquals(2, c.getEndPeriod());
            assertEquals(17, c.getEndWeek());
        }

        @Test
        @DisplayName("倒序：课程信息在前，时间描述在后")
        void reversedOrder() {
            String text = "大学物理 王强 教3-101\n1-16周 星期四 第3-4节";
            List<CourseEntity> courses = parser.parseByRules(text);
            assertEquals(1, courses.size());
            assertEquals("大学物理", courses.get(0).getCourseName());
            assertEquals(4, courses.get(0).getDayOfWeek());
        }
    }

    @Nested
    @DisplayName("实践课程（无固定时间）")
    class PracticeTest {

        @Test
        @DisplayName("无 weekday/period，仅周次")
        void practiceCourse() {
            String text = "3-15周 金工实习 实训基地";
            List<CourseEntity> courses = parser.parseByRules(text);
            assertEquals(1, courses.size());
            CourseEntity c = courses.get(0);
            assertTrue(c.isPractice());
            assertEquals("金工实习", c.getCourseName());
            assertEquals(3, c.getStartWeek());
            assertEquals(15, c.getEndWeek());
            assertNull(c.getDayOfWeek());
            assertNull(c.getStartPeriod());
        }

        @Test
        @DisplayName("实践课标记（分散/集中）")
        void practiceByMarker() {
            String text = "1-16周 认识实习（分散进行）";
            List<CourseEntity> courses = parser.parseByRules(text);
            assertEquals(1, courses.size());
            assertTrue(courses.get(0).isPractice());
        }
    }

    @Nested
    @DisplayName("Tab 矩阵格式")
    class MatrixTest {

        @Test
        @DisplayName("表头无节次标签列")
        void matrixNoLabelColumn() {
            String text = "星期一\t星期二\t星期三\n"
                    + "1-2节\t高等数学 段代凤 教2-203\t\t\n"
                    + "3-4节\t\t离散数学 张伟 教2-516";
            List<CourseEntity> courses = parser.parseByRules(text);
            assertEquals(2, courses.size());

            CourseEntity high = courses.stream()
                    .filter(c -> c.getCourseName().equals("高等数学"))
                    .findFirst().orElseThrow();
            assertEquals(1, high.getDayOfWeek());
            assertEquals(1, high.getStartPeriod());
            assertEquals(2, high.getEndPeriod());

            CourseEntity discrete = courses.stream()
                    .filter(c -> c.getCourseName().equals("离散数学"))
                    .findFirst().orElseThrow();
            assertEquals(2, discrete.getDayOfWeek());
            assertEquals(3, discrete.getStartPeriod());
        }

        @Test
        @DisplayName("表头含节次标签列")
        void matrixWithLabelColumn() {
            String text = "节次\t星期一\t星期二\n"
                    + "1-2节\t高等数学\t\n"
                    + "3-4节\t\t离散数学";
            List<CourseEntity> courses = parser.parseByRules(text);
            assertEquals(2, courses.size());
            assertEquals(1, courses.get(0).getDayOfWeek());
            assertEquals(2, courses.get(1).getDayOfWeek());
        }
    }

    @Nested
    @DisplayName("标签格式")
    class LabeledTest {

        @Test
        @DisplayName("课程名称/教师/时间/地点/周次")
        void labeledCourse() {
            String text = "课程名称：高等数学\n"
                    + "教师：段代凤\n"
                    + "上课时间：星期一 第1-2节\n"
                    + "上课地点：教2-203\n"
                    + "周次：1-16周";
            List<CourseEntity> courses = parser.parseByRules(text);
            assertEquals(1, courses.size());
            CourseEntity c = courses.get(0);
            assertEquals("高等数学", c.getCourseName());
            assertEquals("段代凤", c.getTeacher());
            assertEquals("教2-203", c.getClassroom());
            assertEquals(1, c.getDayOfWeek());
            assertEquals(1, c.getStartPeriod());
            assertEquals(2, c.getEndPeriod());
            assertEquals(16, c.getEndWeek());
        }
    }
}
