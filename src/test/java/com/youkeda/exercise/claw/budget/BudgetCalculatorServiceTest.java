package com.youkeda.exercise.claw.budget;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BudgetCalculatorServiceTest {

    private BudgetCalculatorService service;

    @BeforeEach
    void setUp() {
        service = new BudgetCalculatorService();
    }

    @Test
    void shouldCalculateActualPlanCostUsingBillingRules() {
        BatchPlanCostRequest request = baseRequest(new BigDecimal("30000"));
        request.setPlans(List.of(option("A",
                item("TRANSPORT", "往返大巴", PricingMode.FIXED, "8000", null, null),
                item("ACCOMMODATION", "双床房", PricingMode.PER_ROOM_PER_NIGHT, "500", null, 2),
                item("MEAL", "四顿团餐", PricingMode.PER_PERSON_PER_OCCURRENCE, "80", 4, null),
                item("ACTIVITY", "团建活动", PricingMode.PER_PERSON, "200", null, null),
                item("INSURANCE", "保险", PricingMode.PER_PERSON, "20", null, null))));

        PlanCostResult result = service.calculate(request);
        OptionCostResult plan = result.getPlans().get(0);

        assertEquals("SUCCESS", result.getStatus());
        assertEquals(new BigDecimal("31700.00"), plan.getKnownSubtotalMin());
        assertEquals(new BigDecimal("34870.00"), plan.getEstimatedTotalMax());
        assertEquals(new BigDecimal("1162.33"), plan.getPerPersonMax());
        assertEquals("OVER_BUDGET", plan.getBudgetStatus());
        assertEquals(new BigDecimal("4870.00"), plan.getOverrunMax());
    }

    @Test
    void shouldReturnPartialWithoutTreatingMissingPriceAsZero() {
        BatchPlanCostRequest request = baseRequest(new BigDecimal("35000"));
        PlanCostItem missing = item("ACTIVITY", "团建活动", PricingMode.PER_PERSON,
                null, null, null);
        missing.setPriceStatus(PriceStatus.MISSING);
        request.setPlans(List.of(option("A",
                item("TRANSPORT", "往返大巴", PricingMode.FIXED, "8000", null, null),
                missing)));

        OptionCostResult plan = service.calculate(request).getPlans().get(0);

        assertEquals("PARTIAL", plan.getCostStatus());
        assertEquals(new BigDecimal("8000.00"), plan.getKnownSubtotalMin());
        assertNull(plan.getEstimatedTotalMax());
        assertTrue(plan.getMissingPriceItems().contains("团建活动"));
        assertEquals("INDETERMINATE", plan.getBudgetStatus());
    }

    @Test
    void shouldCalculateAllOptionsWithSameBudgetAndBuildComparison() {
        BatchPlanCostRequest request = baseRequest(new BigDecimal("35000"));
        request.setContingencyRate(BigDecimal.ZERO);
        request.setPlans(List.of(
                option("A", item("OTHER", "方案A", PricingMode.FIXED, "32000", null, null)),
                option("B", item("OTHER", "方案B", PricingMode.FIXED, "38000", null, null))));

        PlanCostResult result = service.calculate(request);

        assertEquals(List.of("A"), result.getWithinBudgetPlans());
        assertEquals(List.of("B"), result.getOverBudgetPlans());
        assertEquals("A", result.getLowestCostPlan());
        assertEquals("B", result.getHighestCostPlan());
    }

    @Test
    void shouldRequireBudgetForComparison() {
        BatchPlanCostRequest request = baseRequest(null);
        request.setPlans(List.of(option("A",
                item("OTHER", "固定费用", PricingMode.FIXED, "10000", null, null))));

        PlanCostResult result = service.calculate(request);

        assertEquals("MISSING_INFORMATION", result.getStatus());
        assertTrue(result.getWarnings().get(0).contains("预算"));
    }

    @Test
    void shouldRejectConflictingTotalAndPerPersonBudgets() {
        BatchPlanCostRequest request = baseRequest(new BigDecimal("35000"));
        request.setTargetPerPersonBudget(new BigDecimal("2000"));
        request.setPlans(List.of(option("A",
                item("OTHER", "固定费用", PricingMode.FIXED, "10000", null, null))));

        PlanCostResult result = service.calculate(request);

        assertEquals("BUDGET_CONFLICT", result.getStatus());
    }

    @Test
    void shouldMarkRangeCrossingBudgetAsPossiblyOverBudget() {
        BatchPlanCostRequest request = baseRequest(new BigDecimal("35000"));
        request.setContingencyRate(BigDecimal.ZERO);
        PlanCostItem range = item("ACCOMMODATION", "酒店",
                PricingMode.FIXED, null, null, null);
        range.setMinUnitPrice(new BigDecimal("33000"));
        range.setMaxUnitPrice(new BigDecimal("37000"));
        request.setPlans(List.of(option("A", range)));

        OptionCostResult result = service.calculate(request).getPlans().get(0);

        assertEquals("POSSIBLY_OVER_BUDGET", result.getBudgetStatus());
        assertEquals(new BigDecimal("2000.00"), result.getOverrunMax());
    }

    private BatchPlanCostRequest baseRequest(BigDecimal targetBudget) {
        BatchPlanCostRequest request = new BatchPlanCostRequest();
        request.setHeadcount(30);
        request.setDays(2);
        request.setNights(1);
        request.setCity("无锡");
        request.setTargetTotalBudget(targetBudget);
        return request;
    }

    private PlanCostOption option(String id, PlanCostItem... items) {
        PlanCostOption option = new PlanCostOption();
        option.setPlanId(id);
        option.setPlanName("方案" + id);
        option.setItems(List.of(items));
        return option;
    }

    private PlanCostItem item(String category, String name, PricingMode mode,
                              String unitPrice, Integer occurrences, Integer capacity) {
        PlanCostItem item = new PlanCostItem();
        item.setCategory(category);
        item.setItemName(name);
        item.setPricingMode(mode);
        if (unitPrice != null) item.setUnitPrice(new BigDecimal(unitPrice));
        item.setOccurrences(occurrences);
        item.setCapacity(capacity);
        item.setPriceStatus(PriceStatus.ESTIMATED);
        item.setPriceSource("测试报价");
        return item;
    }
}
