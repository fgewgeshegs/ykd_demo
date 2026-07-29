package com.youkeda.exercise.claw.scout.notifier;

/**
 * 信息猎手实际投递结果。
 *
 * @param recommended 价值判断通过数量
 * @param eligible    去重后待发送数量
 * @param delivered   实际发送成功数量
 * @param suppressed  冷却期内被去重数量
 * @param failed      实际发送失败数量
 */
public record NotificationResult(
        int recommended,
        int eligible,
        int delivered,
        int suppressed,
        int failed
) {
    public static NotificationResult empty() {
        return new NotificationResult(0, 0, 0, 0, 0);
    }
}
