package com.youkeda.exercise.claw.agent.memory.longterm;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryWriteCoordinatorTest {

    @Test
    void sameTopicNeverExecutesReadWriteSectionConcurrently() throws Exception {
        MemoryWriteCoordinator coordinator = new MemoryWriteCoordinator();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < 20; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    coordinator.withTopicLock("u1", "diet.spicy", () -> {
                        int current = active.incrementAndGet();
                        maxActive.accumulateAndGet(current, Math::max);
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            active.decrementAndGet();
                        }
                        return null;
                    });
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, maxActive.get());
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
}
