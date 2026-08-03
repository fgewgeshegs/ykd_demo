package com.youkeda.exercise.claw.feature.travel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.user.WechatUserManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class TravelPlanServiceNewPlanTest {

    @Test
    void replacesOldDraftOnlyWhenNewCollectRequestIsHandled() {
        ObjectMapper objectMapper = new ObjectMapper();
        TravelPlanStateStore store = new DefaultTravelPlanStateStore();
        TravelPlanService service = new TravelPlanService(
                store, objectMapper, mock(WechatUserManager.class));
        ObjectNode oldPlan = objectMapper.createObjectNode();
        oldPlan.put("departure_city", "杭州");
        oldPlan.put("destination", "新疆");
        oldPlan.put("travel_date", "8月5日");
        oldPlan.put("duration", "7天6晚");
        oldPlan.put("participant_count", 1);
        oldPlan.put("budget_per_person", 10000);
        service.handle(oldPlan, "user-a");

        ObjectNode newPlan = objectMapper.createObjectNode();
        newPlan.put("departure_city", "杭州");
        newPlan.put("destination", "新疆");
        newPlan.put("travel_date", "8月5日");
        newPlan.put("duration", "3天2晚");
        service.startNewPlan(newPlan, "user-a");

        TravelPlanDraft draft = store.get("user-a");
        assertEquals("3天2晚", draft.getDuration());
        assertNull(draft.getParticipantCount());
        assertNull(draft.getBudgetPerPerson());
    }

    @Test
    void retriesSameNewPlanRequestByMergingIntoAlreadyReplacedDraft() {
        ObjectMapper objectMapper = new ObjectMapper();
        TravelPlanStateStore store = new DefaultTravelPlanStateStore();
        TravelPlanService service = new TravelPlanService(
                store, objectMapper, mock(WechatUserManager.class));
        ObjectNode firstAttempt = objectMapper.createObjectNode();
        firstAttempt.put("destination", "新疆");
        firstAttempt.put("duration", "3天2晚");

        service.startNewPlan(firstAttempt, "user-a", "request-1");
        service.startNewPlan(
                objectMapper.createObjectNode().put("participant_count", 2),
                "user-a", "request-1");

        TravelPlanDraft draft = store.get("user-a");
        assertEquals("新疆", draft.getDestination());
        assertEquals("3天2晚", draft.getDuration());
        assertEquals(2, draft.getParticipantCount());
    }

    @Test
    void failedRetryDoesNotMutatePersistedDraftBeforeSave() {
        ObjectMapper objectMapper = new ObjectMapper();
        TravelPlanStateStore store = new DefaultTravelPlanStateStore();
        TravelPlanService service = new TravelPlanService(
                store, objectMapper, mock(WechatUserManager.class));
        service.startNewPlan(
                objectMapper.createObjectNode()
                        .put("departure_city", "杭州")
                        .put("destination", "新疆")
                        .put("duration", "3天2晚"),
                "user-a", "request-1");

        ObjectNode failedRetry = objectMapper.createObjectNode()
                .put("departure_city", "上海")
                .put("duration", "999999999999999999999天");

        assertThrows(NumberFormatException.class,
                () -> service.startNewPlan(failedRetry, "user-a", "request-1"));
        assertEquals("杭州", store.get("user-a").getDepartureCity());
        assertEquals("3天2晚", store.get("user-a").getDuration());
    }

    @Test
    void defaultStoreKeepsUsersIsolated() {
        ObjectMapper objectMapper = new ObjectMapper();
        TravelPlanStateStore store = new DefaultTravelPlanStateStore();
        TravelPlanService service = new TravelPlanService(
                store, objectMapper, mock(WechatUserManager.class));

        service.handle(objectMapper.createObjectNode().put("departure_city", "杭州"), "user-a");
        service.handle(objectMapper.createObjectNode().put("destination", "北京"), "user-b");

        assertEquals("杭州", store.get("user-a").getDepartureCity());
        assertNull(store.get("user-a").getDestination());
        assertEquals("北京", store.get("user-b").getDestination());
        assertNull(store.get("user-b").getDepartureCity());
    }
}
