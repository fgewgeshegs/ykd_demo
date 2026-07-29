package com.youkeda.exercise.claw.scout.processor;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InformationFreshnessTest {

    @Test
    void parsesSupportedPublicationDates() {
        assertTrue(InformationFreshness.parsePublishedAt("2026-07-28") > 0);
        assertTrue(InformationFreshness.parsePublishedAt("2026-07-28T08:00:00Z") > 0);
        assertTrue(InformationFreshness.parsePublishedAt("Tue, 28 Jul 2026 08:00:00 GMT") > 0);
    }

    @Test
    void rejectsUnknownOldAndFarFutureDates() {
        Instant now = Instant.parse("2026-07-28T12:00:00Z");
        long recent = LocalDate.parse("2026-07-25")
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        long old = LocalDate.parse("2025-03-01")
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        long future = LocalDate.parse("2026-08-10")
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        assertTrue(InformationFreshness.isFresh(recent, 14, now));
        assertFalse(InformationFreshness.isFresh(0, 14, now));
        assertFalse(InformationFreshness.isFresh(old, 14, now));
        assertFalse(InformationFreshness.isFresh(future, 14, now));
    }
}
