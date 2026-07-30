package com.youkeda.exercise.claw.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.activity.AgentActivityRecorder;
import com.youkeda.exercise.claw.agent.memory.ContextStore;
import com.youkeda.exercise.claw.agent.memory.longterm.LongTermMemoryService;
import com.youkeda.exercise.claw.agent.plan.PlanStore;
import com.youkeda.exercise.claw.agent.plan.PlanValidator;
import com.youkeda.exercise.claw.agent.skill.*;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.wechat.user.WechatUserManager;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ReActAgentExecutorSkillExecutionTest {

    @Test
    void backgroundSkillShortCircuitsLlmToolLoop() {
        LLMClient llmClient = mock(LLMClient.class);
        SkillRouter skillRouter = mock(SkillRouter.class);
        SkillSessionStore sessionStore = mock(SkillSessionStore.class);
        SkillRegistry skillRegistry = mock(SkillRegistry.class);
        SkillExecutionDispatcher dispatcher = mock(SkillExecutionDispatcher.class);
        AgentActivityRecorder activityRecorder = mock(AgentActivityRecorder.class);
        when(activityRecorder.beginRequest()).thenReturn("request-1");
        when(skillRouter.route("最近有什么值得关注", "owner"))
                .thenReturn(new SkillRoutingResult(
                        "information-scout", Set.of(),
                        SkillRoutingResult.SkillRoutingAction.ACTIVATE,
                        0.95, "explicit request"));
        when(sessionStore.find("owner")).thenReturn(Optional.empty());
        SkillDefinition skill = new SkillDefinition(
                "information-scout", "test", 3, Set.of(), Set.of(), Set.of(),
                null, "scoutTriggerPolicy", null,
                new SkillExecutionConfig(
                        SkillExecutionMode.BACKGROUND_WORKFLOW,
                        InformationScoutSkillExecutor.NAME),
                true);
        when(skillRegistry.find("information-scout")).thenReturn(Optional.of(skill));
        when(dispatcher.dispatch(eq(skill), eq("最近有什么值得关注"), any()))
                .thenAnswer(invocation -> SkillExecutionResult.handledSilent(
                        invocation.getArgument(2)));

        ReActAgentExecutor executor = new ReActAgentExecutor(
                llmClient,
                mock(LLMFunctionRegistry.class),
                mock(ContextStore.class),
                new ObjectMapper(),
                mock(PlanStore.class),
                mock(PlanValidator.class),
                mock(SafetyPolicy.class),
                mock(LongTermMemoryService.class),
                skillRouter,
                sessionStore,
                skillRegistry,
                new SkillsProperties(),
                mock(WechatUserManager.class),
                mock(SkillKnowledgeService.class),
                activityRecorder,
                mock(SkillPendingCoordinator.class),
                dispatcher,
                mock(ToolResultStatusParser.class));

        String result = executor.execute(new AgentContext()
                .setUserId("owner")
                .setMessage("最近有什么值得关注"));

        assertEquals(ReActAgentExecutor.SILENT_REPLY, result);
        verifyNoInteractions(llmClient);
    }
}
