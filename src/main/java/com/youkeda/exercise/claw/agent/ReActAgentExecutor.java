package com.youkeda.exercise.claw.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.memory.ContextStore;
import com.youkeda.exercise.claw.agent.memory.Message;
import com.youkeda.exercise.claw.agent.memory.longterm.LongTermMemoryService;
import com.youkeda.exercise.claw.agent.memory.longterm.MemoryItem;
import com.youkeda.exercise.claw.agent.activity.AgentActivityRecorder;
import com.youkeda.exercise.claw.agent.model.*;
import com.youkeda.exercise.claw.agent.plan.PlanStore;
import com.youkeda.exercise.claw.agent.runtime.ToolRegistry;
import com.youkeda.exercise.claw.agent.runtime.ToolExecutionContext;
import com.youkeda.exercise.claw.agent.runtime.ExecutionLoop;
import com.youkeda.exercise.claw.ai.llm.LLMClient;
import com.youkeda.exercise.claw.ai.llm.LLMResponse;
import com.youkeda.exercise.claw.ai.llm.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.youkeda.exercise.claw.ai.retrieval.SkillKnowledgeService;
import com.youkeda.exercise.claw.skill.SkillDefinition;
import com.youkeda.exercise.claw.skill.SkillExecutionResult;
import com.youkeda.exercise.claw.skill.SkillRegistry;
import com.youkeda.exercise.claw.skill.SkillsProperties;
import com.youkeda.exercise.claw.agent.skill.*;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.user.WechatUserManager;

/**
 * ReAct 模式 Agent 执行器
 *
 * <p>核心调度器：接收用户消息，通过 LLM + Function Calling 的循环自主决定调用哪些工具，
 * 最终给出回复。支持三路 LLM 输出：文本回复、工具调用、结构化计划。
 *
 * <p>执行流程：
 * <ol>
 *   <li>取对话历史 + 当前用户消息</li>
 *   <li>加载当前会话的 PlanState（如果有）</li>
 *   <li>快速判断：明显不需要工具的闲聊直接 LLM 回复（不含工具定义），跳过后续循环</li>
 *   <li>调 LLM（带所有已注册的 {@link Tool} 定义）</li>
 *   <li>LLM 返回
 *     <ul>
 *       <li>文本 → 结束，保存回复到上下文</li>
 *       <li>tool_calls → 逐个执行 → 更新 TaskResult → 结果追加到消息列表 → 回到步骤 3</li>
 *       <li>plan → PlanValidator 校验 → PlanStore.save → 回到步骤 3</li>
 *     </ul>
 *   </li>
 *   <li>达到最大轮次 → 返回当前可用结果</li>
 * </ol>
 */
@Component
public class ReActAgentExecutor implements AgentExecutor {

    private static final Logger log = LoggerFactory.getLogger(ReActAgentExecutor.class);

    /** 每次请求携带的最大历史消息条数 */
    private static final int MAX_HISTORY = 20;

    private static final String ERROR_REPLY = "抱歉，处理请求超时，请稍后再试。";

    public static final String SILENT_REPLY = "__HANDLED_WITHOUT_USER_REPLY__";

    /** 定时任务自动执行时禁用的任务管理类工具，防止 Agent 在自动执行中自我复制/修改任务 */
    static final Set<String> TASK_MANAGEMENT_TOOLS = Set.of(
            "create_schedule_task",
            "cancel_schedule_task",
            "update_schedule_task",
            "pause_agent_task",
            "resume_agent_task",
            "plan_tasks",
            "execute_plan_tasks"
    );

    /**
     * 定时任务自动执行时过滤任务管理类工具。
     *
     * <p>注意：必须在 globalTools + skill.allowedTools 合并之后调用，
     * 确保无论工具来自哪个 Skill 都会被统一过滤。
     *
     * @param effectiveTools        合并后的工具集（会被修改）
     * @param scheduledTaskExecution 是否为定时任务自动执行
     * @return 过滤后的工具集
     */
    static Set<String> filterTaskManagementTools(Set<String> effectiveTools,
                                                 boolean scheduledTaskExecution) {
        if (!scheduledTaskExecution) {
            return effectiveTools;
        }
        effectiveTools.removeAll(TASK_MANAGEMENT_TOOLS);
        return effectiveTools;
    }

    private final LLMClient llmClient;
    private final ToolRegistry functionRegistry;
    private final ContextStore contextStore;
    private final ObjectMapper objectMapper;
    private final PlanStore planStore;
    private final LongTermMemoryService longTermMemoryService;
    private final SkillRouter skillRouter;
    private final SkillSessionStore skillSessionStore;
    private final SkillRegistry skillRegistry;
    private final SkillsProperties skillsProperties;
    private final WechatUserManager wechatUserManager;
    private final SkillKnowledgeService skillKnowledgeService;
    private final AgentActivityRecorder activityRecorder;
    private final SkillExecutionDispatcher skillExecutionDispatcher;
    private final ExecutionLoop executionLoop;

    public ReActAgentExecutor(LLMClient llmClient,
                               ToolRegistry functionRegistry,
                               ContextStore contextStore,
                               ObjectMapper objectMapper,
                               PlanStore planStore,
                               LongTermMemoryService longTermMemoryService,
                               SkillRouter skillRouter,
                               SkillSessionStore skillSessionStore,
                               SkillRegistry skillRegistry,
                               SkillsProperties skillsProperties,
                               WechatUserManager wechatUserManager,
                               SkillKnowledgeService skillKnowledgeService,
                               AgentActivityRecorder activityRecorder,
                               SkillExecutionDispatcher skillExecutionDispatcher,
                               ExecutionLoop executionLoop) {
        this.llmClient = llmClient;
        this.functionRegistry = functionRegistry;
        this.contextStore = contextStore;
        this.objectMapper = objectMapper;
        this.planStore = planStore;
        this.longTermMemoryService = longTermMemoryService;
        this.skillRouter = skillRouter;
        this.skillSessionStore = skillSessionStore;
        this.skillRegistry = skillRegistry;
        this.skillsProperties = skillsProperties;
        this.wechatUserManager = wechatUserManager;
        this.skillKnowledgeService = skillKnowledgeService;
        this.activityRecorder = activityRecorder;
        this.skillExecutionDispatcher = skillExecutionDispatcher;
        this.executionLoop = executionLoop;
    }

    @Override
    public String execute(AgentContext context) {
        long requestStartedAt = System.currentTimeMillis();
        String activityRequestId = activityRecorder.beginRequest();
        String userMessage = context.getMessage();

        log.info("AgentExecutor 执行 | message={}", userMessage);

        // Resolve userId
        String userId = context.getUserId();
        if (userId == null || userId.isBlank()) {
            userId = wechatUserManager.getOwnerUserId();
            context.setUserId(userId);
        }

        // Route through SkillRouter
        SkillRoutingResult routingResult = skillRouter.route(userMessage, userId);
        SkillSession session = updateSession(userId, routingResult);
        context.setSkillSession(session);

        // Get active SkillDefinition
        String activeSkillName = session.activeSkill();
        SkillDefinition activeSkill = skillRegistry.find(activeSkillName).orElse(null);
        activityRecorder.skillSelected(activityRequestId, activeSkillName);

        // Skill dispatch (short-circuit)
        SkillExecutionResult skillExecution = skillExecutionDispatcher.dispatch(
                activeSkill, userMessage, session);
        if (skillExecution.status() != SkillExecutionResult.Status.NOT_HANDLED) {
            session = skillExecution.session() == null ? session : skillExecution.session();
            context.setSkillSession(session);
            skillSessionStore.save(userId, session);
            if (skillExecution.status() == SkillExecutionResult.Status.HANDLED_SILENT) {
                activityRecorder.requestCompleted(
                        activityRequestId, System.currentTimeMillis() - requestStartedAt);
                return SILENT_REPLY;
            }
            String reply = skillExecution.message();
            if (reply == null || reply.isBlank()) {
                reply = "当前功能暂时不可用，请稍后重试。";
            }
            contextStore.append("assistant", reply);
            if (skillExecution.status() == SkillExecutionResult.Status.FAILED) {
                activityRecorder.requestFailed(
                        activityRequestId, reply, System.currentTimeMillis() - requestStartedAt);
            } else {
                activityRecorder.requestCompleted(
                        activityRequestId, System.currentTimeMillis() - requestStartedAt);
            }
            return reply;
        }

        // Build effective tool set
        Set<String> effectiveTools = new LinkedHashSet<>();
        if (skillsProperties.getGlobalTools() != null) {
            effectiveTools.addAll(skillsProperties.getGlobalTools());
        }
        if (activeSkill != null) {
            effectiveTools.addAll(activeSkill.allowedTools());
        }

        // 定时任务自动执行：必须在 globalTools + skill.allowedTools 合并之后过滤，
        // 移除任务管理类工具，防止 Agent 在自动执行已有任务时自我复制/修改任务。
        filterTaskManagementTools(effectiveTools, context.isScheduledTaskExecution());

        // Build dynamic system prompt
        String systemPrompt = buildSystemPrompt(context, activeSkill);

        // Load PlanState
        PlanState planState = context.getPlanState() != null
                ? context.getPlanState()
                : planStore.get();
        context.setPlanState(planState);

        // History + current message
        List<Message> history = contextStore.getHistory(MAX_HISTORY);
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

        // Fast path: simple chat without tools
        if (!continuationRequest && (activeSkill == null || "common".equals(activeSkill.name()))) {
            if (isSimpleChat(userMessage)) {
                log.debug("快速通道：用户消息不需工具，走纯对话");
                LLMResponse quickResponse = llmClient.chatWithTools(systemPrompt, messages, List.of());
                if (quickResponse != null && !quickResponse.isToolCall()
                        && quickResponse.getContent() != null
                        && !quickResponse.getContent().isBlank()) {
                    String reply = quickResponse.getContent();
                    log.info("快速对话回复 | reply={}", reply);
                    contextStore.append("assistant", reply);
                    longTermMemoryService.processAndStoreAsync(userMessage, reply);
                    skillSessionStore.save(userId, session);
                    activityRecorder.requestCompleted(
                            activityRequestId, System.currentTimeMillis() - requestStartedAt);
                    return reply;
                }
                log.warn("快速对话路径异常，回退到工具循环");
            }
        }

        // Tool availability filtering
        ToolExecutionContext execContext = new ToolExecutionContext(userMessage, session, userId);
        List<ToolDefinition> tools = functionRegistry.getAvailableDefinitions(effectiveTools, execContext);
        log.info("[Agent Available Tools] skill={} | count={} | tools={}",
                activeSkillName, tools.size(),
                tools.stream().map(ToolDefinition::name).toList());

        // Execution loop
        ExecutionLoop.Result loopResult = executionLoop.run(
                systemPrompt, messages, tools, planState,
                execContext, session, activityRequestId, activeSkillName, userMessage);
        session = loopResult.session();
        planState = loopResult.planState();

        // Handle loop result
        return handleLoopResult(loopResult, userMessage, session, userId,
                systemPrompt, activityRequestId, requestStartedAt);
    }

    // ==================== 循环结果处理 ====================

    private String handleLoopResult(
            ExecutionLoop.Result result,
            String userMessage,
            SkillSession session,
            String userId,
            String systemPrompt,
            String activityRequestId,
            long requestStartedAt) {
        switch (result.status()) {
            case SILENT:
                skillSessionStore.save(userId, session);
                activityRecorder.requestCompleted(
                        activityRequestId, System.currentTimeMillis() - requestStartedAt);
                return SILENT_REPLY;

            case TEXT_REPLY:
                String reply = result.reply();
                contextStore.append("assistant", reply);
                longTermMemoryService.processAndStoreAsync(userMessage, reply);
                skillSessionStore.save(userId, session);
                activityRecorder.requestCompleted(
                        activityRequestId, System.currentTimeMillis() - requestStartedAt);
                return reply;

            case LLM_FAILED:
                activityRecorder.requestFailed(
                        activityRequestId, "LLM 返回空", System.currentTimeMillis() - requestStartedAt);
                return handleError();

            case MAX_ROUNDS:
                String synthesizedReply = executionLoop.synthesize(systemPrompt, result.messages());
                contextStore.append("assistant", synthesizedReply);
                longTermMemoryService.processAndStoreAsync(userMessage, synthesizedReply);
                skillSessionStore.save(userId, session);
                activityRecorder.requestCompleted(
                        activityRequestId, System.currentTimeMillis() - requestStartedAt);
                return synthesizedReply;
        }
        throw new IllegalStateException("Unknown loop status: " + result.status());
    }

    // ==================== 错误与兜底 ====================

    private String handleError() {
        contextStore.append("assistant", ERROR_REPLY);
        return ERROR_REPLY;
    }

    // ==================== 消息方法 ====================

    private boolean historyContainsCurrentMessage(List<Message> history, String userMessage) {
        if (history.isEmpty() || userMessage == null) return false;
        Message last = history.get(history.size() - 1);
        if (!"user".equals(last.role()) || last.content() == null) return false;
        return last.content().equals(userMessage) || last.content().equals("[语音]" + userMessage);
    }

    private boolean isContinuationRequest(String userMessage) {
        if (userMessage == null) return false;
        String normalized = userMessage.replaceAll("[\\s，。！!？?]", "");
        return Set.of("继续生成", "继续", "接着生成", "继续完成方案").contains(normalized);
    }

    private boolean isLegacyLimitReply(Message message) {
        if (message == null || !"assistant".equals(message.role()) || message.content() == null) {
            return false;
        }
        return message.content().contains("本轮处理步骤已达到上限")
                || message.content().contains("请回复\"继续生成\"")
                || message.content().contains("请回复“继续生成”");
    }

    /**
     * 快速判断用户消息是否需要调用工具。
     */
    private boolean isSimpleChat(String userMessage) {
        if (userMessage == null || userMessage.trim().length() <= 3) return false;

        String prompt = "你是一个分类器。判断用户消息是否需要调用工具才能完整回答。\n"
                + "需要工具：查天气、查地图/地点/路线、查时间/日期/节假日、搜索网页、"
                + "生成图片、生成文件/文档、语音合成、交通推荐、预算计算、"
                + "查课表/今天课表/导入课表/课程信息/考试安排、"
                + "设置提醒/定时提醒/自定义提醒/创建提醒。\n"
                + "不需要工具：纯粹的聊天、问答、解释、翻译、写作、闲聊、感谢。\n"
                + "如果用户消息很短（如\"好\"\"可以\"\"继续\"），可能是在回应之前提出的方案，"
                + "需要让工具系统处理，返回 NEED_TOOLS。\n"
                + "不确定时返回 NEED_TOOLS。\n"
                + "只返回一个词：NEED_TOOLS 或 CHAT_ONLY。";

        String result = llmClient.chatWithSystemPrompt(prompt, userMessage);
        return "CHAT_ONLY".equals(result != null ? result.trim() : "");
    }

    // ==================== Skill 支持方法 ====================

    /**
     * 根据 SkillRouter 的 routing 结果更新 SkillSession。
     * <ul>
     *   <li>ACTIVATE/SWITCH → 切换 activeSkill</li>
     *   <li>CONTINUE → 高置信度重置不活跃计数，低置信度递增</li>
     *   <li>DEACTIVATE → 创建新 session（回退 common）</li>
     *   <li>NONE → 非 common 时递增不活跃计数</li>
     * </ul>
     */
    private SkillSession updateSession(String userId, SkillRoutingResult routing) {
        java.util.Optional<SkillSession> existing = skillSessionStore.find(userId);
        SkillSession session = existing.orElseGet(() -> SkillSession.create(userId));

        switch (routing.action()) {
            case ACTIVATE, SWITCH -> session = session.withActiveSkill(routing.primarySkill());
            case CONTINUE -> {
                if (routing.confidence() >= 0.3) {
                    session = session.withResetInactivity();
                } else {
                    session = session.withIncrementInactivity();
                }
            }
            case DEACTIVATE -> { session = SkillSession.create(userId); }
            case NONE -> {
                if (!"common".equals(session.activeSkill())) {
                    session = session.withIncrementInactivity();
                }
            }
        }
        skillSessionStore.save(userId, session);
        return session;
    }

    /**
     * 构建动态 system prompt：基础 prompt + Active Skill 上下文。
     * <p>Release 2 还会在此追加知识库上下文。
     */
    private String buildSystemPrompt(AgentContext context, SkillDefinition activeSkill) {
        StringBuilder sb = new StringBuilder();
        sb.append(llmClient.getSystemPrompt()).append("\n\n");

        if (activeSkill != null && activeSkill.systemPromptResource() != null) {
            String skillPrompt = loadSkillPrompt(activeSkill.systemPromptResource());
            if (skillPrompt != null) {
                sb.append("--- 当前上下文 ---\n\n");
                sb.append("[SKILL_CONTEXT]\n");
                sb.append(skillPrompt).append("\n");
                sb.append("[/SKILL_CONTEXT]\n\n");
            }
        }
        // RAG knowledge context
        if (skillKnowledgeService != null && activeSkill != null
                && activeSkill.knowledge() != null && activeSkill.knowledge().enabled()) {
            try {
                String knowledge = skillKnowledgeService.recall(context.getMessage(), activeSkill.name());
                if (knowledge != null && !knowledge.isEmpty()) {
                    sb.append(knowledge).append("\n\n");
                }
            } catch (Exception e) {
                log.warn("Failed to recall skill knowledge for: {}", activeSkill.name(), e);
            }
        }

        // 定时任务自动执行：明确告知 Agent 本次是自动执行已存在的任务，不要碰定时任务本身
        if (context.isScheduledTaskExecution()) {
            sb.append("\n\n当前正在自动执行已存在的定时任务，请直接完成任务内容，不要创建、修改、取消任何定时任务。");
        }

        return sb.toString();
    }

    /**
     * 从 classpath 加载 Skill 的 system prompt 资源文件。
     */
    private String loadSkillPrompt(String resourcePath) {
        try {
            return new String(
                new org.springframework.core.io.ClassPathResource(resourcePath)
                    .getInputStream().readAllBytes(),
                java.nio.charset.StandardCharsets.UTF_8
            );
        } catch (Exception e) {
            log.warn("Failed to load skill prompt: {}", resourcePath, e);
            return null;
        }
    }
}
