package com.youkeda.exercise.claw.context;

/**
 * 单条对话消息记录
 *
 * @param role    角色：user / assistant
 * @param content 消息文本
 */
public record Message(String role, String content) {
}
