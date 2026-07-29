package com.youkeda.exercise.claw.scout;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 信息猎手的确定性触发策略。
 *
 * <p>信息猎手开销较大且会主动联网采集，因此不能仅凭用户兴趣、话题相关性、
 * 历史对话或模型推断触发。当前消息必须包含明确的查找信息意图。
 */
public final class ScoutTriggerPolicy {

    private static final Pattern NEGATED_REQUEST = Pattern.compile(
            "(?:不用|不要|别|无需|不需要|禁止|不许).{0,8}(?:查|搜|找|检索|调研|信息猎手)");

    private static final Pattern NON_REQUEST_MENTION = Pattern.compile(
            "(?:(?:为什么|为何|怎么|如何|是否|会不会|能不能|能否|可不可以).{0,12}"
                    + "(?:调用|使用|启动|运行).{0,6}信息猎手)"
                    + "|(?:信息猎手.{0,8}(?:是什么|怎么用|如何用))");

    private static final List<Pattern> EXPLICIT_REQUESTS = List.of(
            Pattern.compile("(?:启动|运行|调用|开启|使用|用一下|让).{0,6}信息猎手"),
            Pattern.compile("信息猎手.{0,6}(?:启动|运行|调用|开启|查|找|搜)"),
            Pattern.compile("(?:帮我|请|麻烦|给我|替我|为我|能不能|可以帮我)?"
                    + "(?:找找|找一下|找一找|搜搜看|搜一下|搜一搜|搜索一下|"
                    + "查查|查一下|查一查|检索一下|调研一下)"),
            Pattern.compile("(?:我想|我需要|想要|需要|给我|推荐|推送).{0,10}"
                    + "(?:信息|资料|资讯|消息|动态|新闻|情报)"),
            Pattern.compile("(?:有什么|有没有).{0,10}"
                    + "(?:新消息|新动态|新资讯|新闻|值得关注)"),
            Pattern.compile("(?:今天|近期|最近).{0,10}有什么值得关注")
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
        if ("信息猎手".equals(normalized)
                || "运行信息猎手".equals(normalized)
                || "启动信息猎手".equals(normalized)) {
            return true;
        }
        return EXPLICIT_REQUESTS.stream()
                .anyMatch(pattern -> pattern.matcher(normalized).find());
    }

    public static boolean isCancellation(String currentMessage) {
        if (currentMessage == null || currentMessage.isBlank()) return false;
        String normalized = currentMessage.replaceAll("\\s+", "");
        return NEGATED_REQUEST.matcher(normalized).find()
                || normalized.matches(".*(?:算了|取消|不用了|不查了|别查了).*");
    }
}
