package com.youkeda.exercise.claw.feature.travel;

import com.youkeda.exercise.claw.agent.skill.SkillLifecycleHandler;
import com.youkeda.exercise.claw.agent.skill.SkillPendingCoordinator;
import com.youkeda.exercise.claw.agent.skill.SkillRoutingResult;
import com.youkeda.exercise.claw.agent.skill.SkillSession;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Pattern;

/** 标记新旅游方案；真正替换草稿由 travel_collect 成功执行时原子完成。 */
@Component
public class TravelSkillLifecycleHandler implements SkillLifecycleHandler {

    private static final Pattern RESUME = Pattern.compile(
            "(?:继续|接着).{0,8}(?:刚才|上次|之前|原来|旧)"
                    + "|恢复.{0,8}(?:上次|之前|原来|旧)"
                    + "|(?:刚才|上次|之前|原来|旧).{0,8}(?:旅行|行程|方案)");
    private static final Pattern REVISION = Pattern.compile("修改|调整|改成|改为|优化|细化");
    private static final Pattern EXPLICIT_NEW = Pattern.compile("重新|再做|再规划|另一个|另一趟|新行程|换个地方|换目的地");
    private static final Pattern FULL_TRIP = Pattern.compile(
            "(?:规划|安排|制定|设计).{0,20}(?:[零一二三四五六七八九十百千万两\\d]+[日天]游|旅游|旅行|行程)"
                    + "|(?:去|到).{1,16}(?:玩|逛|旅游|旅行)(?:[零一二三四五六七八九十百千万两\\d]+)[日天]");

    @Override
    public String getSkillName() {
        return "travel";
    }

    @Override
    public SkillSession onRouting(
            String userMessage,
            SkillRoutingResult routing,
            SkillSession session) {
        return onRouting(userMessage, routing, session, null);
    }

    @Override
    public SkillSession onRouting(
            String userMessage,
            SkillRoutingResult routing,
            SkillSession session,
            String requestId) {
        if (userMessage != null && RESUME.matcher(userMessage).find()) {
            SkillSession resumed = session.withoutContextValue(
                    SkillPendingCoordinator.NEW_TRAVEL_PLAN);
            if (resumed.hasPendingAction(
                    SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS)
                    && "initial_request".equals(resumed.pendingSlot())) {
                return resumed.clearPendingAction();
            }
            return resumed;
        }
        if (!isNewPlanRequest(userMessage, routing)) return session;
        return session
                .withContextValue(
                        SkillPendingCoordinator.NEW_TRAVEL_PLAN,
                        requestId == null || requestId.isBlank()
                                ? UUID.randomUUID().toString()
                                : requestId)
                .withPendingAction(
                        SkillPendingCoordinator.COLLECT_TRAVEL_REQUIREMENTS,
                        "initial_request");
    }

    private boolean isNewPlanRequest(String message, SkillRoutingResult routing) {
        if (message == null || message.isBlank()) return false;
        if (REVISION.matcher(message).find()) return false;
        if (EXPLICIT_NEW.matcher(message).find()) return true;
        if (!FULL_TRIP.matcher(message).find()) return false;
        return routing.action() == SkillRoutingResult.SkillRoutingAction.ACTIVATE
                || routing.action() == SkillRoutingResult.SkillRoutingAction.SWITCH
                || routing.action() == SkillRoutingResult.SkillRoutingAction.CONTINUE;
    }
}
