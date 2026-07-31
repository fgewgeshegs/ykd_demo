package com.youkeda.exercise.claw.skill;
import com.youkeda.exercise.claw.agent.skill.SkillSession;
import com.youkeda.exercise.claw.agent.skill.SkillPendingCoordinator;
import com.youkeda.exercise.claw.feature.scout.skill.InformationScoutIntent;
import com.youkeda.exercise.claw.feature.scout.skill.InformationScoutIntentResolver;
import com.youkeda.exercise.claw.feature.scout.ScoutSubmissionResult;
import com.youkeda.exercise.claw.agent.runtime.SkillExecutor;
import com.youkeda.exercise.claw.feature.scout.ScoutSubmissionService;
import org.springframework.stereotype.Component;

@Component
public class InformationScoutSkillExecutor implements SkillExecutor {

    public static final String NAME = "informationScoutSkillExecutor";

    private final InformationScoutIntentResolver intentResolver;
    private final ScoutSubmissionService submissionService;

    public InformationScoutSkillExecutor(InformationScoutIntentResolver intentResolver,
                                         ScoutSubmissionService submissionService) {
        this.intentResolver = intentResolver;
        this.submissionService = submissionService;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public SkillExecutionResult execute(SkillExecutionRequest request) {
        SkillSession session = request.session();
        InformationScoutIntent intent = intentResolver.resolve(
                request.currentMessage(), session);

        return switch (intent.action()) {
            case PROFILE_DISCOVERY -> submit("", request.workflowName(), session);
            case TOPIC_SEARCH -> submit(intent.query(), request.workflowName(), session);
            case NEED_CLARIFICATION -> SkillExecutionResult.reply(
                    intent.clarification(),
                    session.withPendingAction(
                            SkillPendingCoordinator.START_INFORMATION_SCOUT, "query"));
            case CANCEL -> SkillExecutionResult.reply("好的，已取消。", session.clearPendingAction());
            case NO_ACTION -> SkillExecutionResult.notHandled(session);
        };
    }

    private SkillExecutionResult submit(String query, String workflowName,
                                        SkillSession session) {
        if (workflowName == null || workflowName.isBlank()) {
            return SkillExecutionResult.failed(
                    "信息猎手暂时无法启动，请稍后重试。", session.clearPendingAction());
        }
        ScoutSubmissionResult result = submissionService.submit(query, workflowName);
        return switch (result.status()) {
            case STARTED, DUPLICATE ->
                    SkillExecutionResult.reply(
                            "已创建关注任务，发现重要内容会通知你。", session.clearPendingAction());
            case UNAVAILABLE, FAILED -> SkillExecutionResult.failed(
                    "信息猎手暂时无法启动，请稍后重试。", session.clearPendingAction());
        };
    }
}
