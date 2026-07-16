package com.youkeda.exercise.claw.wechat.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 简单回复处理器
 *
 * 当前阶段固定回复 "你好，我是Claw助手"
 * 后续可替换或扩充为 AI 对话
 */
@Slf4j
@Component
public class SimpleReplyHandler implements MessageHandler {

    private static final String REPLY_TEXT = "你好，我是Claw助手";

    @Override
    public String handle(String userId, String text) {
        log.debug("SimpleReplyHandler 处理消息 | from={} | text={}", userId, text);
        return REPLY_TEXT;
    }
}
