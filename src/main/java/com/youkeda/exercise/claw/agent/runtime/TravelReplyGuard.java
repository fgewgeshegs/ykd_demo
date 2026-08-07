package com.youkeda.exercise.claw.agent.runtime;

import com.youkeda.exercise.claw.agent.model.ResultStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 旅游规划交付不变量守卫。
 *
 * <p>只做「交付质量」确定性校验，不强制流程：
 * <ul>
 *   <li>声称完成行程，但需求未收集齐（travel_collect 凭证缺失）→ 拦截</li>
 *   <li>回复含预算结论（总费用/人均/预算内/超预算），但无有效核算凭证（travel_calculate_cost）→ 拦截</li>
 * </ul>
 *
 * <p>凭证判定 = 「本轮 toolStatuses」∪「跨轮业务凭证」：
 * 工具结果默认只进当轮 toolStatuses（且 travel_collect 的 ALL_COLLECTED 被 Parser 归为 FAILED），
 * 若只看本轮，选方案/汇报轮引用上轮已核算金额会被误拦并触发重试死循环。
 * 跨轮凭证由 {@link TravelDeliveryCredentialSource}（TravelPlanService 实现）持久化提供。
 *
 * <p>与 cxx-tools 版本的区别：不依赖 SkillSession 的 travel 常量（Session/Plan 边界），
 * 不含「天数不得超过用户要求」规则——该规则属策略，由 travel.txt prompt 引导，
 * 避免 guard 用正则猜用户天数造成误伤（用户说「3 天 2 晚」时 2 晚不是第 3 天）。
 */
@Component
public class TravelReplyGuard implements SkillReplyGuard {

    private static final Pattern COMPLETED_PLAN = Pattern.compile(
            "行程总览|行程安排|行程已生成|规划好了|方案如下|Day\\s*1|第一天");

    private static final Pattern BUDGET_SUMMARY = Pattern.compile(
            "总费用|总价|人均费用|人均价|预算内|超预算|合计.*元|共.*元");

    /** 缺失价格披露信号：核算不完整时，回复必须包含此类措辞才能给出金额结论。 */
    private static final Pattern PRICE_DISCLOSURE = Pattern.compile(
            "待确认|未确认|待定|暂估|估算|预估|待核实|价格未知|待补充|未包含|不含|仅供参考");

    private final TravelDeliveryCredentialSource credentialSource;

    public TravelReplyGuard(TravelDeliveryCredentialSource credentialSource) {
        this.credentialSource = credentialSource;
    }

    @Override
    public String getSkillName() {
        return "travel";
    }

    @Override
    public GuardResult validate(GuardContext context) {
        if (context.reply() == null || context.reply().isBlank()) {
            return GuardResult.allow();
        }
        Map<String, ResultStatus> statuses = context.toolStatuses() != null
                ? context.toolStatuses() : Map.of();
        boolean collectedThisRound = statuses.get("travel_collect") == ResultStatus.SUCCESS;
        boolean costThisRound = statuses.get("travel_calculate_cost") == ResultStatus.SUCCESS;

        // 跨轮凭证：业务侧持久化的需求齐全度 + 核算凭证
        TravelDeliveryCredentialSource.DeliveryCredential credential = null;
        if (context.session() != null && context.session().userId() != null) {
            credential = credentialSource.getCredential(context.session().userId()).orElse(null);
        }
        boolean collected = collectedThisRound
                || (credential != null && credential.requirementsComplete());
        boolean costCalculated = costThisRound
                || (credential != null && credential.costCalculated());
        // 完整度：本轮工具返回 SUCCESS 即完整核算；否则看跨轮凭证的完整度
        boolean costComplete = costThisRound
                || (credential != null && credential.costComplete());

        // 不变量 1：旅行请求 + 声称完成行程 + 需求未收集齐 → 拦截
        boolean claimsCompleted = COMPLETED_PLAN.matcher(context.reply()).find();
        if (claimsCompleted && !collected) {
            return GuardResult.reject(
                    "当前是旅游规划请求，但你尚未调用 travel_collect 收集齐需求，"
                            + "或该工具仍返回缺失字段。请先调用 travel_collect 记录已提供信息，"
                            + "根据返回的 missing_fields 追问缺失项；需求未齐全前不得声称已完成行程。");
        }

        // 不变量 2：预算结论必须有核算凭证（本轮调用，或跨轮已有有效核算结果）
        boolean claimsBudget = BUDGET_SUMMARY.matcher(context.reply()).find();
        if (claimsBudget && !costCalculated) {
            return GuardResult.reject(
                    "你的回复包含总费用/人均费用/预算结论，但尚未有对应的 travel_calculate_cost 核算凭证。"
                            + "请先调用 travel_calculate_cost 核算后再给出金额结论。");
        }

        // 不变量 2b：核算不完整（PARTIAL，存在缺失价格项）时，金额结论必须如实披露缺失项
        if (claimsBudget && costCalculated && !costComplete) {
            if (!PRICE_DISCLOSURE.matcher(context.reply()).find()) {
                String missing = (credential != null && !credential.costMissingItems().isEmpty())
                        ? String.join("、", credential.costMissingItems())
                        : "部分费用项";
                return GuardResult.reject(
                        "你的回复给出总费用/人均费用/预算结论，但核算结果不完整，以下费用项价格未确认："
                                + missing + "。请在回复中如实标注这些项为「待确认/估算」，"
                                + "不得给出确定的总费用结论；或补充缺失价格后重新调用 travel_calculate_cost 核算。");
            }
        }

        return GuardResult.allow();
    }
}
