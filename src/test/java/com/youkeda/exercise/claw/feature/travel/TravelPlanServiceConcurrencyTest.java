package com.youkeda.exercise.claw.feature.travel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.user.WechatUserManager;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class TravelPlanServiceConcurrencyTest {

    @Test
    void serializesUpdatesForSameUser() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RacingStore store = new RacingStore(objectMapper);
        TravelPlanService service = new TravelPlanService(
                store, objectMapper, mock(WechatUserManager.class));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> service.handle(
                    objectMapper.createObjectNode().put("departure_city", "杭州"), "user-a"));
            store.firstSaveEntered.await(2, TimeUnit.SECONDS);
            Future<?> second = executor.submit(() -> service.handle(
                    objectMapper.createObjectNode().put("budget_total", 5000), "user-a"));
            store.secondGet.await(200, TimeUnit.MILLISECONDS);
            store.allowFirstSave.countDown();
            first.get(2, TimeUnit.SECONDS);
            second.get(2, TimeUnit.SECONDS);

            assertEquals("杭州", store.stored.getDepartureCity());
            assertEquals(5000D, store.stored.getBudgetTotal());
        } finally {
            executor.shutdownNow();
        }
    }

    private static final class RacingStore implements TravelPlanStateStore {
        private final ObjectMapper objectMapper;
        private final AtomicInteger gets = new AtomicInteger();
        private final AtomicInteger saves = new AtomicInteger();
        private final CountDownLatch firstSaveEntered = new CountDownLatch(1);
        private final CountDownLatch allowFirstSave = new CountDownLatch(1);
        private final CountDownLatch firstSaveDone = new CountDownLatch(1);
        private final CountDownLatch secondGet = new CountDownLatch(1);
        private volatile TravelPlanDraft stored = new TravelPlanDraft();

        private RacingStore(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public TravelPlanDraft get(String userId) {
            if (gets.incrementAndGet() == 2) secondGet.countDown();
            return copy(stored);
        }

        @Override
        public void save(String userId, TravelPlanDraft draft) {
            int saveNumber = saves.incrementAndGet();
            try {
                if (saveNumber == 1) {
                    firstSaveEntered.countDown();
                    allowFirstSave.await(2, TimeUnit.SECONDS);
                    stored = copy(draft);
                    firstSaveDone.countDown();
                } else {
                    firstSaveDone.await(2, TimeUnit.SECONDS);
                    stored = copy(draft);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void clear(String userId) {
            stored = null;
        }

        private TravelPlanDraft copy(TravelPlanDraft draft) {
            return objectMapper.convertValue(draft, TravelPlanDraft.class);
        }
    }
}