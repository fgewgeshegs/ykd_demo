package com.youkeda.exercise.claw.wechat.handler;

import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 兜底回复处理器
 *
 * 作为最后一级处理器，对前面所有 Handler 未处理的消息类型给出兜底回复
 */
@Slf4j
@Component
public class SimpleReplyHandler implements MessageHandler {

    private static final String FALLBACK_REPLY = "暂时无法理解该消息类型";

    @Override
    public String handle(WechatMessage message) {
        log.debug("SimpleReplyHandler 兜底处理 | from={} | type={}", message.getUserId(), message.getType());
        return FALLBACK_REPLY;
    }
}
