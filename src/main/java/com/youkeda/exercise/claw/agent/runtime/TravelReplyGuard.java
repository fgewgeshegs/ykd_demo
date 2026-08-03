package com.youkeda.exercise.claw.agent.runtime;

import com.youkeda.exercise.claw.agent.skill.SkillPendingCoordinator;
import com.youkeda.exercise.claw.agent.skill.SkillTriggerMatch;
import com.youkeda.exercise.claw.agent.skill.TravelTriggerPolicy;
import com.youkeda.exercise.claw.agent.model.ResultStatus;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 旅游规划必须先落入 travel_collect，防止模型跳过状态与必填信息直接交付方案。 */
@Component
public class TravelReplyGuard implements SkillReplyGuard {

    private static final Pattern ARABIC_DURATION = Pattern.compile("(?<!月)(\\d+)\\s*[天日]");
    private static final Pattern CHINESE_DURATION = Pattern.compile("(?<!月)([零一二三四五六七八九十百千万两]+)\\s*[天日]");
    private static final Pattern ARABIC_DAY = Pattern.compile("(?i)Day\\s*(\\d+)|第\\s*(\\d+)\\s*天");
    private static final Pattern CHINESE_DAY = Pattern.compile("第\\s*([零一二三四五六七八九十百千万两]+)\\s*天");

    private final TravelTriggerPolicy triggerPolicy;

    public TravelReplyGuard(TravelTriggerPolicy triggerPolicy) {
        this.triggerPolicy = triggerPolicy;
    }

    @Override
    public String getSkillName() {
        return "travel";
    }

    @Override
    public GuardResult validate(GuardContext context) {
        ResultStatus collectStatus = context.toolStatuses().get("travel_collect");
        boolean collectedThisTurn = collectStatus == ResultStatus.SUCCESS
                || collectStatus == ResultStatus.PARTIAL;
        boolean pendingCollection = context.session() != null
                && context.session().hasPendingAction(
                SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS);
        if (pendingCollection && (!collectedThisTurn
                || !looksLikePendingClarification(
                context.reply(), context.session().pendingSlot()))) {
            return GuardResult.reject(
                    "travel_collect 仍返回缺失字段。只能追问缺失信息，"
                            + "不得生成或声称已完成完整行程。");
        }
        if (exceedsRequestedDuration(context.userMessage(), context.reply())) {
            return GuardResult.reject(
                    "行程天数超出用户要求。返程必须包含在总天数内，"
                            + "请删除超出的日期并重新安排。");
        }
        ResultStatus costStatus = context.toolStatuses().get("travel_calculate_cost");
        boolean calculatedThisTurn = costStatus == ResultStatus.SUCCESS;
        if (containsBudgetSummary(context.reply()) && !calculatedThisTurn) {
            return GuardResult.reject(
                    "回复包含总费用、人均费用或预算结论，"
                            + "必须先调用 travel_calculate_cost 核算后再回复。");
        }
        if (collectedThisTurn) {
            return GuardResult.allow();
        }

        SkillTriggerMatch trigger = triggerPolicy.match(
                context.userMessage(), Optional.ofNullable(context.session()));
        if (!pendingCollection && !trigger.matched()) {
            return GuardResult.allow();
        }

        return GuardResult.reject(
                "当前是旅游规划请求，但本轮尚未调用 travel_collect。"
                        + "请先把用户已提供的出发地、目的地、日期、天数、人数、预算等信息"
                        + "传给 travel_collect；根据工具返回的缺失字段追问，"
                        + "在需求未齐全前不得直接生成或声称已完成完整行程。");
    }

    private boolean looksLikeCompletedPlan(String reply) {
        if (reply == null) return false;
        String normalized = reply.replaceAll("\\s+", "");
        return normalized.contains("规划好了")
                || normalized.contains("行程总览")
                || normalized.contains("行程安排")
                || Pattern.compile("(?i)Day\\s*1").matcher(reply).find()
                || normalized.contains("第一天");
    }

    private boolean looksLikePendingClarification(String reply, String pendingSlot) {
        if (reply == null || reply.isBlank()) return false;
        String normalized = reply.replaceAll("\\s+", "");
        if (looksLikeCompletedPlan(reply)) return false;
        boolean asksForInformation = normalized.contains("需要")
                || normalized.contains("请问")
                || normalized.contains("请提供")
                || normalized.contains("确认")
                || normalized.contains("补充")
                || normalized.contains("多少")
                || normalized.contains("几人")
                || normalized.contains("？")
                || normalized.contains("?");
        if (!asksForInformation) return false;
        if (pendingSlot == null || pendingSlot.isBlank()) return true;
        return switch (pendingSlot) {
            case "departure_city" -> normalized.contains("出发") || normalized.contains("哪里出发");
            case "participant_count" -> normalized.contains("人数") || normalized.contains("几人")
                    || normalized.contains("多少人");
            case "travel_date" -> normalized.contains("日期") || normalized.contains("时间")
                    || normalized.contains("哪天") || normalized.contains("什么时候");
            case "duration" -> normalized.contains("天数") || normalized.contains("几天")
                    || normalized.contains("多久");
            case "destination" -> normalized.contains("目的地") || normalized.contains("去哪")
                    || normalized.contains("哪里");
            case "budget" -> normalized.contains("预算") || normalized.contains("费用");
            default -> true;
        };
    }

    private boolean containsBudgetSummary(String reply) {
        if (reply == null) return false;
        return reply.contains("总费用")
                || reply.contains("总价")
                || reply.contains("人均费用")
                || reply.contains("预算内")
                || reply.contains("超预算");
    }

    private boolean exceedsRequestedDuration(String request, String reply) {
        BigInteger requestedDays = extractRequestedDays(request);
        BigInteger replyDays = maxDayNumber(reply);
        return requestedDays != null
                && requestedDays.signum() > 0
                && replyDays != null
                && replyDays.compareTo(requestedDays) > 0;
    }

    private BigInteger extractRequestedDays(String text) {
        if (text == null) return null;
        Matcher arabic = ARABIC_DURATION.matcher(text);
        if (arabic.find()) return new BigInteger(arabic.group(1));
        Matcher chinese = CHINESE_DURATION.matcher(text);
        if (chinese.find()) return chineseNumber(chinese.group(1));
        return null;
    }

    private BigInteger maxDayNumber(String text) {
        if (text == null) return null;
        BigInteger max = null;
        Matcher arabic = ARABIC_DAY.matcher(text);
        while (arabic.find()) {
            String value = arabic.group(1) != null ? arabic.group(1) : arabic.group(2);
            BigInteger parsed = new BigInteger(value);
            max = max == null || parsed.compareTo(max) > 0 ? parsed : max;
        }
        Matcher chinese = CHINESE_DAY.matcher(text);
        while (chinese.find()) {
            BigInteger parsed = chineseNumber(chinese.group(1));
            if (parsed != null && (max == null || parsed.compareTo(max) > 0)) {
                max = parsed;
            }
        }
        return max;
    }

    private BigInteger chineseNumber(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.replace('两', '二');
        BigInteger total = BigInteger.ZERO;
        BigInteger section = BigInteger.ZERO;
        BigInteger number = BigInteger.ZERO;
        for (int index = 0; index < normalized.length(); index++) {
            char current = normalized.charAt(index);
            int digit = chineseDigit(current);
            if (digit >= 0) {
                number = BigInteger.valueOf(digit);
                continue;
            }
            int unit = chineseUnit(current);
            if (unit < 0) return null;
            if (unit == 10_000) {
                section = section.add(number);
                if (section.signum() == 0) section = BigInteger.ONE;
                total = total.add(section.multiply(BigInteger.valueOf(unit)));
                section = BigInteger.ZERO;
            } else {
                if (number.signum() == 0) number = BigInteger.ONE;
                section = section.add(number.multiply(BigInteger.valueOf(unit)));
            }
            number = BigInteger.ZERO;
        }
        return total.add(section).add(number);
    }

    private int chineseDigit(char value) {
        return switch (value) {
            case '零' -> 0;
            case '一' -> 1;
            case '二' -> 2;
            case '三' -> 3;
            case '四' -> 4;
            case '五' -> 5;
            case '六' -> 6;
            case '七' -> 7;
            case '八' -> 8;
            case '九' -> 9;
            default -> -1;
        };
    }

    private int chineseUnit(char value) {
        return switch (value) {
            case '十' -> 10;
            case '百' -> 100;
            case '千' -> 1_000;
            case '万' -> 10_000;
            default -> -1;
        };
    }
}
