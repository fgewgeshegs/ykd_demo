package com.youkeda.exercise.claw.agent;

import com.youkeda.exercise.claw.agent.memory.ContextStore;
import com.youkeda.exercise.claw.agent.memory.Message;
import com.youkeda.exercise.claw.agent.memory.MessageRole;
import com.youkeda.exercise.claw.agent.memory.longterm.LongTermMemoryService;
import com.youkeda.exercise.claw.agent.memory.longterm.MemoryItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 对话历史与记忆构建器。
 *
 * <p>从 {@code ReActAgentExecutor} 拆出：拉取历史、按 continuation 请求过滤旧上限提示、
 * 追加当前用户消息、注入长期记忆；并负责将执行循环新增的工具消息持久化。
 * 非 Spring bean，由 {@code ReActAgentExecutor} 构造时用已有依赖创建。
 */
public class MessageHistoryBuilder {

    private static final Logger log = LoggerFactory.getLogger(MessageHistoryBuilder.class);

    private final ContextStore contextStore;
    private final LongTermMemoryService longTermMemoryService;
    private final int maxHistory;

    public MessageHistoryBuilder(ContextStore contextStore,
                                 LongTermMemoryService longTermMemoryService,
                                 int maxHistory) {
        this.contextStore = contextStore;
        this.longTermMemoryService = longTermMemoryService;
        this.maxHistory = maxHistory;
    }

    /**
     * 构建执行循环的初始消息列表：历史（过滤旧上限提示）+ 当前用户消息 + 长期记忆注入。
     */
    public List<Message> buildMessages(String userMessage) {
        List<Message> history = contextStore.getHistory(maxHistory);
        boolean continuationRequest = isContinuationRequest(userMessage);
        List<Message> messages = new ArrayList<>();
        for (Message message : history) {
            if (continuationRequest && isLegacyLimitReply(message)) continue;
            messages.add(message);
        }
        if (!historyContainsCurrentMessage(history, userMessage)) {
            messages.add(new Message("user", userMessage));
        }

        // Long-term memory recall
        List<MemoryItem> recalledMemories = longTermMemoryService.recall(userMessage);
        if (!recalledMemories.isEmpty()) {
            String memoryPrompt = longTermMemoryService.buildMemoryPrompt(recalledMemories);
            messages.add(0, new Message("system", memoryPrompt));
            log.debug("长期记忆已注入 | count={}", recalledMemories.size());
        }
        return messages;
    }

    /**
     * 将本次执行循环中新增的工具调用（assistant 带 tool_calls）与工具结果（tool）消息
     * 持久化到 contextStore，供下一轮对话使用。
     *
     * @param messages          执行循环的完整消息列表（含本轮新增的 tool 相关消息）
     * @param initialMessageCount 执行循环开始前的消息数量（本轮新增的消息从该下标开始）
     */
    public void persistToolMessages(List<Message> messages, int initialMessageCount) {
        if (messages == null) return;
        for (int i = initialMessageCount; i < messages.size(); i++) {
            Message message = messages.get(i);
            if (message == null) continue;
            boolean isToolResult = message.role() == MessageRole.TOOL;
            boolean isToolCall = message.role() == MessageRole.ASSISTANT && message.isToolCall();
            if (isToolResult || isToolCall) {
                contextStore.append(message);
                log.debug("工具消息已持久化 | role={} | toolCallId={} | toolName={}",
                        message.role(), message.toolCallId(), message.toolName());
            }
        }
    }

    private boolean historyContainsCurrentMessage(List<Message> history, String userMessage) {
        if (history.isEmpty() || userMessage == null) return false;
        Message last = history.get(history.size() - 1);
        if (last.role() != MessageRole.USER || last.content() == null) return false;
        return last.content().equals(userMessage) || last.content().equals("[语音]" + userMessage);
    }

    /** 判断是否为「继续生成」类延续请求（fast-path 与消息构建共用） */
    public boolean isContinuationRequest(String userMessage) {
        if (userMessage == null) return false;
        String normalized = userMessage.replaceAll("[\\s，。！!？?]", "");
        return Set.of("继续生成", "继续", "接着生成", "继续完成方案").contains(normalized);
    }

    private boolean isLegacyLimitReply(Message message) {
        if (message == null || message.role() != MessageRole.ASSISTANT || message.content() == null) {
            return false;
        }
        return message.content().contains("本轮处理步骤已达到上限")
                || message.content().contains("请回复\"继续生成\"")
                || message.content().contains("请回复“继续生成”");
    }
}
