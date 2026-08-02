package com.youkeda.exercise.claw.agent.runtime;

import java.util.Set;

/**
 * 文本回复防幻觉 guard。
 *
 * <p>内核不感知具体业务（如定时提醒创建）；业务方实现本接口，
 * 在 LLM 直接回复文本前检查是否「声称完成了一个未执行的副作用」，
 * 命中则返回提示消息由 {@link ExecutionLoop} 注入并重试。
 *
 * @return 非 null 表示需注入提示重试；null 表示放行
 */
public interface TextReplyGuard {

    String inspectBeforeReply(String userMessage, String reply, Set<String> executedCalls);
}
