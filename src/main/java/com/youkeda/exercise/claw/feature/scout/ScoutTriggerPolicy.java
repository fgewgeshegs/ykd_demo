package com.youkeda.exercise.claw.feature.scout;

import java.util.regex.Pattern;

/**
 * 信息猎手的确定性触发策略。
 *
 * <p>信息猎手开销较大且会主动联网采集，因此不能仅凭用户兴趣、话题相关性、
 * 历史对话或模型推断触发。当前消息必须包含明确的查找信息意图。
 */
public final class ScoutTriggerPolicy {

    private static final Pattern NEGATED_REQUEST = Pattern.compile(
            "(?:不用|不要|别|无需|不需要|不想|不希望|不愿意|拒绝|禁止|不许)"
                    + ".{0,10}(?:查|搜|找|检索|调研|启动|运行|调用|开启|使用|信息猎手)");

    private static final Pattern NON_REQUEST_MENTION = Pattern.compile(
            "(?:(?:为什么|为何|怎么|如何|是否|会不会|能不能|能否|可不可以).{0,12}"
                    + "(?:调用|使用|启动|运行).{0,6}信息猎手)"
                    + "|(?:信息猎手.{0,8}(?:是什么|怎么用|如何用))");

    private static final Pattern DIRECT_SCOUT_REQUEST = Pattern.compile(
            "(?:启动|运行|调用|开启|使用|用一下|让).{0,6}信息猎手"
                    + "|信息猎手.{0,6}(?:启动|运行|调用|开启|查|找|搜)");

    private static final Pattern DISCOVERY_ACTION = Pattern.compile(
            "(?:看看|看一下|找找|找一下|找一找|搜搜看|搜一下|搜一搜|搜索一下|"
                    + "搜集|收集|汇总|"
                    + "查查|查一下|查一查|检索一下|调研一下|跟踪|追踪|监控|关注)"
    );

    private static final Pattern SCOUT_ACTIVITY = Pattern.compile(
            "(?:最新|最近|近期|本周|今天|动态|更新|版本|新闻|资讯|消息|趋势|机会|"
                    + "政策|变化|岗位|比赛|竞赛|情报|资料|值得关注)"
    );

    private static final Pattern BROAD_DISCOVERY_REQUEST = Pattern.compile(
            "(?:有什么|有没有).{0,12}(?:新消息|新动态|新资讯|新闻|值得关注)"
                    + "|(?:今天|近期|最近).{0,12}有什么值得关注"
    );

    private ScoutTriggerPolicy() {
    }

    /**
     * 只依据当前用户原话判断，不从历史或用户画像推断。
     */
    public static boolean hasExplicitRequest(String currentMessage) {
        if (currentMessage == null || currentMessage.isBlank()) {
            return false;
        }

        String normalized = currentMessage.replaceAll("\\s+", "");
        if (NEGATED_REQUEST.matcher(normalized).find()
                || NON_REQUEST_MENTION.matcher(normalized).find()) {
            return false;
        }
        if ("运行信息猎手".equals(normalized)
                || "启动信息猎手".equals(normalized)) {
            return true;
        }
        if (DIRECT_SCOUT_REQUEST.matcher(normalized).find()
                || BROAD_DISCOVERY_REQUEST.matcher(normalized).find()) {
            return true;
        }
        return DISCOVERY_ACTION.matcher(normalized).find()
                && SCOUT_ACTIVITY.matcher(normalized).find();
    }

    public static boolean isCancellation(String currentMessage) {
        if (currentMessage == null || currentMessage.isBlank()) return false;
        String normalized = currentMessage.replaceAll("\\s+", "");
        return NEGATED_REQUEST.matcher(normalized).find()
                || normalized.matches(".*(?:算了|取消|不用了|不查了|别查了).*");
    }
}
