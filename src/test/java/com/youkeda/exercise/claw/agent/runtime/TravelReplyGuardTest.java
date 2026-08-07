package com.youkeda.exercise.claw.agent.runtime;

import com.youkeda.exercise.claw.agent.model.ResultStatus;
import com.youkeda.exercise.claw.agent.runtime.SkillReplyGuard.GuardResult;
import com.youkeda.exercise.claw.agent.skill.SkillSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TravelReplyGuardTest {

    private final TravelDeliveryCredentialSource source =
            mock(TravelDeliveryCredentialSource.class);
    private final TravelReplyGuard guard = new TravelReplyGuard(source);

    @BeforeEach
    void setUp() {
        // 默认无跨轮凭证：守卫退回「本轮 toolStatuses」判断（原行为）
        when(source.getCredential(anyString())).thenReturn(Optional.empty());
    }

    private SkillReplyGuard.GuardResult validate(String userMsg, String reply,
                                                 Map<String, ResultStatus> statuses) {
        return guard.validate(new SkillReplyGuard.GuardContext(
                userMsg, reply, SkillSession.create("u"), Set.of(), statuses));
    }

    @Test
    void blocksCompletedPlanWhenCollectNotCalled() {
        // 旅行请求，回复声称已完成行程，但 travel_collect 从未调用（map 为空）→ 拦截
        GuardResult r = validate("我要去三亚玩三天", "已为你规划好三亚三天游行程：Day 1...",
                Map.of());
        assertFalse(r.allowed());
        assertNotNull(r.correction());
    }

    @Test
    void blocksCompletedPlanWhenCollectNeedsMoreInfo() {
        // travel_collect 返回 NEED_MORE_INFORMATION（Parser 解析为 FAILED）→ 只能追问
        GuardResult r = validate("我要去三亚", "已规划好了完整行程",
                Map.of("travel_collect", ResultStatus.FAILED));
        assertFalse(r.allowed());
    }

    @Test
    void allowsCollectingMoreInfoReply() {
        // 回复是追问，且 travel_collect 已执行（PARTIAL 表示仍在收集）→ 放行
        GuardResult r = validate("我要去三亚", "请问一共几个人去呢？",
                Map.of("travel_collect", ResultStatus.PARTIAL));
        assertTrue(r.allowed());
    }

    @Test
    void blocksBudgetConclusionWithoutCostTool() {
        // 回复含预算结论，但未调 travel_calculate_cost → 拦截
        GuardResult r = validate("我要去三亚玩三天，预算五千",
                "总费用约 4800 元，在预算内", Map.of());
        assertFalse(r.allowed());
    }

    @Test
    void allowsReplyAfterCostCalculated() {
        GuardResult r = validate("我要去三亚玩三天", "总费用 4800 元",
                Map.of("travel_calculate_cost", ResultStatus.SUCCESS));
        assertTrue(r.allowed());
    }

    @Test
    void allowsNonTravelGeneralReply() {
        // 回复不含行程完成词、不含预算结论，且无 travel 请求 → 放行
        GuardResult r = validate("三亚现在天气怎么样", "三亚今天晴。",
                Map.of());
        assertTrue(r.allowed());
    }

    // ==================== 跨轮凭证（E 方案） ====================

    @Test
    void allowsBudgetConclusionWithCrossRoundCredential() {
        // 选方案/汇报轮：上轮已核算，跨轮凭证 costCalculated=true，
        // 本轮 toolStatuses 无 calculate 也不误拦（修复重试死循环）
        when(source.getCredential("u")).thenReturn(Optional.of(
                new TravelDeliveryCredentialSource.DeliveryCredential(true, true)));

        GuardResult r = validate("A吧", "方案A预计总费用约 3618 元，在预算内",
                Map.of());
        assertTrue(r.allowed(), "引用上轮已核算金额应放行，不得反复拦截");
    }

    @Test
    void allowsCompletedPlanWithCrossRoundCollectCredential() {
        // 需求已收集齐（跨轮凭证），回复声称完成行程，本轮无 collect 也放行
        when(source.getCredential("u")).thenReturn(Optional.of(
                new TravelDeliveryCredentialSource.DeliveryCredential(true, true)));

        GuardResult r = validate("A吧", "行程已生成：Day 1 昆明，Day 2 大理",
                Map.of());
        assertTrue(r.allowed());
    }

    @Test
    void blocksBudgetConclusionWhenCredentialMissingButIncomplete() {
        // 跨轮凭证存在但需求未收集齐、无核算 → 预算结论仍拦截
        when(source.getCredential("u")).thenReturn(Optional.of(
                new TravelDeliveryCredentialSource.DeliveryCredential(false, false)));

        GuardResult r = validate("帮我规划旅游", "总费用约 3000 元", Map.of());
        assertFalse(r.allowed());
    }

    @Test
    void prefersThisRoundToolCallOverMissingCredential() {
        // 无跨轮凭证，但本轮确实调了 travel_collect(SUCCESS) + calculate(SUCCESS) → 放行
        Map<String, ResultStatus> statuses = Map.of(
                "travel_collect", ResultStatus.SUCCESS,
                "travel_calculate_cost", ResultStatus.SUCCESS);

        GuardResult r = validate("我要去三亚玩三天", "总费用 4800 元，行程已生成",
                statuses);
        assertTrue(r.allowed());
    }

    // ==================== PARTIAL 核算披露不变量（3B 缺口） ====================

    @Test
    void blocksPartialBudgetConclusionWithoutDisclosure() {
        // PARTIAL 凭证：核算不完整，小七孔门票价格缺失。
        // 回复给出确定总费用却未披露缺失项 → 拦截（防 LLM 把未确认价格当确定结果交付）
        when(source.getCredential("u")).thenReturn(Optional.of(
                new TravelDeliveryCredentialSource.DeliveryCredential(
                        true, true, false, java.util.List.of("小七孔门票"))));

        GuardResult r = validate("看看方案A", "方案A总费用4135元，在预算内", Map.of());
        assertFalse(r.allowed(), "PARTIAL 未披露缺失项时必须拦截确定金额");
    }

    @Test
    void allowsPartialBudgetConclusionWithDisclosure() {
        // PARTIAL + 如实披露缺失项 → 放行（合法交付，不会死锁）
        when(source.getCredential("u")).thenReturn(Optional.of(
                new TravelDeliveryCredentialSource.DeliveryCredential(
                        true, true, false, java.util.List.of("小七孔门票"))));

        GuardResult r = validate("看看方案A",
                "方案A总费用约4135元，其中小七孔门票价格待确认，其余已核算", Map.of());
        assertTrue(r.allowed(), "PARTIAL 已披露缺失项应放行，避免重试死锁");
    }

    @Test
    void allowsPartialReplyWithoutAssertingTotal() {
        // PARTIAL + 不给出确定总费用（只披露缺失）→ 放行
        when(source.getCredential("u")).thenReturn(Optional.of(
                new TravelDeliveryCredentialSource.DeliveryCredential(
                        true, true, false, java.util.List.of("小七孔门票"))));

        GuardResult r = validate("看看方案A",
                "方案A已核算，但小七孔门票价格待确认，暂无法给出确定总费用", Map.of());
        assertTrue(r.allowed(), "未断言确定金额时 PARTIAL 可交付");
    }

    @Test
    void correctionNamesMissingItemsForPartial() {
        // 纠正消息必须点名缺失项，让 LLM 有明确出路（修复「纠正指令不可执行」）
        when(source.getCredential("u")).thenReturn(Optional.of(
                new TravelDeliveryCredentialSource.DeliveryCredential(
                        true, true, false, java.util.List.of("小七孔门票"))));

        GuardResult r = validate("看看方案A", "方案A总费用4135元", Map.of());
        assertFalse(r.allowed());
        assertNotNull(r.correction());
        assertTrue(r.correction().contains("小七孔门票"),
                "纠正消息应点名缺失项，LLM 才知道披露什么");
    }
}
