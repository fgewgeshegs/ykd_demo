package com.youkeda.exercise.claw.agent.activity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AgentActivityRecorder {

    private static final Logger log = LoggerFactory.getLogger(AgentActivityRecorder.class);

    private final AgentActivityStore store;

    public AgentActivityRecorder(AgentActivityStore store) {
        this.store = store;
    }

    public String beginRequest() {
        String requestId = UUID.randomUUID().toString();
        record(new AgentActivityEvent(
                requestId, ActivityEventType.REQUEST_RECEIVED,
                null, null, "RUNNING", "收到新请求", null));
        return requestId;
    }

    public void skillSelected(String requestId, String skillName) {
        record(new AgentActivityEvent(
                requestId, ActivityEventType.SKILL_SELECTED,
                skillName, null, "SUCCESS", "选择 " + safeName(skillName) + " Skill", null));
    }

    public void toolStarted(String requestId, String skillName, String toolName) {
        record(new AgentActivityEvent(
                requestId, ActivityEventType.TOOL_STARTED,
                skillName, toolName, "RUNNING", "开始执行工具", null));
    }

    public void toolFinished(String requestId, String skillName, String toolName,
                             boolean success, long durationMs) {
        record(new AgentActivityEvent(
                requestId,
                success ? ActivityEventType.TOOL_SUCCEEDED : ActivityEventType.TOOL_FAILED,
                skillName, toolName, success ? "SUCCESS" : "FAILED",
                success ? "工具执行完成" : "工具执行失败",
                Math.max(0L, durationMs)));
    }

    public void toolBlocked(String requestId, String skillName, String toolName, String reason) {
        record(new AgentActivityEvent(
                requestId, ActivityEventType.TOOL_BLOCKED,
                skillName, toolName, "BLOCKED", safeReason(reason), null));
    }

    public void requestCompleted(String requestId, long durationMs) {
        record(new AgentActivityEvent(
                requestId, ActivityEventType.RESPONSE_COMPLETED,
                null, null, "SUCCESS", "回复已完成", Math.max(0L, durationMs)));
    }

    public void requestFailed(String requestId, String reason, long durationMs) {
        record(new AgentActivityEvent(
                requestId, ActivityEventType.REQUEST_FAILED,
                null, null, "FAILED", safeReason(reason), Math.max(0L, durationMs)));
    }

    private void record(AgentActivityEvent event) {
        try {
            store.record(event);
        } catch (RuntimeException e) {
            log.warn("记录 Agent 活动失败 | type={} | requestId={}",
                    event.eventType(), event.requestId(), e);
        }
    }

    private static String safeName(String value) {
        return value == null || value.isBlank() ? "common" : value;
    }

    private static String safeReason(String value) {
        if (value == null || value.isBlank()) return "未提供原因";
        return value.length() <= 120 ? value : value.substring(0, 120) + "...";
    }
}
