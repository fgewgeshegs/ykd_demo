package com.youkeda.exercise.claw.feature.transport.didi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 {@link DidiRideResultFormatter} 的 MCP 非 JSON 文本状态推断。
 */
class DidiRideResultFormatterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DidiRideResultFormatter formatter = new DidiRideResultFormatter(objectMapper);

    // ==================== inferStatus 关键词匹配 ====================

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "searching|系统正在为您匹配最近的司机...",
            "searching|正在寻找附近司机，请稍候",
            "searching|等待司机接单",
            "accepted|司机已接单，正在赶来",
            "accepted|司机已接驾，请耐心等待",
            "en_route|司机正在赶来，距您约2公里",
            "en_route|司机正前往上车点",
            "arrived|司机已到达上车点，等待上车",
            "arrived|车辆已到达，请尽快上车",
            "in_progress|行程进行中",
            "completed|行程已完成，感谢使用",
            "completed|您已到达目的地",
            "cancelled|订单已取消",
            "searching|系统正在为您匹配最近的司机...提示：您可以随时取消订单",
            "en_route|司机正前往目的地",
            "unknown|您的订单已提交",
    })
    void inferStatusFromKeywords(String expected, String text) {
        assertEquals(expected, DidiRideResultFormatter.inferStatus(text),
                "text='" + text + "' should infer status='" + expected + "'");
    }

    // ==================== 边界条件 ====================

    @Test
    void inferStatusNullText() {
        assertEquals("unknown", DidiRideResultFormatter.inferStatus(null));
    }

    @Test
    void inferStatusEmptyText() {
        assertEquals("unknown", DidiRideResultFormatter.inferStatus(""));
    }

    @Test
    void inferStatusBlankText() {
        assertEquals("unknown", DidiRideResultFormatter.inferStatus("   "));
    }

    @Test
    void inferStatusSearchingNotOverriddenByCancelTip() {
        // 回归测试：滴滴标准提示语"您可以随时取消订单"不应导致状态误判为 cancelled
        String text = "系统正在为您匹配最近的司机...\n提示：您可以随时取消订单";
        assertEquals("searching", DidiRideResultFormatter.inferStatus(text),
                "包含'随时取消订单'提示语的匹配中文案应返回 searching，而非 cancelled");
    }

    @Test
    void inferStatusCancelledStillWorks() {
        // 确认"订单已取消"仍然正确返回 cancelled
        assertEquals("cancelled", DidiRideResultFormatter.inferStatus("订单已取消"));
    }

    // ==================== formatQueryResult - MCP 文本包装 ====================

    @Test
    void formatTextQueryResultPreservesMessageAndInfersStatus() {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("text", "系统正在为您匹配最近的司机...");

        String result = formatter.formatQueryResult(data, "ORDER-001");

        assertTrue(result.contains("\"status\":\"searching\""),
                "应推断出 searching 状态，实际: " + result);
        assertTrue(result.contains("\"message\":\"系统正在为您匹配最近的司机...\""),
                "应保留原始文本到 message 字段，实际: " + result);
        assertTrue(result.contains("\"order_id\":\"ORDER-001\""),
                "应包含 order_id，实际: " + result);
    }

    @Test
    void formatTextQueryResultAccepted() {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("text", "司机已接单，车牌号京B12345，正在赶来");

        String result = formatter.formatQueryResult(data, "ORDER-002");

        // "已接单" 在 inferStatus 中优先级高于 "赶来"
        assertTrue(result.contains("\"status\":\"accepted\""),
                "应推断出 accepted 状态（已接单优先于赶来），实际: " + result);
    }

    // ==================== formatQueryResult - JSON 路径不变 ====================

    @Test
    void formatNormalJsonResultShouldNotBeAffected() {
        ObjectNode data = objectMapper.createObjectNode();
        data.put("status", "in_progress");
        data.put("driverName", "张师傅");
        data.put("driverPhone", "13800138000");
        data.put("carNumber", "京B12345");

        String result = formatter.formatQueryResult(data, "ORDER-003");

        // 不应走文本解析路径
        assertTrue(result.contains("\"status\":\"in_progress\""),
                "JSON 路径应保持原 status，实际: " + result);
        assertTrue(result.contains("\"driver_name\":\"张师傅\""),
                "应包含司机名，实际: " + result);
        assertFalse(result.contains("\"message\":\""),
                "JSON 路径不应有 message 字段，实际: " + result);
    }

    @Test
    void formatJsonWithTextAndStatusShouldNotBeAffected() {
        // 既有 text 又有 status → 走 JSON 路径
        ObjectNode data = objectMapper.createObjectNode();
        data.put("text", "司机正在赶来");
        data.put("status", "en_route");

        String result = formatter.formatQueryResult(data, "ORDER-004");

        assertTrue(result.contains("\"status\":\"en_route\""),
                "有 status 字段时应走 JSON 路径，实际: " + result);
    }
}
