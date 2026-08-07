package com.youkeda.exercise.claw.tool.travel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.runtime.ToolExecutionContext;
import com.youkeda.exercise.claw.agent.runtime.ToolRegistry;
import com.youkeda.exercise.claw.agent.skill.SkillSession;
import com.youkeda.exercise.claw.feature.budget.BudgetCalculatorService;
import com.youkeda.exercise.claw.feature.budget.PlanCostResult;
import com.youkeda.exercise.claw.feature.travel.TravelPlanService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * travel_calculate_cost 工具：核算结果必须回写 TravelPlanService（凭证持久化），
 * 供 TravelReplyGuard 跨轮校验；无用户时不回写。
 */
class TravelCalculateCostToolTest {

    @Test
    void recordsCostCredentialAfterCalculate() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        BudgetCalculatorService calculator = mock(BudgetCalculatorService.class);
        TravelPlanService planService = mock(TravelPlanService.class);
        PlanCostResult result = new PlanCostResult();
        result.setStatus("SUCCESS");
        when(calculator.calculate(any())).thenReturn(result);

        TravelCalculateCostTool tool = new TravelCalculateCostTool(
                calculator, planService, mapper, mock(ToolRegistry.class));
        ToolExecutionContext ctx = new ToolExecutionContext(
                "核算费用", SkillSession.create("u1"), "u1");

        String out = tool.execute("{\"headcount\":1,\"days\":5,\"plans\":[]}", ctx);

        assertTrue(out.contains("SUCCESS"), "工具返回内容不应被回写逻辑改变");
        verify(planService).recordCostCalculation("u1", result);
    }

    @Test
    void skipsRecordWhenNoUser() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        BudgetCalculatorService calculator = mock(BudgetCalculatorService.class);
        TravelPlanService planService = mock(TravelPlanService.class);
        when(calculator.calculate(any())).thenReturn(new PlanCostResult());

        TravelCalculateCostTool tool = new TravelCalculateCostTool(
                calculator, planService, mapper, mock(ToolRegistry.class));

        tool.execute("{\"headcount\":1,\"days\":5,\"plans\":[]}", null);

        verify(planService, never()).recordCostCalculation(any(), any());
    }
}
