package com.youkeda.exercise.claw.teamtrip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class TeamTripPlanServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final InMemoryTeamTripPlanStateStore store = new InMemoryTeamTripPlanStateStore();
    private final TeamTripPlanService service = new TeamTripPlanService(store, objectMapper);

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
        assertEquals("ALL_COLLECTED", second.path("status").asText());
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
    void shouldResolveYearlessMonthDayWithoutTimeToolRoundTrip() throws Exception {
        LocalDate expected = LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(10);
        String monthDay = expected.getMonthValue() + "月" + expected.getDayOfMonth() + "日";

        ObjectNode result = service.handle("u-date", objectMapper.readTree("""
                {"action":"collect","departure_city":"合肥","participant_count":30,
                 "travel_date":"%s","duration":"2天1晚","budget_total":20000,
                 "destination":"杭州"}
                """.formatted(monthDay)));

        assertEquals("ALL_COLLECTED", result.path("status").asText());
        assertEquals(expected.toString(),
                result.path("collected_information").path("travelDate").asText());
    }

    @Test
    void shouldNormalizeCommonModelAliasesAndCamelCaseOptions() throws Exception {
        String collectArgs = """
                {"origin":"上海","people":30,"start_date":"2026-08-05",
                 "days":2,"budget":20000,"destination":"无锡"}
                """;
        service.handle("u-alias", objectMapper.readTree(
                "{\"action\":\"collect\"," + collectArgs.substring(1)));

        TeamTripPlanDraft draft = store.get("u-alias");
        assertEquals("上海", draft.getDepartureCity());
        assertEquals(30, draft.getParticipantCount());
        assertEquals(20000d, draft.getBudgetTotal());

        service.handle("u-alias", objectMapper.readTree("""
                {"action":"save_options","option_count":1,"options":[{
                  "optionId":"plan_a","displayName":"太湖方案",
                  "positioning":"均衡型","itinerarySummary":"太湖两日行程"
                }]}
                """));
        assertEquals("plan_a", store.get("u-alias").getOptions().get(0).getOptionId());
        assertEquals("太湖方案", store.get("u-alias").getOptions().get(0).getDisplayName());
    }

    @Test
    void shouldSaveOptionsAndSelectOption() throws Exception {
        service.handle("u-opt", objectMapper.readTree("""
                {"action":"collect","departure_city":"上海","participant_count":30,
                 "travel_date":"2026-09-05","duration":"2天1晚","budget_total":35000,
                 "destination":"无锡","option_count":2}
                """));
        ObjectNode saved = service.handle("u-opt", objectMapper.readTree("""
                {"action":"save_options","options":[
                  {"option_id":"A","display_name":"方案A","positioning":"经济型","itinerary_summary":"经济住宿和轻量活动"},
                  {"option_id":"B","display_name":"方案B","positioning":"均衡型","itinerary_summary":"品质住宿和团队活动"}
                ]}
                """));
        assertEquals("OPTIONS_SAVED", saved.path("status").asText());

        ObjectNode selected = service.handle("u-opt", objectMapper.readTree("""
                {"action":"select_option","selected_option_id":"B"}
                """));
        assertEquals("OPTION_SELECTED", selected.path("status").asText());
        assertEquals("B", selected.path("selected_option_id").asText());
    }

    @Test
    void shouldAcceptBudgetDecision() throws Exception {
        service.handle("u-budget", objectMapper.readTree("""
                {"action":"collect","departure_city":"上海","participant_count":20,
                 "travel_date":"2026-09-05","duration":"2天1晚","budget_total":30000,
                 "destination":"无锡","option_count":1}
                """));
        service.handle("u-budget", objectMapper.readTree("""
                {"action":"save_options","option_count":1,"options":[
                  {"option_id":"X","display_name":"方案X","positioning":"经济型","itinerary_summary":"标准行程"}
                ]}
                """));
        service.handle("u-budget", objectMapper.readTree("""
                {"action":"select_option","selected_option_id":"X"}
                """));

        ObjectNode accepted = service.handle("u-budget", objectMapper.readTree("""
                {"action":"budget_decision","budget_decision":"ACCEPT_OVERRUN"}
                """));
        assertEquals("BUDGET_DECISION_ACCEPT_OVERRUN", accepted.path("status").asText());
    }

    @Test
    void shouldReviseAndIncrementVersion() throws Exception {
        service.handle("u5", objectMapper.readTree("""
                {"action":"collect","departure_city":"杭州","participant_count":20,
                 "travel_date":"2026-08-15","duration":"2天1晚","budget_per_person":1200,
                 "destination":"安吉"}
                """));
        ObjectNode revision = service.handle("u5", objectMapper.readTree("""
                {"action":"revise","feedback":"还是太贵了"}
                """));
        assertEquals("REVISION_RECORDED", revision.path("status").asText());
        assertEquals(2, revision.path("version").asInt());
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
