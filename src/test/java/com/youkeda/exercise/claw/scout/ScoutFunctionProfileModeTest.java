package com.youkeda.exercise.claw.scout;
import com.youkeda.exercise.claw.tool.scout.ScoutTool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.skill.SkillsProperties;
import com.youkeda.exercise.claw.agent.runtime.ToolExecutionContext;
import com.youkeda.exercise.claw.agent.runtime.ToolRegistry;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.*;

class ScoutFunctionProfileModeTest {

    @Test
    void startsTaskWithoutUserIdentity() {
        ScoutSubmissionService submissionService = mock(ScoutSubmissionService.class);
        when(submissionService.submit("", "customScoutWorkflow"))
                .thenReturn(ScoutSubmissionResult.started("task-1"));
        SkillsProperties properties = new SkillsProperties();
        properties.setSkillWorkflowBindings(Map.of(
                "information-scout", "customScoutWorkflow"));
        ScoutTool function = new ScoutTool(
                mock(ToolRegistry.class), new ObjectMapper(), submissionService, properties);

        function.execute("{}", new ToolExecutionContext("最近有什么值得关注的事情吗"));

        verify(submissionService).submit("", "customScoutWorkflow");
    }

    @Test
    void omittedQueryStartsProfileDiscoveryInsteadOfUsingBroadRequestAsTopic() {
        ScoutSubmissionService submissionService = mock(ScoutSubmissionService.class);
        when(submissionService.submit("", "scoutWorkflow"))
                .thenReturn(ScoutSubmissionResult.started("task-1"));
        SkillsProperties properties = new SkillsProperties();
        properties.setSkillWorkflowBindings(Map.of(
                "information-scout", "scoutWorkflow"));
        ScoutTool function = new ScoutTool(
                mock(ToolRegistry.class), new ObjectMapper(), submissionService, properties);

        function.execute("{}", new ToolExecutionContext("最近有什么值得关注的事情吗"));

        verify(submissionService).submit("", "scoutWorkflow");
    }
}
