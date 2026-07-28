package com.youkeda.exercise.claw.scout;

import com.youkeda.exercise.claw.scout.collector.CollectorRegistry;
import com.youkeda.exercise.claw.scout.context.UserBehaviorAnalyzer;
import com.youkeda.exercise.claw.scout.context.UserContextService;
import com.youkeda.exercise.claw.scout.context.UserProfile;
import com.youkeda.exercise.claw.scout.judge.DecisionMaker;
import com.youkeda.exercise.claw.scout.matcher.CandidateMatcher;
import com.youkeda.exercise.claw.scout.notifier.NotificationService;
import com.youkeda.exercise.claw.scout.planner.SearchPlanner;
import com.youkeda.exercise.claw.scout.processor.InformationProcessor;
import com.youkeda.exercise.claw.scout.store.InformationStore;
import com.youkeda.exercise.claw.wechat.user.WechatUserManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScoutOrchestratorTest {

    @TempDir
    Path tempDir;

    @Test
    void scheduledCollectionUsesPersistedOwner() {
        WechatUserManager userManager = new WechatUserManager(
                tempDir.resolve("assistant.db").toString());
        userManager.init();
        userManager.recordInteraction("owner");

        UserContextService contextService = mock(UserContextService.class);
        SearchPlanner planner = mock(SearchPlanner.class);
        CollectorRegistry collectors = mock(CollectorRegistry.class);
        UserProfile profile = new UserProfile(
                "owner", List.of(), List.of(), List.of(), List.of(), "");
        when(contextService.buildProfile("owner")).thenReturn(profile);
        when(planner.plan(profile)).thenReturn(List.of());
        when(collectors.collectAll(List.of(), "owner")).thenReturn(List.of());

        ScoutProperties props = new ScoutProperties();
        ScoutOrchestrator orchestrator = new ScoutOrchestrator(
                contextService,
                mock(UserBehaviorAnalyzer.class),
                planner,
                collectors,
                mock(InformationProcessor.class),
                mock(InformationStore.class),
                mock(CandidateMatcher.class),
                mock(DecisionMaker.class),
                mock(NotificationService.class),
                props,
                userManager);

        orchestrator.scheduledCollect();

        verify(contextService).buildProfile("owner");
    }
}
