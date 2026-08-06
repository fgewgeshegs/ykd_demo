package com.youkeda.exercise.claw.feature.travel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.agent.runtime.TravelDeliveryCredentialSource;
import com.youkeda.exercise.claw.feature.budget.OptionCostResult;
import com.youkeda.exercise.claw.feature.budget.OptionCostStatus;
import com.youkeda.exercise.claw.feature.budget.PlanCostResult;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.user.WechatUserManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TravelPlanService 回归测试：travel_save_options 补 action 后，候选方案必须真正持久化、
 * 可被 travel_select_option 选中，且核算凭证能被守卫读取。
 */
class TravelPlanServiceTest {

    private static final String OWNER = "owner";

    private ObjectMapper mapper;
    private DefaultTravelPlanStateStore store;
    private TravelPlanService service;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        store = new DefaultTravelPlanStateStore();
        WechatUserManager userManager = mock(WechatUserManager.class);
        when(userManager.getOwnerUserId()).thenReturn(OWNER);
        service = new TravelPlanService(store, mapper, userManager);
    }

    private void collectRequirements() {
        ObjectNode args = mapper.createObjectNode();
        args.put("departure_city", "杭州");
        args.put("participant_count", 1);
        args.put("travel_date", "2026-08-12");
        args.put("duration", "5天4晚");
        args.put("budget_total", 4000);
        args.put("destination", "云南");
        ObjectNode result = service.handle(args);
        assertEquals("ALL_COLLECTED", result.path("status").asText());
    }

    private ObjectNode saveTwoOptions() {
        ObjectNode args = mapper.createObjectNode();
        args.put("action", "save_options");
        args.put("option_count", 2);
        ArrayNode options = args.putArray("options");
        options.addObject()
                .put("option_id", "plan_a")
                .put("display_name", "方案A")
                .put("positioning", "经济型")
                .put("itinerary_summary", "D1 杭州→昆明，宿青旅");
        options.addObject()
                .put("option_id", "plan_b")
                .put("display_name", "方案B")
                .put("positioning", "均衡型")
                .put("itinerary_summary", "D1 杭州→昆明，宿客栈");
        return service.handle(args);
    }

    @Test
    void saveOptionsPersistsAndCanBeSelected() {
        collectRequirements();
        ObjectNode saveResult = saveTwoOptions();

        assertEquals("OPTIONS_SAVED", saveResult.path("status").asText(),
                "save_options 必须落到 saveOptions 分支，而非 default→collect");
        assertEquals(2, saveResult.path("option_count").asInt());

        // 跨轮 select：能选中 plan_a（回归：曾因 action 缺失导致"未找到候选方案"）
        ObjectNode select = mapper.createObjectNode();
        select.put("action", "select_option");
        select.put("selected_option_id", "plan_a");
        ObjectNode selectResult = service.handle(select);

        assertEquals("OPTION_SELECTED", selectResult.path("status").asText());
        assertEquals("plan_a", selectResult.path("selected_option_id").asText());
    }

    @Test
    void recordCostCalculationExposesCostCredential() {
        collectRequirements();
        saveTwoOptions();

        PlanCostResult cost = new PlanCostResult();
        cost.setStatus("SUCCESS");
        OptionCostResult optionA = new OptionCostResult();
        optionA.setPlanId("plan_a");
        optionA.setCostStatus(OptionCostStatus.SUCCESS);
        optionA.setEstimatedTotalMin(new BigDecimal("1000"));
        cost.getPlans().add(optionA);
        service.recordCostCalculation(OWNER, cost);

        // 凭证：costCalculated=true 且 options 里 plan_a 有 costResult
        TravelDeliveryCredentialSource.DeliveryCredential credential =
                service.getCredential(OWNER).orElseThrow();
        assertTrue(credential.costCalculated());
        assertTrue(credential.requirementsComplete());

        TravelPlanDraft draft = store.get(OWNER);
        assertNotNull(draft, "核算后 draft 应仍存在");
        assertNotNull(draft.getOptions().get(0).getCostResult(),
                "plan_a 的 costResult 应回写（守卫跨轮凭证的数据源）");
    }

    @Test
    void partialCostYieldsIncompleteCredentialWithMissingItems() {
        collectRequirements();
        saveTwoOptions();

        // PARTIAL 核算：plan_a 缺小七孔门票价格
        PlanCostResult cost = new PlanCostResult();
        cost.setStatus("PARTIAL");
        OptionCostResult optionA = new OptionCostResult();
        optionA.setPlanId("plan_a");
        optionA.setCostStatus(OptionCostStatus.PARTIAL);
        optionA.getMissingPriceItems().add("荔波小七孔门票");
        cost.getPlans().add(optionA);
        service.recordCostCalculation(OWNER, cost);

        TravelDeliveryCredentialSource.DeliveryCredential credential =
                service.getCredential(OWNER).orElseThrow();
        assertTrue(credential.costCalculated(), "PARTIAL 也算已核算（有 costResult）");
        assertEquals(false, credential.costComplete(),
                "PARTIAL 核算必须暴露 costComplete=false（守卫据此要求披露缺失项）");
        assertTrue(credential.costMissingItems().contains("荔波小七孔门票"),
                "缺失项必须带进凭证，供守卫纠正消息点名");
    }

    @Test
    void completeCostYieldsCompleteCredential() {
        collectRequirements();
        saveTwoOptions();

        PlanCostResult cost = new PlanCostResult();
        cost.setStatus("SUCCESS");
        OptionCostResult optionA = new OptionCostResult();
        optionA.setPlanId("plan_a");
        optionA.setCostStatus(OptionCostStatus.SUCCESS);
        optionA.setEstimatedTotalMin(new BigDecimal("1000"));
        cost.getPlans().add(optionA);
        service.recordCostCalculation(OWNER, cost);

        TravelDeliveryCredentialSource.DeliveryCredential credential =
                service.getCredential(OWNER).orElseThrow();
        assertTrue(credential.costCalculated());
        assertTrue(credential.costComplete(), "SUCCESS 核算必须授予完整凭证");
        assertTrue(credential.costMissingItems().isEmpty());
    }

    @Test
    void noPlanStateYieldsEmptyCredential() {
        assertTrue(service.getCredential(OWNER).isEmpty(),
                "无方案状态时凭证应为空（守卫据此判定从未核算/未收集）");
    }

    @Test
    void incompleteRequirementsYieldFalseRequirementsComplete() {
        ObjectNode args = mapper.createObjectNode();
        args.put("departure_city", "杭州"); // 只有出发地，缺人数/日期/预算等
        service.handle(args);

        TravelDeliveryCredentialSource.DeliveryCredential credential =
                service.getCredential(OWNER).orElseThrow();
        assertEquals(false, credential.requirementsComplete(),
                "需求未收集齐时 requirementsComplete 必须为 false");
    }
}
