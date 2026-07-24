package com.youkeda.exercise.claw.agent.tool;

import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 兜底回复处理器
 *
 * 作为最后一级处理器，对前面所有 Handler 未处理的消息类型给出兜底回复
 */
@Component
public class SimpleReplyTool implements WechatMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(SimpleReplyTool.class);

    private static final String FALLBACK_REPLY = "暂时无法理解该消息类型";

    @Override
    public List<WechatReply> handle(WechatMessage message) {
        log.debug("SimpleReplyTool 兜底处理 | from={} | type={}", message.getUserId(), message.getType());
        return List.of(WechatReply.text(FALLBACK_REPLY));
    }
}
