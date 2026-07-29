package com.youkeda.exercise.claw.agent.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class SkillRouter {

    private static final Logger log = LoggerFactory.getLogger(SkillRouter.class);

    private final SkillRegistry skillRegistry;
    private final SkillSessionStore sessionStore;
    private final TriggerPolicyFactory triggerPolicyFactory;
    private final SkillLlmRouter llmRouter;
    private final RouterConfig config;

    public record RouterConfig(
            int inactiveLimit,
            int sessionTimeoutMinutes,
            boolean llmRouterEnabled
    ) {
        public static RouterConfig defaults() {
            return new RouterConfig(5, 30, true);
        }
    }

    private static final Set<String> NEGATIONS = Set.of("别", "不要", "先不说", "不谈", "不说", "不用", "换", "切到", "切换到");

    public SkillRouter(SkillRegistry skillRegistry,
                       SkillSessionStore sessionStore,
                       TriggerPolicyFactory triggerPolicyFactory,
                       SkillLlmRouter llmRouter) {
        this.skillRegistry = skillRegistry;
        this.sessionStore = sessionStore;
        this.triggerPolicyFactory = triggerPolicyFactory;
        this.llmRouter = llmRouter;
        this.config = RouterConfig.defaults();
    }

    public SkillRoutingResult route(String message, String userId) {
        if (message == null || message.isBlank()) return SkillRoutingResult.fallback();

        Optional<SkillSession> sessionOpt = sessionStore.find(userId);

        // Layer 1: Pending interaction confirmation
        SkillRoutingResult layer1 = handlePendingInteraction(message, sessionOpt);
        if (layer1 != null) return layer1;

        // Layer 2: Explicit skill switch
        SkillRoutingResult layer2 = handleExplicitSwitch(message, sessionOpt);
        if (layer2 != null) return layer2;

        // Layer 3: New trigger word match
        SkillRoutingResult layer3 = handleNewTrigger(message, sessionOpt);
        if (layer3 != null && layer3.confidence() >= 0.8) {
            return layer3;
        }

        // Layer 4: Continuation check
        SkillRoutingResult layer4 = handleContinuation(message, sessionOpt);
        if (layer4 != null) return layer4;

        // Layer 5: LLM Router (slow path)
        if (config.llmRouterEnabled()) {
            SkillRoutingResult layer5 = llmRouter.route(message, userId, skillRegistry);
            if (layer5 != null) return layer5;
        }

        return SkillRoutingResult.fallback();
    }

    private SkillRoutingResult handlePendingInteraction(String message, Optional<SkillSession> sessionOpt) {
        if (sessionOpt.isEmpty()) return null;
        SkillSession session = sessionOpt.get();
        if (!"travel".equals(session.activeSkill())) return null;

        Set<String> pendingKeywords = Set.of("确认", "选择方案", "好的", "行", "可以", "这个方案");
        boolean hasKeyword = pendingKeywords.stream().anyMatch(message::contains);

        if (hasKeyword) {
            return new SkillRoutingResult(session.activeSkill(), Set.of(),
                    SkillRoutingResult.SkillRoutingAction.CONTINUE, 0.95,
                    "pending interaction confirmation for " + session.activeSkill());
        }
        return null;
    }

    private SkillRoutingResult handleExplicitSwitch(String message, Optional<SkillSession> sessionOpt) {
        boolean hasNegation = NEGATIONS.stream().anyMatch(message::startsWith) || message.startsWith("不说");
        if (!hasNegation) return null;

        String afterNegation = message;
        for (String neg : NEGATIONS) {
            if (message.startsWith(neg)) {
                afterNegation = message.substring(neg.length()).trim();
                break;
            }
        }

        List<SkillDefinition> all = new ArrayList<>(skillRegistry.getAll());
        all.sort(Comparator.comparingInt(SkillDefinition::priority).reversed());

        for (SkillDefinition skill : all) {
            if ("common".equals(skill.name())) continue;
            SkillTriggerPolicy policy = triggerPolicyFactory.getPolicy(skill.triggerPolicyName());
            SkillTriggerMatch match = policy.match(afterNegation, Optional.empty());
            if (match.matched() && match.confidence() >= 0.6) {
                return new SkillRoutingResult(skill.name(), Set.of(),
                        SkillRoutingResult.SkillRoutingAction.SWITCH,
                        match.confidence(), "explicit switch to " + skill.name());
            }
        }
        return null;
    }

    private SkillRoutingResult handleNewTrigger(String message, Optional<SkillSession> sessionOpt) {
        List<SkillMatchResult> matches = new ArrayList<>();
        List<SkillDefinition> all = new ArrayList<>(skillRegistry.getAll());
        all.sort(Comparator.comparingInt(SkillDefinition::priority).reversed());

        for (SkillDefinition skill : all) {
            if ("common".equals(skill.name())) continue;
            SkillTriggerPolicy policy = triggerPolicyFactory.getPolicy(skill.triggerPolicyName());
            SkillTriggerMatch match = policy.match(message, sessionOpt);
            if (match.matched()) {
                matches.add(new SkillMatchResult(skill.name(), match.confidence(), skill.priority()));
            }
        }

        if (matches.isEmpty()) return null;

        if (matches.size() == 1) {
            SkillMatchResult top = matches.get(0);
            SkillRoutingResult.SkillRoutingAction action =
                    sessionOpt.map(s -> s.activeSkill().equals(top.skillName())
                            ? SkillRoutingResult.SkillRoutingAction.CONTINUE
                            : SkillRoutingResult.SkillRoutingAction.ACTIVATE)
                    .orElse(SkillRoutingResult.SkillRoutingAction.ACTIVATE);
            return new SkillRoutingResult(top.skillName(), Set.of(), action, top.confidence(),
                    "single keyword trigger: " + top.skillName());
        }

        // Multiple matches: resolve by priority
        matches.sort((a, b) -> {
            int cmp = Integer.compare(b.priority(), a.priority());
            if (cmp != 0) return cmp;
            return Double.compare(b.confidence(), a.confidence());
        });

        SkillMatchResult top = matches.get(0);

        // Conflict if same priority
        if (matches.size() > 1 && matches.get(1).priority() == top.priority()) {
            return new SkillRoutingResult(top.skillName(), Set.of(),
                    SkillRoutingResult.SkillRoutingAction.NONE,
                    0.5, "priority conflict, fall through to LLM");
        }

        SkillRoutingResult.SkillRoutingAction action =
                sessionOpt.map(s -> s.activeSkill().equals(top.skillName())
                        ? SkillRoutingResult.SkillRoutingAction.CONTINUE
                        : SkillRoutingResult.SkillRoutingAction.ACTIVATE)
                .orElse(SkillRoutingResult.SkillRoutingAction.ACTIVATE);
        return new SkillRoutingResult(top.skillName(), Set.of(), action, top.confidence(),
                "trigger match (priority resolved): " + top.skillName());
    }

    private SkillRoutingResult handleContinuation(String message, Optional<SkillSession> sessionOpt) {
        if (sessionOpt.isEmpty()) return null;
        SkillSession session = sessionOpt.get();

        long minutesSinceActivity = ChronoUnit.MINUTES.between(session.lastActivityAt(), Instant.now());
        if (minutesSinceActivity >= config.sessionTimeoutMinutes()) {
            return new SkillRoutingResult("common", Set.of(),
                    SkillRoutingResult.SkillRoutingAction.DEACTIVATE, 0.0,
                    "session timeout for " + session.activeSkill());
        }

        if (session.inactivityCount() >= config.inactiveLimit()) {
            return new SkillRoutingResult("common", Set.of(),
                    SkillRoutingResult.SkillRoutingAction.DEACTIVATE, 0.0,
                    "inactivity limit reached for " + session.activeSkill());
        }

        Set<String> resumeKeywords = Set.of("继续", "刚才", "接着", "上一步");
        if (resumeKeywords.stream().anyMatch(message::contains) && session.previousSkill() != null) {
            return new SkillRoutingResult(session.previousSkill(), Set.of(),
                    SkillRoutingResult.SkillRoutingAction.SWITCH, 0.9,
                    "resume previous skill: " + session.previousSkill());
        }

        // Default continuation check
        SkillDefinition skillDef = skillRegistry.find(session.activeSkill()).orElse(null);
        if (skillDef == null) return null;

        SkillTriggerPolicy policy = triggerPolicyFactory.getPolicy(skillDef.triggerPolicyName());
        SkillTriggerMatch match = policy.match(message, sessionOpt);

        if (match.matched()) {
            return new SkillRoutingResult(session.activeSkill(), Set.of(),
                    SkillRoutingResult.SkillRoutingAction.CONTINUE, match.confidence(),
                    "continuation of " + session.activeSkill());
        }

        return new SkillRoutingResult(session.activeSkill(), Set.of(),
                SkillRoutingResult.SkillRoutingAction.CONTINUE, 0.1,
                "possible continuation, low confidence");
    }

    private record SkillMatchResult(String skillName, double confidence, int priority) {}
}
