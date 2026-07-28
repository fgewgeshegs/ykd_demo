package com.youkeda.exercise.claw.scout.notifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoutDeliveryStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void deliveryRecordSurvivesRestart() {
        String dbPath = tempDir.resolve("assistant.db").toString();
        long now = System.currentTimeMillis();

        ScoutDeliveryStore first = new ScoutDeliveryStore(dbPath);
        first.init();
        first.markDelivered("owner", "item-1", now);

        ScoutDeliveryStore restarted = new ScoutDeliveryStore(dbPath);
        restarted.init();

        assertTrue(restarted.wasDeliveredSince("owner", "item-1", now - 1));
        assertFalse(restarted.wasDeliveredSince("owner", "item-1", now + 1));
    }
}
