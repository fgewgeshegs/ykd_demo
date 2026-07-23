package com.youkeda.exercise.claw.teamtrip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.tool.FunctionExecutionContext;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamTripPlanServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private InMemoryTeamTripPlanStateStore store;
    private TeamTripPlanService service;
    private TeamTripToolCallPolicy policy;

    @BeforeEach
    void setUp() {
        store = new InMemoryTeamTripPlanStateStore();
        service = new TeamTripPlanService(store, objectMapper);
        policy = new TeamTripToolCallPolicy(store);
    }

    @Test
    void shouldAskOnlyForMissingRequiredInformationAcrossTurns() throws Exception {
        ObjectNode first = service.handle("u1", objectMapper.readTree("""
                {"action":"collect","departure_city":"杭州","participant_count":20}
                """));
        assertEquals("NEED_MORE_INFORMATION", first.path("status").asText());
        assertEquals(4, first.path("missing_fields").size());

        ObjectNode second = service.handle("u1", objectMapper.readTree("""
                {"action":"collect","travel_date":"2026-08-15","duration":"2天1晚",
                 "budget_per_person":1200,"travel_scope":"杭州周边车程2小时内"}
                """));
        assertEquals("READY_FOR_MAP", second.path("status").asText());
        assertEquals("BALANCED_DEFAULT", second.path("plan_mode").asText());
        assertEquals("杭州", second.path("collected_information").path("departureCity").asText());
    }

    @Test
    void shouldUsePriorityModeOnlyWhenUserProvidesPriorities() throws Exception {
        ObjectNode result = service.handle("u2", objectMapper.readTree("""
                {"action":"collect","departure_city":"上海","participant_count":15,
                 "travel_date":"2026-09-05","duration":"1天","budget_total":15000,
                 "destination":"苏州","priorities":["行程轻松","住宿品质"]}
                """));

        assertEquals("PRIORITY", result.path("plan_mode").asText());
        assertEquals("行程轻松", result.path("collected_information").path("priorities").get(0).asText());
    }

    @Test
    void shouldRequireDateNormalizationBeforeMap() throws Exception {
        ObjectNode result = service.handle("u3", objectMapper.readTree("""
                {"action":"collect","departure_city":"南京","participant_count":10,
                 "travel_date":"下周六","duration":"1天","budget_total":12000,
                 "destination":"扬州"}
                """));

        assertEquals("READY_FOR_DATE", result.path("status").asText());
        assertEquals("time_query", result.path("next_tool").asText());
    }

    @Test
    void shouldResolveYearlessMonthDayWithoutTimeToolRoundTrip() throws Exception {
        LocalDate expected = LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(10);
        String monthDay = expected.getMonthValue() + "月" + expected.getDayOfMonth() + "日";

        ObjectNode result = service.handle("u-date", objectMapper.readTree("""
                {"action":"collect","departure_city":"合肥","participant_count":30,
                 "travel_date":"%s","duration":"2天1晚","budget_total":20000,
                 "destination":"杭州"}
                """.formatted(monthDay)));

        assertEquals("READY_FOR_MAP", result.path("status").asText());
        assertEquals(expected.toString(),
                result.path("collected_information").path("travelDate").asText());
    }

    @Test
    void shouldNormalizeCommonModelAliasesAndCamelCaseOptions() throws Exception {
        TeamTripPlanFunction function = new TeamTripPlanFunction(
                service, objectMapper, new LLMFunctionRegistry());

        function.execute("""
                {"action":"collect","origin":"上海","people":30,"start_date":"2026-08-05",
                 "days":2,"budget":20000,"destination":"无锡"}
                """, new FunctionExecutionContext("u-alias", "继续生成"));
        TeamTripPlanDraft draft = store.get("u-alias");
        assertEquals("上海", draft.getDepartureCity());
        assertEquals(30, draft.getParticipantCount());
        assertEquals(20000d, draft.getBudgetTotal());

        function.execute("""
                {"action":"save_options","option_count":1,"options":[{
                  "optionId":"plan_a","displayName":"太湖方案",
                  "positioning":"均衡型","itinerarySummary":"太湖两日行程"
                }]}
                """, new FunctionExecutionContext("u-alias", "保存方案"));
        assertEquals("plan_a", store.get("u-alias").getOptions().get(0).getOptionId());
        assertEquals("太湖方案", store.get("u-alias").getOptions().get(0).getDisplayName());
    }

    @Test
    void shouldEnforceMapBeforeWebSearchAndPreventSameBatchFallback() throws Exception {
        service.handle("u4", objectMapper.readTree("""
                {"action":"collect","departure_city":"杭州","participant_count":20,
                 "travel_date":"2026-08-15","duration":"2天1晚","budget_per_person":1200,
                 "destination":"安吉"}
                """));

        assertTrue(policy.validate("u4", "web_search", List.of("web_search")).contains("地图"));
        assertNull(policy.validate("u4", "map_search_place", List.of("map_search_place")));

        service.recordToolResult("u4", "map_search_place",
                "{\"status\":\"PARTIAL\",\"fallback_required\":true}");
        assertNull(policy.validate("u4", "web_search", List.of("web_search")));
        assertTrue(policy.validate("u4", "map_search_place",
                List.of("map_search_place")).contains("停止重复"));
        assertTrue(policy.validate("u4", "web_search",
                List.of("map_search_place", "web_search")).contains("同一轮"));
    }

    @Test
    void shouldReviseOnlyAffectedStrategyAndIncrementVersion() throws Exception {
        service.handle("u5", objectMapper.readTree("""
                {"action":"collect","departure_city":"杭州","participant_count":20,
                 "travel_date":"2026-08-15","duration":"2天1晚","budget_per_person":1200,
                 "destination":"安吉"}
                """));

        ObjectNode revision = service.handle("u5", objectMapper.readTree("""
                {"action":"revise","feedback":"还是太贵了"}
                """));
        assertEquals("REVISING", revision.path("status").asText());
        assertEquals(2, revision.path("version").asInt());
        assertEquals("AWAITING_ADJUSTMENT_PREFERENCE", revision.path("stage").asText());
        assertEquals("none", revision.path("next_tool").asText());
        assertTrue(revision.path("instruction").asText().contains("保留"));
    }

    @Test
    void shouldRequireNumericBudgetInsteadOfBudgetLevel() throws Exception {
        ObjectNode result = service.handle("u6", objectMapper.readTree("""
                {"action":"collect","departure_city":"上海","participant_count":20,
                 "travel_date":"2026-09-05","duration":"2天1晚","budget_level":"标准型",
                 "destination":"无锡"}
                """));

        assertEquals("NEED_MORE_INFORMATION", result.path("status").asText());
        assertTrue(result.path("missing_fields").toString().contains("budget"));
    }

    @Test
    void shouldCostMultipleOptionsThenWaitForSelectionAndOverrunDecision() throws Exception {
        service.handle("u7", objectMapper.readTree("""
                {"action":"collect","departure_city":"上海","participant_count":30,
                 "travel_date":"2026-09-05","duration":"2天1晚","budget_total":35000,
                 "destination":"无锡","option_count":2}
                """));
        ObjectNode saved = service.handle("u7", objectMapper.readTree("""
                {"action":"save_options","options":[
                  {"option_id":"A","display_name":"方案A","positioning":"经济型","itinerary_summary":"经济住宿和轻量活动"},
                  {"option_id":"B","display_name":"方案B","positioning":"均衡型","itinerary_summary":"品质住宿和团队活动"}
                ]}
                """));
        assertEquals("OPTIONS_READY_FOR_COSTING", saved.path("status").asText());

        service.recordToolResult("u7", "budget_calculator", """
                {"status":"SUCCESS","plans":[
                  {"planId":"A","planName":"方案A","planVersion":1,"costStatus":"SUCCESS",
                   "estimatedTotalMin":32800,"estimatedTotalMax":32800,
                   "targetBudget":35000,"budgetStatus":"WITHIN_BUDGET","overrunMin":0,"overrunMax":0},
                  {"planId":"B","planName":"方案B","planVersion":1,"costStatus":"SUCCESS",
                   "estimatedTotalMin":36800,"estimatedTotalMax":36800,
                   "targetBudget":35000,"budgetStatus":"OVER_BUDGET",
                   "overrunMin":1800,"overrunMax":1800,"overrunRateMax":5.14}
                ]}
                """);
        assertEquals("AWAITING_OPTION_SELECTION", store.get("u7").getStage());
        assertTrue(policy.shouldReplyWithoutTools("u7"));
        assertTrue(policy.validate("u7", "web_search", List.of("web_search")).contains("等待用户"));

        ObjectNode selected = service.handle("u7", objectMapper.readTree("""
                {"action":"select_option","selected_option_id":"B"}
                """));
        assertEquals("AWAITING_BUDGET_DECISION", selected.path("status").asText());
        assertEquals(1800, selected.path("budget_question").path("overrun_max").asInt());

        ObjectNode accepted = service.handle("u7", objectMapper.readTree("""
                {"action":"budget_decision","budget_decision":"ACCEPT_OVERRUN"}
                """));
        assertEquals("OVERRUN_ACCEPTED", accepted.path("status").asText());
        assertEquals("FINALIZABLE", accepted.path("stage").asText());
        assertEquals(BudgetDecisionStatus.OVERRUN_ACCEPTED,
                store.get("u7").getBudgetDecisionStatus());
    }

    @Test
    void shouldPreventPriceSearchAndCostCalculationInSameBatch() throws Exception {
        service.handle("u8", objectMapper.readTree("""
                {"action":"collect","departure_city":"上海","participant_count":20,
                 "travel_date":"2026-09-05","duration":"2天1晚","budget_total":30000,
                 "destination":"无锡"}
                """));
        service.recordToolResult("u8", "map_search_place", "{\"status\":\"SUCCESS\"}");

        assertTrue(policy.validate("u8", "budget_calculator",
                List.of("web_search", "budget_calculator")).contains("不能"));
    }

    @Test
    void shouldDefaultToThreeOptionsAndAllowAtMostFive() throws Exception {
        ObjectNode result = service.handle("u9", objectMapper.readTree("""
                {"action":"collect","departure_city":"上海","participant_count":20,
                 "travel_date":"2026-09-05","duration":"2天1晚","budget_total":30000,
                 "destination":"无锡"}
                """));
        assertEquals(3, result.path("collected_information").path("optionCount").asInt());

        ObjectNode invalid = service.handle("u9", objectMapper.readTree("""
                {"action":"collect","option_count":6}
                """));
        assertEquals("INVALID_ARGUMENT", invalid.path("status").asText());

        ObjectNode valid = service.handle("u9", objectMapper.readTree("""
                {"action":"collect","option_count":5}
                """));
        assertEquals(5, valid.path("collected_information").path("optionCount").asInt());
    }
}
