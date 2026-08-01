package com.youkeda.exercise.claw.feature.schedule.imports;

import com.youkeda.exercise.claw.feature.schedule.CourseEntity;
import com.youkeda.exercise.claw.feature.schedule.imports.CourseScheduleValidator.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 课程校验器测试：规范化 / 相邻节次合并 / 冲突检测 / 实践课
 */
class CourseScheduleValidatorTest {

    private final CourseScheduleValidator validator = new CourseScheduleValidator();

    @Nested
    @DisplayName("相邻节次自动合并")
    class MergeTest {

        @Test
        @DisplayName("同一课程相邻节次合并为一段")
        void mergeAdjacent() {
            List<CourseEntity> courses = List.of(
                    new CourseEntity("u1", "高等数学", "段老师", 1, 1, 1, "教2-203", 1, 17, CourseEntity.WEEK_ALL),
                    new CourseEntity("u1", "高等数学", "段老师", 1, 2, 2, "教2-203", 1, 17, CourseEntity.WEEK_ALL));
            ValidationResult r = validator.validate(courses);
            assertEquals(1, r.courses().size());
            assertEquals(1, r.courses().get(0).getStartPeriod());
            assertEquals(2, r.courses().get(0).getEndPeriod());
        }

        @Test
        @DisplayName("三节连续合并为 1-3")
        void mergeThree() {
            List<CourseEntity> courses = List.of(
                    new CourseEntity("u1", "物理", "李老师", 3, 1, 1, "A101", 1, 16, CourseEntity.WEEK_ALL),
                    new CourseEntity("u1", "物理", "李老师", 3, 2, 2, "A101", 1, 16, CourseEntity.WEEK_ALL),
                    new CourseEntity("u1", "物理", "李老师", 3, 3, 3, "A101", 1, 16, CourseEntity.WEEK_ALL));
            ValidationResult r = validator.validate(courses);
            assertEquals(1, r.courses().size());
            assertEquals(1, r.courses().get(0).getStartPeriod());
            assertEquals(3, r.courses().get(0).getEndPeriod());
        }

        @Test
        @DisplayName("不相邻节次不合并")
        void notMergeIfGap() {
            List<CourseEntity> courses = List.of(
                    new CourseEntity("u1", "高数", "段老师", 1, 1, 2, "教2-203", 1, 17, CourseEntity.WEEK_ALL),
                    new CourseEntity("u1", "高数", "段老师", 1, 4, 5, "教2-203", 1, 17, CourseEntity.WEEK_ALL));
            ValidationResult r = validator.validate(courses);
            assertEquals(2, r.courses().size());
        }

        @Test
        @DisplayName("单双周不同不合并")
        void notMergeIfParityDiffers() {
            List<CourseEntity> courses = List.of(
                    new CourseEntity("u1", "体育", "王老师", 3, 1, 1, "操场", 1, 18, CourseEntity.WEEK_ODD),
                    new CourseEntity("u1", "体育", "王老师", 3, 2, 2, "操场", 1, 18, CourseEntity.WEEK_EVEN));
            ValidationResult r = validator.validate(courses);
            assertEquals(2, r.courses().size());
        }
    }

    @Nested
    @DisplayName("冲突检测")
    class ConflictTest {

        @Test
        @DisplayName("同天同时段两门课产生冲突告警")
        void conflictDetected() {
            List<CourseEntity> courses = List.of(
                    new CourseEntity("u1", "高数", "段老师", 1, 1, 2, "教2-203", 1, 17, CourseEntity.WEEK_ALL),
                    new CourseEntity("u1", "离散数学", "张老师", 1, 2, 3, "教2-516", 1, 17, CourseEntity.WEEK_ALL));
            ValidationResult r = validator.validate(courses);
            assertTrue(r.hasConflict());
            assertTrue(r.warnings().stream().anyMatch(w -> w.contains("时间冲突")));
        }

        @Test
        @DisplayName("实践课不参与冲突检测")
        void practiceSkipsConflict() {
            CourseEntity practice = CourseEntity.create("u1", "金工实习", "厂方", null, null, null,
                    "实训基地", 3, 15, CourseEntity.WEEK_ALL);
            List<CourseEntity> courses = List.of(
                    new CourseEntity("u1", "高数", "段老师", 1, 1, 2, "教2-203", 1, 17, CourseEntity.WEEK_ALL),
                    practice);
            ValidationResult r = validator.validate(courses);
            assertFalse(r.hasConflict());
            assertEquals(2, r.courses().size());
            assertTrue(r.courses().stream().anyMatch(CourseEntity::isPractice));
        }
    }

    @Nested
    @DisplayName("规范化")
    class NormalizeTest {

        @Test
        @DisplayName("空课程名剔除")
        void dropBlankName() {
            CourseEntity blank = new CourseEntity();
            blank.setCourseName("   ");
            ValidationResult r = validator.validate(List.of(blank));
            assertEquals(0, r.courses().size());
        }

        @Test
        @DisplayName("课程名含(单)标记时覆盖 weekType")
        void oddMarkerOverride() {
            CourseEntity c = new CourseEntity("u1", "体育(单)", "王老师", 3, 1, 2, "操场",
                    1, 18, CourseEntity.WEEK_ALL);
            ValidationResult r = validator.validate(List.of(c));
            assertEquals(CourseEntity.WEEK_ODD, r.courses().get(0).getWeekType());
        }

        @Test
        @DisplayName("课程名含「单」字但非单双周标记时不误判（如单片机原理）")
        void bareDanCharacterNotTreatedAsOdd() {
            CourseEntity c = new CourseEntity("u1", "单片机原理", "王老师", 3, 1, 2, "教2-301",
                    1, 18, CourseEntity.WEEK_ALL);
            ValidationResult r = validator.validate(List.of(c));
            assertEquals(CourseEntity.WEEK_ALL, r.courses().get(0).getWeekType());
        }

        @Test
        @DisplayName("节次越界修正")
        void clampOutOfRangePeriod() {
            CourseEntity c = new CourseEntity("u1", "高数", "段老师", 1, 99, 100, "教2-203",
                    1, 17, CourseEntity.WEEK_ALL);
            ValidationResult r = validator.validate(List.of(c));
            assertEquals(12, r.courses().get(0).getEndPeriod());
        }
    }
}
