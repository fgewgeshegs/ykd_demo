package com.youkeda.exercise.claw.agent.runtime;

import java.util.Optional;

/**
 * 旅游交付凭证源：让回复守卫能读取「跨轮」的业务状态，判断预算/需求结论是否有真实工具凭证。
 *
 * <p>背景：travel_collect 返回 {@code ALL_COLLECTED} 被 {@link com.youkeda.exercise.claw.agent.ToolResultStatusParser}
 * 解析为 FAILED，travel_calculate_cost 结果是纯工具、不写业务状态——守卫只看当轮 toolStatuses
 * 无法区分「引用上轮已核算金额」与「新提出未核算结论」，导致选方案/汇报轮反复撞同一个守卫（重试死循环）。
 *
 * <p>此接口由业务侧（TravelPlanService）实现：把需求齐全度（collect 凭证）与最新核算结果
 * （cost 凭证）持久化后暴露给守卫，守卫据此做跨轮校验。
 */
public interface TravelDeliveryCredentialSource {

    /**
     * 交付凭证。
     *
     * @param requirementsComplete 需求已收集齐（travel_collect 完成凭证）
     * @param costCalculated       存在对当前候选方案的有效核算结果（travel_calculate_cost 完成凭证）
     */
    record DeliveryCredential(boolean requirementsComplete, boolean costCalculated) {
    }

    /** 返回用户当前的交付凭证；用户无方案状态时返回 empty（无凭证）。 */
    Optional<DeliveryCredential> getCredential(String userId);
}
