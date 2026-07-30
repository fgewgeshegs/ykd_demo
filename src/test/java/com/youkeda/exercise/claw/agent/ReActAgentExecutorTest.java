package com.youkeda.exercise.claw.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.memory.ContextStore;
import com.youkeda.exercise.claw.agent.memory.longterm.LongTermMemoryService;
import com.youkeda.exercise.claw.agent.model.PlanState;
import com.youkeda.exercise.claw.agent.model.PlanTask;
import com.youkeda.exercise.claw.agent.plan.DefaultPlanStore;
import com.youkeda.exercise.claw.agent.plan.PlanStore;
import com.youkeda.exercise.claw.agent.plan.PlanValidator;
import com.youkeda.exercise.claw.agent.activity.AgentActivityRecorder;
import com.youkeda.exercise.claw.agent.skill.*;
import com.youkeda.exercise.claw.agent.tool.LLMFunction;
import com.youkeda.exercise.claw.agent.tool.LLMFunctionRegistry;
import com.youkeda.exercise.claw.agent.tool.FunctionExecutionContext;
import com.youkeda.exercise.claw.agent.skill.SkillKnowledgeService;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.ai.llm.LLMResponse;
import com.youkeda.exercise.claw.ai.llm.ToolDefinition;
import com.youkeda.exercise.claw.wechat.user.WechatUserManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReActAgentExecutorTest {

    @Test
    void shouldStopOfferingToolsWhenWholeBatchWasBlocked() {
        Fixture fixture = fixture();
        when(fixture.llmClient.chatWithTools(anyString(), anyList(), anyList()))
                .thenReturn(
                        new LLMResponse(null,
                                List.of(new LLMResponse.ToolCall("tc1", "unknown_tool", "{}")),
                                "tool_calls"),
                        new LLMResponse("请补充必要信息。", List.of(), "stop"));

        String reply = fixture.executor.execute(new AgentContext()
                .setMessage("帮我做方案"));

        assertEquals("请补充必要信息。", reply);
        ArgumentCaptor<List<ToolDefinition>> tools = ArgumentCaptor.forClass(List.class);
        verify(fixture.llmClient, times(2)).chatWithTools(anyString(), anyList(), tools.capture());
        assertFalse(tools.getAllValues().get(0).isEmpty());
        assertTrue(tools.getAllValues().get(1).isEmpty());
    }

    @Test
    void shouldSynthesizeExistingResultsInsteadOfReturningTimeoutAtRoundLimit() {
        Fixture fixture = fixture();
        AtomicInteger calls = new AtomicInteger();
        when(fixture.llmClient.chatWithTools(anyString(), anyList(), anyList())).thenAnswer(invocation -> {
            int call = calls.incrementAndGet();
            if (call <= 12) {
                return new LLMResponse(null,
                        List.of(new LLMResponse.ToolCall(
                                "tc" + call, "dummy_tool", "{\"round\":" + call + "}")),
                        "tool_calls");
            }
            return new LLMResponse("已根据现有结果整理回复。", List.of(), "stop");
        });

        String reply = fixture.executor.execute(new AgentContext()
                .setMessage("生成完整方案"));

        assertEquals("已根据现有结果整理回复。", reply);
        assertEquals(13, calls.get());
    }

    @Test
    void shouldNotReportTimeoutWhenFinalSynthesisStillRequestsTool() {
        Fixture fixture = fixture();
        AtomicInteger calls = new AtomicInteger();
        when(fixture.llmClient.chatWithTools(anyString(), anyList(), anyList())).thenAnswer(invocation -> {
            int call = calls.incrementAndGet();
            return new LLMResponse(null,
                    List.of(new LLMResponse.ToolCall(
                            "tc" + call,
                            call <= 12 ? "dummy_tool" : "budget_calculator",
                            "{\"round\":" + call + "}")),
                    "tool_calls");
        });

        String reply = fixture.executor.execute(new AgentContext()
                .setMessage("生成完整方案"));

        assertTrue(reply.contains("当前可用信息") || reply.contains("已有结果"));
        assertFalse(reply.contains("处理请求超时"));
        assertFalse(reply.contains("继续生成"));
        assertEquals(16, calls.get());
    }

    @Test
    void shouldRemoveLegacyLimitReplyWhenUserContinuesGeneration() {
        Fixture fixture = fixture();
        when(fixture.contextStore.getHistory(anyInt())).thenReturn(List.of(
                new com.youkeda.exercise.claw.agent.memory.Message("user", "生成团建方案"),
                new com.youkeda.exercise.claw.agent.memory.Message("assistant",
                        "本轮处理步骤已达到上限，请回复" + "“" + "继续生成" + "”" + "。"),
                new com.youkeda.exercise.claw.agent.memory.Message("user", "继续生成")));
        when(fixture.llmClient.chatWithTools(anyString(), anyList(), anyList()))
                .thenReturn(new LLMResponse("继续完成方案。", List.of(), "stop"));

        fixture.executor.execute(new AgentContext()
                .setMessage("继续生成"));

        ArgumentCaptor<List<com.youkeda.exercise.claw.agent.memory.Message>> messages =
                ArgumentCaptor.forClass(List.class);
        verify(fixture.llmClient).chatWithTools(anyString(), messages.capture(), anyList());
        assertFalse(messages.getValue().stream()
                .anyMatch(message -> message.content() != null
                        && message.content().contains("本轮处理步骤已达到上限")));
    }

    @Test
    void shouldHandleSimpleChatQuickPath() {
        Fixture fixture = fixture();
        when(fixture.llmClient.chatWithTools(anyString(), anyList(), anyList()))
                .thenReturn(new LLMResponse("你好！有什么可以帮你的？", List.of(), "stop"));

        AgentContext context = new AgentContext()
                .setMessage("你好");
        String reply = fixture.executor.execute(context);

        assertEquals("你好！有什么可以帮你的？", reply);
    }

    @Test
    void shouldIncludeCreateScheduleTaskInToolsWhenCommonSkillActive() {
        // 验证 create_schedule_task 在 common skill 激活时对 LLM 可见
        // 场景：用户说「晚上10点提醒我背单词」
        Fixture fixture = commonSkillFixture();

        when(fixture.llmClient.chatWithTools(anyString(), anyList(), anyList()))
                .thenReturn(new LLMResponse("已创建提醒！", List.of(), "stop"));

        fixture.executor.execute(new AgentContext()
                .setMessage("晚上10点提醒我背单词"));

        ArgumentCaptor<List<ToolDefinition>> toolsCaptor = ArgumentCaptor.forClass(List.class);
        verify(fixture.llmClient).chatWithTools(anyString(), anyList(), toolsCaptor.capture());

        List<ToolDefinition> tools = toolsCaptor.getValue();
        Set<String> toolNames = tools.stream()
                .map(ToolDefinition::name)
                .collect(Collectors.toSet());
        assertTrue(toolNames.contains("create_schedule_task"),
                "common skill 激活时 tools 中应包含 create_schedule_task");
        assertTrue(toolNames.contains("course_schedule"),
                "common skill 激活时 tools 中应包含 course_schedule（不应影响已有工具）");
    }

    @Test
    void shouldIncludeCourseScheduleInToolsForCourseQueries() {
        // 验证课程查询场景下 course_schedule 对 LLM 可见
        // 场景：已有课表用户说「我明天有什么课」
        Fixture fixture = commonSkillFixture();

        when(fixture.llmClient.chatWithTools(anyString(), anyList(), anyList()))
                .thenReturn(new LLMResponse("明天你有以下课程...", List.of(), "stop"));

        fixture.executor.execute(new AgentContext()
                .setMessage("我明天有什么课"));

        ArgumentCaptor<List<ToolDefinition>> toolsCaptor = ArgumentCaptor.forClass(List.class);
        verify(fixture.llmClient).chatWithTools(anyString(), anyList(), toolsCaptor.capture());

        List<ToolDefinition> tools = toolsCaptor.getValue();
        Set<String> toolNames = tools.stream()
                .map(ToolDefinition::name)
                .collect(Collectors.toSet());
        assertTrue(toolNames.contains("course_schedule"),
                "课程查询时 tools 中应包含 course_schedule");
        assertTrue(toolNames.contains("create_schedule_task"),
                "课程查询时 tools 中仍应包含 create_schedule_task（common skill 全量）");
    }

    @Test
    void systemPromptShouldContainReminderGuidanceWhenCommonSkillActive() {
        // 验证 common skill 系统提示中包含课程提醒引导
        // 场景：用户说「每节课半小时前提醒我该上课」
        Fixture fixture = commonSkillFixture();

        when(fixture.llmClient.chatWithTools(anyString(), anyList(), anyList()))
                .thenReturn(new LLMResponse("导入课表后系统会自动提醒。", List.of(), "stop"));

        fixture.executor.execute(new AgentContext()
                .setMessage("每节课半小时前提醒我该上课"));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(fixture.llmClient).chatWithTools(promptCaptor.capture(), anyList(), anyList());

        String prompt = promptCaptor.getValue();
        // base prompt 应存在
        assertTrue(prompt.contains("Claw助手"), "System prompt 应包含基础 prompt");
        // common.txt 中的课程提醒引导应存在（通过 SkillContext 注入）
        assertTrue(prompt.contains("课程提醒") || prompt.contains("自动"),
                "System prompt 应包含课程提醒引导说明");
    }

    private Fixture commonSkillFixture() {
        ObjectMapper objectMapper = new ObjectMapper();
        LLMClient llmClient = mock(LLMClient.class);
        ContextStore contextStore = mock(ContextStore.class);
        when(contextStore.getHistory(anyInt())).thenReturn(List.of());

        LLMFunctionRegistry registry = new LLMFunctionRegistry();

        // Register dummy_tool (global tool)
        registry.register(new LLMFunction() {
            @Override
            public String getName() { return "dummy_tool"; }
            @Override
            public String getDescription() { return "测试工具"; }
            @Override
            public JsonNode getParameters() { return objectMapper.createObjectNode().put("type", "object"); }
            @Override
            public String execute(String argumentsJson) { return "{\"status\":\"SUCCESS\"}"; }
        });

        // Register create_schedule_task (the tool being added)
        registry.register(new LLMFunction() {
            @Override
            public String getName() { return "create_schedule_task"; }
            @Override
            public String getDescription() { return "创建定时提醒任务"; }
            @Override
            public JsonNode getParameters() { return objectMapper.createObjectNode().put("type", "object"); }
            @Override
            public String execute(String argumentsJson) { return "{\"status\":\"SUCCESS\"}"; }
        });

        // Register course_schedule (existing tool)
        registry.register(new LLMFunction() {
            @Override
            public String getName() { return "course_schedule"; }
            @Override
            public String getDescription() { return "课程表管理"; }
            @Override
            public JsonNode getParameters() { return objectMapper.createObjectNode().put("type", "object"); }
            @Override
            public String execute(String argumentsJson) { return "{\"status\":\"SUCCESS\"}"; }
        });

        PlanStore planStore = new DefaultPlanStore();
        PlanValidator planValidator = new PlanValidator();
        SafetyPolicy safetyPolicy = new SafetyPolicy();
        LongTermMemoryService longTermMemoryService = mock(LongTermMemoryService.class);
        when(longTermMemoryService.recall(anyString())).thenReturn(List.of());

        // Route to common skill with ACTIVATE action
        SkillRouter skillRouter = mock(SkillRouter.class);
        when(skillRouter.route(anyString(), anyString()))
                .thenReturn(new SkillRoutingResult("common", Set.of(),
                        SkillRoutingResult.SkillRoutingAction.ACTIVATE, 1.0, "test"));

        SkillSessionStore skillSessionStore = mock(SkillSessionStore.class);
        when(skillSessionStore.find(anyString())).thenReturn(Optional.empty());

        // Common SkillDefinition with all expected tools including create_schedule_task
        SkillDefinition commonSkill = new SkillDefinition(
                "common", "通用能力", 0, Set.of("通用"),
                Set.of(),  // requiredTools
                Set.of("course_schedule", "exam_schedule", "exam_reminder_setup",
                       "create_schedule_task", "web_search", "holiday_check"),  // optionalTools
                "prompts/skills/common.txt", null, null, true);

        SkillRegistry skillRegistry = mock(SkillRegistry.class);
        when(skillRegistry.find("common")).thenReturn(Optional.of(commonSkill));

        SkillsProperties skillsProperties = new SkillsProperties();
        skillsProperties.setGlobalTools(new java.util.LinkedHashSet<>(Set.of("dummy_tool")));

        WechatUserManager wechatUserManager = mock(WechatUserManager.class);
        when(wechatUserManager.getOwnerUserId()).thenReturn("test-user");
        when(llmClient.getSystemPrompt()).thenReturn("你是 Claw助手，一个智能AI助手。");

        ReActAgentExecutor executor = new ReActAgentExecutor(
                llmClient, registry, contextStore, objectMapper,
                planStore, planValidator, safetyPolicy, longTermMemoryService,
                skillRouter, skillSessionStore, skillRegistry, skillsProperties, wechatUserManager,
                mock(SkillKnowledgeService.class),
                mock(AgentActivityRecorder.class),
                mock(SkillPendingCoordinator.class),
                mock(SkillToolFallbackPolicy.class),
                mock(ToolResultStatusParser.class));
        return new Fixture(llmClient, executor, contextStore);
    }

    private Fixture fixture() {
        ObjectMapper objectMapper = new ObjectMapper();
        LLMClient llmClient = mock(LLMClient.class);
        ContextStore contextStore = mock(ContextStore.class);
        when(contextStore.getHistory(anyInt())).thenReturn(List.of());

        LLMFunctionRegistry registry = new LLMFunctionRegistry();
        registry.register(new LLMFunction() {
            @Override
            public String getName() {
                return "dummy_tool";
            }

            @Override
            public String getDescription() {
                return "测试工具";
            }

            @Override
            public com.fasterxml.jackson.databind.JsonNode getParameters() {
                return objectMapper.createObjectNode().put("type", "object");
            }

            @Override
            public String execute(String argumentsJson) {
                return "{\"status\":\"SUCCESS\"}";
            }
        });

        PlanStore planStore = new DefaultPlanStore();
        PlanValidator planValidator = new PlanValidator();
        SafetyPolicy safetyPolicy = new SafetyPolicy();
        LongTermMemoryService longTermMemoryService = mock(LongTermMemoryService.class);
        when(longTermMemoryService.recall(anyString())).thenReturn(List.of());

        // Skill dependencies (mocked to fallback to common mode)
        SkillRouter skillRouter = mock(SkillRouter.class);
        when(skillRouter.route(anyString(), anyString()))
                .thenReturn(SkillRoutingResult.fallback());
        SkillSessionStore skillSessionStore = mock(SkillSessionStore.class);
        when(skillSessionStore.find(anyString())).thenReturn(java.util.Optional.empty());
        SkillRegistry skillRegistry = mock(SkillRegistry.class);
        SkillsProperties skillsProperties = new SkillsProperties();
        skillsProperties.setGlobalTools(new java.util.LinkedHashSet<>(Set.of("dummy_tool")));
        WechatUserManager wechatUserManager = mock(WechatUserManager.class);
        when(wechatUserManager.getOwnerUserId()).thenReturn("test-user");
        when(llmClient.getSystemPrompt()).thenReturn("你是 Claw助手，一个智能AI助手。");

        ReActAgentExecutor executor = new ReActAgentExecutor(
                llmClient, registry, contextStore, objectMapper,
                planStore, planValidator, safetyPolicy, longTermMemoryService,
                skillRouter, skillSessionStore, skillRegistry, skillsProperties, wechatUserManager,
                mock(SkillKnowledgeService.class),
                mock(AgentActivityRecorder.class),
                mock(SkillPendingCoordinator.class),
                mock(SkillToolFallbackPolicy.class),
                mock(ToolResultStatusParser.class));
        return new Fixture(llmClient, executor, contextStore);
    }

    private record Fixture(LLMClient llmClient, ReActAgentExecutor executor,
                           ContextStore contextStore) {
    }
}
