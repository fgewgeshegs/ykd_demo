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

    // ==================== 提醒能力完整闭环测试 ====================

    /**
     * 【Case 1】用户说「晚上10点提醒我背单词」
     *
     * 路由到 common skill。
     * 预期：
     * 1. 不进入 simple chat 快速路径
     * 2. LLM 可见工具包含 create_schedule_task（不含 campus 工具）
     * 3. LLM 返回 tool_call, 工具执行成功
     * 4. 最终返回文本回复
     */
    @Test
    void integration_callCreateScheduleTaskForCustomReminder() {
        Fixture fixture = commonSkillFixture();

        // Round 1: tool_call for create_schedule_task
        // Round 2: text response after tool execution
        when(fixture.llmClient.chatWithTools(anyString(), anyList(), anyList()))
                .thenReturn(
                        new LLMResponse(null,
                                List.of(new LLMResponse.ToolCall("tc1", "create_schedule_task",
                                        "{\"content\":\"背单词\",\"execute_time\":\"2026-07-30 22:00:00\"}")),
                                "tool_calls"),
                        new LLMResponse("好的，已创建提醒！今晚10点会提醒你背单词。", List.of(), "stop"));

        String reply = fixture.executor.execute(new AgentContext()
                .setMessage("晚上10点提醒我背单词"));

        // 验证：tools 中包含 create_schedule_task，不含 campus 工具
        ArgumentCaptor<List<ToolDefinition>> toolsCaptor = ArgumentCaptor.forClass(List.class);
        verify(fixture.llmClient, atLeastOnce()).chatWithTools(anyString(), anyList(), toolsCaptor.capture());

        Set<String> toolNames = toolsCaptor.getAllValues().get(0).stream()
                .map(ToolDefinition::name)
                .collect(Collectors.toSet());
        assertTrue(toolNames.contains("create_schedule_task"),
                "[Case1] common mode: tools 应包含 create_schedule_task");
        assertFalse(toolNames.contains("course_schedule"),
                "[Case1] common mode: tools 不应包含 course_schedule（已移至 campus skill）");

        // 验证：最终回复正确
        assertEquals("好的，已创建提醒！今晚10点会提醒你背单词。", reply,
                "[Case1] 最终回复应与 mock 一致");
    }

    /**
     * 【Case 2】用户说「每节课半小时前提醒我」
     *
     * 路由到 campus skill。
     * 预期：
     * 1. LLM 可见工具包含 course_schedule（不含 create_schedule_task）
     * 2. system prompt 中包含课程自动提醒引导（campus.txt）
     * 3. LLM 直接回复（提示导入课表，不创建定时提醒）
     */
    @Test
    void integration_shouldNotCallToolForCourseReminder() {
        Fixture fixture = campusSkillFixture();

        // LLM 直接返回文本（不调工具）
        when(fixture.llmClient.chatWithTools(anyString(), anyList(), anyList()))
                .thenReturn(new LLMResponse(
                        "导入课表后系统会自动在上课前30分钟发送微信提醒你，无需手动设置。", List.of(), "stop"));

        String reply = fixture.executor.execute(new AgentContext()
                .setMessage("每节课半小时前提醒我该上课"));

        // 验证：tools 中包含 course_schedule，不含 create_schedule_task
        ArgumentCaptor<List<ToolDefinition>> toolsCaptor = ArgumentCaptor.forClass(List.class);
        verify(fixture.llmClient, atLeastOnce()).chatWithTools(anyString(), anyList(), toolsCaptor.capture());
        Set<String> toolNames = toolsCaptor.getAllValues().get(0).stream()
                .map(ToolDefinition::name)
                .collect(Collectors.toSet());
        assertTrue(toolNames.contains("course_schedule"),
                "[Case2] campus mode: tools 应包含 course_schedule");
        assertFalse(toolNames.contains("create_schedule_task"),
                "[Case2] campus mode: tools 不应包含 create_schedule_task（仅限 common）");

        // 验证：system prompt 包含课程提醒引导说明（来自 campus.txt）
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(fixture.llmClient, atLeastOnce()).chatWithTools(promptCaptor.capture(), anyList(), anyList());
        String systemPrompt = promptCaptor.getValue();
        assertTrue(systemPrompt.contains("课程提醒") || systemPrompt.contains("上课"),
                "[Case2] system prompt 应包含「课程提醒」引导");

        // 验证：最终回复不是工具创建结果，而是自动提醒说明
        assertTrue(reply.contains("自动") || reply.contains("课表"),
                "[Case2] 回复应包含自动提醒或课表相关说明");
        assertFalse(reply.contains("已创建提醒"),
                "[Case2] 不应创建定时提醒");
    }

    /**
     * 【Case 3】用户说「查询今天课程」
     *
     * 路由到 campus skill。
     * 预期：
     * 1. 调用 course_schedule
     * 2. create_schedule_task 不可见（campus 不含此工具）
     */
    @Test
    void integration_callCourseScheduleForQuery() {
        Fixture fixture = campusSkillFixture();

        // Round 1: tool_call for course_schedule
        // Round 2: text response
        when(fixture.llmClient.chatWithTools(anyString(), anyList(), anyList()))
                .thenReturn(
                        new LLMResponse(null,
                                List.of(new LLMResponse.ToolCall("tc1", "course_schedule",
                                        "{\"action\":\"query_today\"}")),
                                "tool_calls"),
                        new LLMResponse("今日课程：08:00-09:40 高等数学...", List.of(), "stop"));

        String reply = fixture.executor.execute(new AgentContext()
                .setMessage("查询今天课程"));

        // 验证：tools 中包含 course_schedule，不含 create_schedule_task
        ArgumentCaptor<List<ToolDefinition>> toolsCaptor = ArgumentCaptor.forClass(List.class);
        verify(fixture.llmClient, atLeastOnce()).chatWithTools(anyString(), anyList(), toolsCaptor.capture());
        Set<String> toolNames = toolsCaptor.getAllValues().get(0).stream()
                .map(ToolDefinition::name)
                .collect(Collectors.toSet());
        assertTrue(toolNames.contains("course_schedule"),
                "[Case3] campus mode: tools 应包含 course_schedule");
        assertFalse(toolNames.contains("create_schedule_task"),
                "[Case3] campus mode: tools 不应包含 create_schedule_task");

        assertEquals("今日课程：08:00-09:40 高等数学...", reply,
                "[Case3] 最终回复应与 mock 一致");
    }

    /**
     * 创建 common mode fixture：相关工具为 create_schedule_task、web_search、holiday_check
     */
    private Fixture commonSkillFixture() {
        ObjectMapper objectMapper = new ObjectMapper();
        LLMClient llmClient = mock(LLMClient.class);
        ContextStore contextStore = mock(ContextStore.class);
        when(contextStore.getHistory(anyInt())).thenReturn(List.of());

        LLMFunctionRegistry registry = baseRegistry(objectMapper);

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

        // Common SkillDefinition — 不含校园工具
        SkillDefinition commonSkill = new SkillDefinition(
                "common", "通用能力", 0, Set.of("通用"),
                Set.of(),  // requiredTools
                Set.of("create_schedule_task", "web_search", "holiday_check"),  // optionalTools
                "prompts/skills/common.txt", null, null, true);

        SkillRegistry skillRegistry = mock(SkillRegistry.class);
        when(skillRegistry.find("common")).thenReturn(Optional.of(commonSkill));

        SkillsProperties skillsProperties = new SkillsProperties();
        skillsProperties.setGlobalTools(new java.util.LinkedHashSet<>(Set.of("dummy_tool")));

        WechatUserManager wechatUserManager = mock(WechatUserManager.class);
        when(wechatUserManager.getOwnerUserId()).thenReturn("test-user");
        when(llmClient.getSystemPrompt()).thenReturn("你是 Claw助手，一个智能AI助手。");

        ReActAgentExecutor executor = createExecutor(llmClient, registry, contextStore, objectMapper,
                planStore, planValidator, safetyPolicy, longTermMemoryService,
                skillRouter, skillSessionStore, skillRegistry, skillsProperties, wechatUserManager);
        return new Fixture(llmClient, executor, contextStore);
    }

    /**
     * 创建 campus mode fixture：相关工具为 course_schedule、exam_schedule、exam_reminder_setup
     */
    private Fixture campusSkillFixture() {
        ObjectMapper objectMapper = new ObjectMapper();
        LLMClient llmClient = mock(LLMClient.class);
        ContextStore contextStore = mock(ContextStore.class);
        when(contextStore.getHistory(anyInt())).thenReturn(List.of());

        LLMFunctionRegistry registry = baseRegistry(objectMapper);

        PlanStore planStore = new DefaultPlanStore();
        PlanValidator planValidator = new PlanValidator();
        SafetyPolicy safetyPolicy = new SafetyPolicy();
        LongTermMemoryService longTermMemoryService = mock(LongTermMemoryService.class);
        when(longTermMemoryService.recall(anyString())).thenReturn(List.of());

        // Route to campus skill with ACTIVATE action
        SkillRouter skillRouter = mock(SkillRouter.class);
        when(skillRouter.route(anyString(), anyString()))
                .thenReturn(new SkillRoutingResult("campus", Set.of(),
                        SkillRoutingResult.SkillRoutingAction.ACTIVATE, 1.0, "test"));

        SkillSessionStore skillSessionStore = mock(SkillSessionStore.class);
        when(skillSessionStore.find(anyString())).thenReturn(Optional.empty());

        // Campus SkillDefinition
        SkillDefinition campusSkill = new SkillDefinition(
                "campus", "校园事务助手，负责个人课表、考试安排、课程提醒、学校相关信息",
                4, Set.of("校园", "课程"),
                Set.of(),  // requiredTools
                Set.of("course_schedule", "exam_schedule", "exam_reminder_setup"),  // optionalTools
                "prompts/skills/campus.txt", null, null, true);

        SkillRegistry skillRegistry = mock(SkillRegistry.class);
        when(skillRegistry.find("campus")).thenReturn(Optional.of(campusSkill));

        SkillsProperties skillsProperties = new SkillsProperties();
        skillsProperties.setGlobalTools(new java.util.LinkedHashSet<>(Set.of("dummy_tool")));

        WechatUserManager wechatUserManager = mock(WechatUserManager.class);
        when(wechatUserManager.getOwnerUserId()).thenReturn("test-user");
        when(llmClient.getSystemPrompt()).thenReturn("你是 Claw助手，一个智能AI助手。");

        ReActAgentExecutor executor = createExecutor(llmClient, registry, contextStore, objectMapper,
                planStore, planValidator, safetyPolicy, longTermMemoryService,
                skillRouter, skillSessionStore, skillRegistry, skillsProperties, wechatUserManager);
        return new Fixture(llmClient, executor, contextStore);
    }

    /**
     * 注册全局 dummy_tool + create_schedule_task + course_schedule
     * （所有工具在 registry 中注册，由 Skill 控制是否暴露给 LLM）
     */
    private LLMFunctionRegistry baseRegistry(ObjectMapper objectMapper) {
        LLMFunctionRegistry registry = new LLMFunctionRegistry();

        registry.register(new LLMFunction() {
            @Override
            public String getName() { return "dummy_tool"; }
            @Override
            public String getDescription() { return "全局测试工具"; }
            @Override
            public JsonNode getParameters() { return objectMapper.createObjectNode().put("type", "object"); }
            @Override
            public String execute(String argumentsJson) { return "{\"status\":\"SUCCESS\"}"; }
        });

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

        return registry;
    }

    /**
     * 创建 ReActAgentExecutor（公共构造逻辑）
     */
    private ReActAgentExecutor createExecutor(LLMClient llmClient, LLMFunctionRegistry registry,
                                               ContextStore contextStore, ObjectMapper objectMapper,
                                               PlanStore planStore, PlanValidator planValidator,
                                               SafetyPolicy safetyPolicy,
                                               LongTermMemoryService longTermMemoryService,
                                               SkillRouter skillRouter,
                                               SkillSessionStore skillSessionStore,
                                               SkillRegistry skillRegistry,
                                               SkillsProperties skillsProperties,
                                               WechatUserManager wechatUserManager) {
        return new ReActAgentExecutor(
                llmClient, registry, contextStore, objectMapper,
                planStore, planValidator, safetyPolicy, longTermMemoryService,
                skillRouter, skillSessionStore, skillRegistry, skillsProperties, wechatUserManager,
                mock(SkillKnowledgeService.class),
                mock(AgentActivityRecorder.class),
                mock(SkillPendingCoordinator.class),
                mock(SkillToolFallbackPolicy.class),
                mock(ToolResultStatusParser.class));
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
