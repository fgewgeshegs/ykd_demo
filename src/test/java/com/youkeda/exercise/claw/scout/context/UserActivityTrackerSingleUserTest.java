package com.youkeda.exercise.claw.scout.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserActivityTrackerSingleUserTest {

    @Test
    void recordsActivityWithoutUserIdentity() {
        UserActivityTracker tracker = new UserActivityTracker();

        tracker.record();
        tracker.record();

        assertEquals(2, tracker.getRecordCount());
    }
}
