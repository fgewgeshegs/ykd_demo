package com.youkeda.exercise.claw.feature.schedule.imports;

import com.youkeda.exercise.claw.feature.schedule.CourseEntity;
import com.youkeda.exercise.claw.feature.schedule.imports.WeekParser.WeekSpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 周次表达式解析测试
 */
class WeekParserTest {

    @Nested
    @DisplayName("连续范围")
    class ContiguousTest {
        @Test
        void plainRange() {
            WeekSpec s = WeekParser.parse("1-17周");
            assertEquals(1, s.startWeek());
            assertEquals(17, s.endWeek());
            assertEquals(CourseEntity.WEEK_ALL, s.weekType());
            assertNull(s.weekPattern());
        }

        @Test
        void oddRange() {
            WeekSpec s = WeekParser.parse("1-16(单)");
            assertEquals(1, s.startWeek());
            assertEquals(16, s.endWeek());
            assertEquals(CourseEntity.WEEK_ODD, s.weekType());
            assertNull(s.weekPattern());
        }

        @Test
        void evenRange() {
            WeekSpec s = WeekParser.parse("2-18周(双)");
            assertEquals(2, s.startWeek());
            assertEquals(18, s.endWeek());
            assertEquals(CourseEntity.WEEK_EVEN, s.weekType());
            assertNull(s.weekPattern());
        }

        @Test
        void singleWeekMarkerOnly() {
            WeekSpec s = WeekParser.parse("单周");
            assertEquals(1, s.startWeek());
            assertEquals(20, s.endWeek());
            assertEquals(CourseEntity.WEEK_ODD, s.weekType());
        }
    }

    @Nested
    @DisplayName("复杂周次模式")
    class PatternTest {
        @Test
        void oddWeeksList() {
            WeekSpec s = WeekParser.parse("第1,3,5周");
            assertEquals(1, s.startWeek());
            assertEquals(5, s.endWeek());
            assertEquals(CourseEntity.WEEK_ALL, s.weekType());
            assertEquals("1,3,5", s.weekPattern());
        }

        @Test
        void multipleRangesWithParity() {
            WeekSpec s = WeekParser.parse("1-8,11-17周(双)");
            assertEquals(1, s.startWeek());
            assertEquals(17, s.endWeek());
            assertEquals(CourseEntity.WEEK_EVEN, s.weekType());
            assertEquals("1-8,11-17", s.weekPattern());
        }

        @Test
        void fullWidthParens() {
            WeekSpec s = WeekParser.parse("1-8,11-17周（单）");
            assertEquals(CourseEntity.WEEK_ODD, s.weekType());
            assertEquals("1-8,11-17", s.weekPattern());
        }
    }

    @Nested
    @DisplayName("边界")
    class EdgeTest {
        @Test
        void blankReturnsDefault() {
            WeekSpec s = WeekParser.parse("  ");
            assertEquals(1, s.startWeek());
            assertEquals(20, s.endWeek());
            assertEquals(CourseEntity.WEEK_ALL, s.weekType());
            assertNull(s.weekPattern());
        }

        @Test
        void nullReturnsDefault() {
            WeekSpec s = WeekParser.parse(null);
            assertEquals(1, s.startWeek());
            assertEquals(20, s.endWeek());
        }

        @Test
        void weekTypeDetection() {
            assertEquals(CourseEntity.WEEK_ODD, WeekParser.detectWeekType("(单)"));
            assertEquals(CourseEntity.WEEK_EVEN, WeekParser.detectWeekType("双周"));
            assertEquals(CourseEntity.WEEK_ALL, WeekParser.detectWeekType("1-17"));
            assertEquals(CourseEntity.WEEK_ALL, WeekParser.detectWeekType(null));
        }
    }
}
