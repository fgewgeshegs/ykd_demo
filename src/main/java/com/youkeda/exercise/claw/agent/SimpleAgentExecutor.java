package com.youkeda.exercise.claw.agent;

import com.youkeda.exercise.claw.wechat.MessageRouter;
import com.youkeda.exercise.claw.wechat.model.MessageType;
import com.youkeda.exercise.claw.wechat.model.WechatMessage;
import com.youkeda.exercise.claw.wechat.model.WechatReply;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 简易 Agent 执行器
 *
 * 当前实现：桥接 MessageRouter 体系，不改变任何现有行为。
 * 内部流程：AgentContext → WechatMessage → MessageRouter.route() → 回复文本。
 *
 * 未来演进路径：
 * SimpleAgentExecutor（当前）
 *   → IntentAgentExecutor（加入 Planner 替换 IntentClassifier）
 *     → ReActAgentExecutor（Planner → ToolRegistry → Tool 调用循环）
 */
@Slf4j
@Component
public class SimpleAgentExecutor implements AgentExecutor {

    private final MessageRouter messageRouter;

    public SimpleAgentExecutor(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }

    @Override
    public String execute(AgentContext context) {
        log.info("SimpleAgentExecutor 执行 | user={} | type={} | intent={}",
                context.getUserId(), context.getMessageType(), context.getIntent());

        // 桥接：将 AgentContext 转为 WechatMessage，委托 MessageRouter
        WechatMessage wechatMsg = context.getRawMessage();
        if (wechatMsg == null) {
            wechatMsg = new WechatMessage();
            wechatMsg.setUserId(context.getUserId());
            wechatMsg.setType(context.getMessageType());
            wechatMsg.setText(context.getMessage());
        }

        WechatReply wechatReply = messageRouter.route(wechatMsg);
        return wechatReply != null ? wechatReply.getText() : null;
    }
}