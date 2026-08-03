package com.youkeda.exercise.claw.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.youkeda.exercise.claw.agent.memory.Message;
import com.youkeda.exercise.claw.agent.plan.PlanStore;
import com.youkeda.exercise.claw.agent.plan.PlanValidator;
import com.youkeda.exercise.claw.agent.runtime.SkillReplyGuard.GuardContext;
import com.youkeda.exercise.claw.agent.runtime.SkillReplyGuard.GuardResult;
import com.youkeda.exercise.claw.agent.skill.SkillSession;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.ai.llm.LLMResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExecutionLoopSkillReplyGuardTest {

    @Test
    void textReplyBlockedByGuardInjectsCorrectionAndRetries() {
        LLMClient llm = mock(LLMClient.class);
        ToolExecutor toolExecutor = mock(ToolExecutor.class);
        PlanValidator validator = mock(PlanValidator.class);
        ObjectMapper om = new ObjectMapper();

        // 第一轮返回文本（会被 guard 拦），第二轮返回放行文本
        when(llm.chatWithTools(any(), any(), any()))
                .thenReturn(new LLMResponse("这是被拦的回复", List.of(), "stop"))
                .thenReturn(new LLMResponse("这是放行的回复", List.of(), "stop"));

        SkillReplyGuardRegistry registry = new SkillReplyGuardRegistry(List.of(
                new SkillReplyGuard() {
                    @Override public String getSkillName() { return "travel"; }
                    @Override public GuardResult validate(GuardContext ctx) {
                        return ctx.reply().contains("被拦")
                                ? GuardResult.reject("先调 travel_collect")
                                : GuardResult.allow();
                    }
                }));

        ExecutionLoop loop = new ExecutionLoop(
                llm, toolExecutor, mock(PlanStore.class), validator, om,
                List.of(), registry);

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", "我要去三亚"));
        ExecutionLoop.Result result = loop.run(
                "sys", messages, List.of(), null,
                mock(ToolExecutionContext.class), SkillSession.create("u"),
                "req", "travel", "我要去三亚");

        assertEquals(ExecutionLoop.LoopStatus.TEXT_REPLY, result.status());
        assertEquals("这是放行的回复", result.reply());
        verify(llm, times(2)).chatWithTools(any(), any(), any());
    }
}
