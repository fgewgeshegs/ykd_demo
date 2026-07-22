package com.youkeda.exercise.claw.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TimeFunction 单元测试
 *
 * <p>验证三个 action（get_current_time / date_calculate / date_diff）以及各种边界情况。
 */
class TimeFunctionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TimeFunction timeFunction;

    @BeforeEach
    void setUp() {
        timeFunction = new TimeFunction(objectMapper, new LLMFunctionRegistry());
    }

    @Test
    void shouldReturnCorrectName() {
        assertEquals("time_query", timeFunction.getName());
    }

    @Test
    void shouldReturnNonEmptyDescription() {
        assertNotNull(timeFunction.getDescription());
        assertFalse(timeFunction.getDescription().isEmpty());
    }

    @Test
    void shouldReturnValidJsonSchema() throws Exception {
        JsonNode params = timeFunction.getParameters();

        assertEquals("object", params.get("type").asText());
        assertTrue(params.has("properties"));
        assertTrue(params.has("required"));
        assertEquals("action", params.get("required").get(0).asText());

        JsonNode properties = params.get("properties");
        assertTrue(properties.has("action"));
        assertEquals("string", properties.get("action").get("type").asText());

        // 验证 action 的枚举值
        JsonNode enumValues = properties.get("action").get("enum");
        assertTrue(enumValues.isArray());
        assertEquals(3, enumValues.size());
    }

    @Test
    void shouldGetCurrentTime() throws Exception {
        String result = timeFunction.execute("{\"action\":\"get_current_time\"}");
        JsonNode json = objectMapper.readTree(result);

        assertEquals("get_current_time", json.get("action").asText());
        assertTrue(json.has("datetime"));
        assertTrue(json.has("date"));
        assertTrue(json.has("weekday"));
        assertTrue(json.has("weekday_name"));
        assertTrue(json.has("is_weekend"));
        assertTrue(json.has("timezone"));
        assertEquals("Asia/Shanghai", json.get("timezone").asText());

        // 验证日期格式
        String date = json.get("date").asText();
        assertTrue(date.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    void shouldGetCurrentTimeWithCustomTimezone() throws Exception {
        String result = timeFunction.execute(
                "{\"action\":\"get_current_time\",\"timezone\":\"America/New_York\"}");
        JsonNode json = objectMapper.readTree(result);

        assertEquals("America/New_York", json.get("timezone").asText());
    }

    @Test
    void shouldCalculateDateByDeltaDays() throws Exception {
        String result = timeFunction.execute(
                "{\"action\":\"date_calculate\",\"base_date\":\"2026-07-22\",\"delta_days\":3}");
        JsonNode json = objectMapper.readTree(result);

        assertEquals("date_calculate", json.get("action").asText());
        assertEquals("2026-07-22", json.get("from_date").asText());
        assertEquals("2026-07-25", json.get("result_date").asText());
        assertEquals(3, json.get("diff_days").asInt());
    }

    @Test
    void shouldCalculateNegativeDeltaDays() throws Exception {
        String result = timeFunction.execute(
                "{\"action\":\"date_calculate\",\"base_date\":\"2026-07-22\",\"delta_days\":-5}");
        JsonNode json = objectMapper.readTree(result);

        assertEquals("2026-07-17", json.get("result_date").asText());
        assertEquals(-5, json.get("diff_days").asInt());
    }

    @Test
    void shouldFindThisWeekday() throws Exception {
        // 2026-07-22 是星期三，本周六 = 2026-07-25
        String result = timeFunction.execute(
                "{\"action\":\"date_calculate\",\"base_date\":\"2026-07-22\",\"target_weekday\":\"saturday\",\"week_context\":\"this\"}");
        JsonNode json = objectMapper.readTree(result);

        assertEquals("2026-07-25", json.get("result_date").asText());
        assertEquals("星期六", json.get("result_weekday").asText());
    }

    @Test
    void shouldFindNextWeekday() throws Exception {
        // 2026-07-22 是星期三，下周五 = 2026-07-31
        String result = timeFunction.execute(
                "{\"action\":\"date_calculate\",\"base_date\":\"2026-07-22\",\"target_weekday\":\"friday\",\"week_context\":\"next\"}");
        JsonNode json = objectMapper.readTree(result);

        assertEquals("2026-07-31", json.get("result_date").asText());
        assertEquals("星期五", json.get("result_weekday").asText());
    }

    @Test
    void shouldFindLastWeekday() throws Exception {
        // 2026-07-22 是星期三，上周五 = 2026-07-17
        String result = timeFunction.execute(
                "{\"action\":\"date_calculate\",\"base_date\":\"2026-07-22\",\"target_weekday\":\"friday\",\"week_context\":\"last\"}");
        JsonNode json = objectMapper.readTree(result);

        assertEquals("2026-07-17", json.get("result_date").asText());
    }

    @Test
    void shouldCalculateDateDiff() throws Exception {
        String result = timeFunction.execute(
                "{\"action\":\"date_diff\",\"base_date\":\"2026-05-01\",\"target_date\":\"2026-06-19\"}");
        JsonNode json = objectMapper.readTree(result);

        assertEquals("date_diff", json.get("action").asText());
        assertEquals("2026-05-01", json.get("from_date").asText());
        assertEquals("2026-06-19", json.get("to_date").asText());
        assertEquals(49, json.get("diff_days").asInt());
        assertEquals(7, json.get("diff_weeks").asInt());
        assertEquals("future", json.get("direction").asText());
    }

    @Test
    void shouldCalculateNegativeDateDiff() throws Exception {
        // target_date < base_date
        String result = timeFunction.execute(
                "{\"action\":\"date_diff\",\"base_date\":\"2026-07-22\",\"target_date\":\"2026-05-01\"}");
        JsonNode json = objectMapper.readTree(result);

        assertEquals(82, json.get("diff_days").asInt());
        assertEquals("past", json.get("direction").asText());
    }

    @Test
    void shouldHandleSameDayDiff() throws Exception {
        String result = timeFunction.execute(
                "{\"action\":\"date_diff\",\"base_date\":\"2026-07-22\",\"target_date\":\"2026-07-22\"}");
        JsonNode json = objectMapper.readTree(result);

        assertEquals(0, json.get("diff_days").asInt());
        assertEquals("same", json.get("direction").asText());
    }

    @Test
    void shouldDefaultWeekContextToThis() throws Exception {
        // 不传 week_context 应默认 "this"
        String result = timeFunction.execute(
                "{\"action\":\"date_calculate\",\"base_date\":\"2026-07-22\",\"target_weekday\":\"monday\"}");
        JsonNode json = objectMapper.readTree(result);

        // 2026-07-22 是周三，本周一 = 2026-07-20
        assertEquals("2026-07-20", json.get("result_date").asText());
    }

    @Test
    void shouldReturnErrorForMissingAction() throws Exception {
        String result = timeFunction.execute("{}");
        assertTrue(result.contains("error"));
        assertTrue(result.contains("缺少"));
    }

    @Test
    void shouldReturnErrorForInvalidAction() throws Exception {
        String result = timeFunction.execute("{\"action\":\"invalid_action\"}");
        assertTrue(result.contains("error"));
    }

    @Test
    void shouldReturnErrorForMissingDateDiffTarget() throws Exception {
        String result = timeFunction.execute("{\"action\":\"date_diff\",\"base_date\":\"2026-07-22\"}");
        assertTrue(result.contains("error"));
        assertTrue(result.contains("target_date"));
    }

    @Test
    void shouldReturnErrorForMissingCalculateParams() throws Exception {
        String result = timeFunction.execute("{\"action\":\"date_calculate\"}");
        assertTrue(result.contains("error"));
    }

    @Test
    void shouldHandleInvalidWeekdayName() throws Exception {
        String result = timeFunction.execute(
                "{\"action\":\"date_calculate\",\"base_date\":\"2026-07-22\",\"target_weekday\":\"funday\"}");
        assertTrue(result.contains("error"));
    }

    @Test
    void shouldHandleInvalidTimezone() throws Exception {
        String result = timeFunction.execute(
                "{\"action\":\"get_current_time\",\"timezone\":\"Invalid/Zone\"}");
        assertTrue(result.contains("error"));
    }

    @Test
    void shouldHandleInvalidBaseDateFormat() throws Exception {
        // 无效 base_date 不会抛错，而是 fallback 到当前日期
        String result = timeFunction.execute(
                "{\"action\":\"date_calculate\",\"base_date\":\"not-a-date\",\"delta_days\":1}");
        JsonNode json = objectMapper.readTree(result);
        assertEquals("date_calculate", json.get("action").asText());
    }

    @Test
    void shouldReturnChineseWeekdayNames() throws Exception {
        String result = timeFunction.execute(
                "{\"action\":\"get_current_time\"}");
        JsonNode json = objectMapper.readTree(result);

        String weekdayName = json.get("weekday_name").asText();
        assertTrue(weekdayName.startsWith("星期"));
    }
}
