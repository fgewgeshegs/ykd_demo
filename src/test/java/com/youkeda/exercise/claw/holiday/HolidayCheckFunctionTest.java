package com.youkeda.exercise.claw.holiday;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HolidayCheckFunction 单元测试
 *
 * <p>验证单日期查询、范围查询、邻近检查、边界情况等场景。
 */
class HolidayCheckFunctionTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static HolidayDataLoader dataLoader;
    private HolidayCheckFunction function;

    @BeforeAll
    static void initDataLoader() {
        dataLoader = new HolidayDataLoader(objectMapper);
        dataLoader.loadAll();
    }

    @BeforeEach
    void setUp() {
        function = new HolidayCheckFunction(objectMapper, new LLMFunctionRegistry(), dataLoader);
    }

    @Test
    void shouldReturnCorrectName() {
        assertEquals("holiday_check", function.getName());
    }

    @Test
    void shouldReturnNonEmptyDescription() {
        assertNotNull(function.getDescription());
        assertFalse(function.getDescription().isEmpty());
    }

    @Test
    void shouldReturnValidJsonSchema() throws Exception {
        JsonNode params = function.getParameters();
        assertEquals("object", params.get("type").asText());
        assertTrue(params.has("properties"));
        // 至少要有 date 或 date_range 参数
        JsonNode properties = params.get("properties");
        assertTrue(properties.has("date") || properties.has("date_range"));
    }

    // ========== 单日期查询 ==========

    @Test
    void shouldClassifyHoliday() throws Exception {
        // 2026-10-01 国庆节
        String result = function.execute("{\"date\":\"2026-10-01\"}");
        JsonNode json = objectMapper.readTree(result);

        assertEquals("2026-10-01", json.get("date").asText());
        assertEquals("holiday", json.get("day_type").asText());
        assertEquals("国庆节", json.get("holiday_name").asText());
        assertTrue(json.has("team_building_score"));
        assertTrue(json.has("team_building_advice"));
        assertTrue(json.has("weekday_name"));
    }

    @Test
    void shouldClassifySwapWorkday() throws Exception {
        // 2026-10-10 国庆节调休上班（周六上班）
        String result = function.execute("{\"date\":\"2026-10-10\"}");
        JsonNode json = objectMapper.readTree(result);

        assertEquals("swap_workday", json.get("day_type").asText());
        assertTrue(json.get("team_building_score").asInt() <= 3);
        assertTrue(json.has("holiday_context"));
    }

    @Test
    void shouldClassifyWeekend() throws Exception {
        // 2026-07-25 周六
        String result = function.execute("{\"date\":\"2026-07-25\"}");
        JsonNode json = objectMapper.readTree(result);

        assertEquals("weekend", json.get("day_type").asText());
        assertTrue(json.get("is_weekend").asBoolean());
        assertEquals("星期六", json.get("weekday_name").asText());
        assertTrue(json.get("team_building_score").asInt() >= 7);
    }

    @Test
    void shouldClassifyWeekday() throws Exception {
        // 2026-07-27 周一
        String result = function.execute("{\"date\":\"2026-07-27\"}");
        JsonNode json = objectMapper.readTree(result);

        assertEquals("weekday", json.get("day_type").asText());
        assertFalse(json.get("is_weekend").asBoolean());
        assertEquals("星期一", json.get("weekday_name").asText());
    }

    @Test
    void shouldClassifySpringFestival() throws Exception {
        // 2026-02-15 春节（腊月二十八，假期第一天）
        String result = function.execute("{\"date\":\"2026-02-15\"}");
        JsonNode json = objectMapper.readTree(result);

        assertEquals("holiday", json.get("day_type").asText());
        assertEquals("春节", json.get("holiday_name").asText());
    }

    // ========== 日期范围查询 ==========

    @Test
    void shouldQueryDateRange() throws Exception {
        // 2026-09-28 ~ 2026-10-10（国庆前后共13天）
        String result = function.execute(
                "{\"date_range\":{\"start\":\"2026-09-28\",\"end\":\"2026-10-10\"}}");
        JsonNode json = objectMapper.readTree(result);

        assertEquals("2026-09-28", json.get("start").asText());
        assertEquals("2026-10-10", json.get("end").asText());

        assertTrue(json.has("days"));
        assertEquals(13, json.get("days").size());

        JsonNode summary = json.get("summary");
        assertEquals(13, summary.get("total_days").asInt());
        assertTrue(summary.has("holidays"));
        assertTrue(summary.has("swap_workdays"));
        assertTrue(summary.has("weekends"));
        assertTrue(summary.has("weekdays"));

        // 确认有推荐日期和最差日期
        assertTrue(summary.get("recommended_dates").isArray());
        assertTrue(summary.get("worst_dates").isArray());
    }

    @Test
    void shouldReturnAllDayTypesInRange() throws Exception {
        // 2026-09-26 ~ 2026-10-11 中应包含 holiday / swap_workday / weekend / weekday 四种类型
        String result = function.execute(
                "{\"date_range\":{\"start\":\"2026-09-26\",\"end\":\"2026-10-11\"}}");
        JsonNode json = objectMapper.readTree(result);
        JsonNode days = json.get("days");

        boolean hasHoliday = false, hasSwap = false, hasWeekend = false, hasWeekday = false;
        for (JsonNode day : days) {
            String type = day.get("day_type").asText();
            switch (type) {
                case "holiday" -> hasHoliday = true;
                case "swap_workday" -> hasSwap = true;
                case "weekend" -> hasWeekend = true;
                case "weekday" -> hasWeekday = true;
            }
        }
        assertTrue(hasHoliday, "应包含法定假日");
        assertTrue(hasSwap, "应包含调休工作日");
        assertTrue(hasWeekend, "应包含普通周末");
        assertTrue(hasWeekday, "应包含普通工作日");
    }

    // ========== 跨年查询 ==========

    @Test
    void shouldHandleCrossYearRange() throws Exception {
        // 2026-12-25 ~ 2027-01-05
        String result = function.execute(
                "{\"date_range\":{\"start\":\"2026-12-25\",\"end\":\"2027-01-05\"}}");
        JsonNode json = objectMapper.readTree(result);

        JsonNode summary = json.get("summary");
        assertEquals(12, summary.get("total_days").asInt());

        // 2027-01-01 应为元旦假日
        JsonNode days = json.get("days");
        JsonNode jan1 = null;
        for (JsonNode day : days) {
            if ("2027-01-01".equals(day.get("date").asText())) {
                jan1 = day;
                break;
            }
        }
        assertNotNull(jan1, "应包含 2027-01-01");
        if (jan1 != null) {
            assertEquals("holiday", jan1.get("day_type").asText());
            assertEquals("元旦", jan1.get("holiday_name").asText());
        }
    }

    // ========== 边界数据（无数据年份） ==========

    @Test
    void shouldFallbackWhenNoData() throws Exception {
        // 2030 年无数据，应仅按周末判断
        String result = function.execute("{\"date\":\"2030-05-01\"}");
        JsonNode json = objectMapper.readTree(result);

        // 2030-05-01 是周三，应为普通工作日
        assertTrue(json.has("day_type"));
        assertEquals("weekday", json.get("day_type").asText());
        assertTrue(json.has("team_building_score"));
        assertTrue(json.has("team_building_advice"));
    }

    @Test
    void shouldReturnWeekendForNoDataSaturday() throws Exception {
        // 2030-05-04 是周六，无数据时应判为 weekend
        String result = function.execute("{\"date\":\"2030-05-04\"}");
        JsonNode json = objectMapper.readTree(result);

        assertEquals("weekend", json.get("day_type").asText());
        assertTrue(json.get("is_weekend").asBoolean());
    }

    // ========== 邻近检查 ==========

    @Test
    void shouldCheckAdjacentImpact() throws Exception {
        // 2026-09-25，国庆前，开启邻近检查
        String result = function.execute(
                "{\"date\":\"2026-09-25\",\"check_adjacent\":true}");
        JsonNode json = objectMapper.readTree(result);

        assertTrue(json.has("adjacent_impact"));
        JsonNode adj = json.get("adjacent_impact");

        assertTrue(adj.has("has_holiday_within_3days_after"));
        assertTrue(adj.has("nearest_holiday"));
        assertTrue(adj.has("impact_description"));

        // 9/25 在国庆前，后面应有节假日
        assertTrue(adj.get("has_holiday_within_3days_after").asBoolean() ||
                adj.get("days_to_nearest_holiday").asLong() <= 6);
    }

    // ========== 错误处理 ==========

    @Test
    void shouldReturnErrorForMissingParameter() throws Exception {
        String result = function.execute("{}");
        assertTrue(result.contains("error"));
    }

    @Test
    void shouldReturnErrorForInvalidDate() throws Exception {
        String result = function.execute("{\"date\":\"not-a-date\"}");
        assertTrue(result.contains("error"));
    }

    @Test
    void shouldReturnErrorForInvalidRangeOrder() throws Exception {
        String result = function.execute(
                "{\"date_range\":{\"start\":\"2026-10-10\",\"end\":\"2026-09-28\"}}");
        assertTrue(result.contains("error"));
    }

    // ========== JSON 格式完整性 ==========

    @Test
    void singleDateResultShouldContainAllFields() throws Exception {
        String result = function.execute("{\"date\":\"2026-07-25\"}");
        JsonNode json = objectMapper.readTree(result);

        assertTrue(json.has("date"));
        assertTrue(json.has("weekday"));
        assertTrue(json.has("weekday_name"));
        assertTrue(json.has("day_type"));
        assertTrue(json.has("is_weekend"));
        assertTrue(json.has("team_building_score"));
        assertTrue(json.has("team_building_advice"));
    }
}
