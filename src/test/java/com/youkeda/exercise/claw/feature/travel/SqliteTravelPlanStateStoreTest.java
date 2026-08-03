package com.youkeda.exercise.claw.feature.travel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.memory.StorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class SqliteTravelPlanStateStoreTest {

    @Test
    void propagatesSaveFailureSoNewPlanMarkerIsNotConsumed() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        doThrow(new IllegalStateException("disk full"))
                .when(jdbc).update(anyString(), any(Object[].class));
        SqliteTravelPlanStateStore store = new SqliteTravelPlanStateStore(
                jdbc, new ObjectMapper(), new StorageProperties());

        assertThrows(IllegalStateException.class,
                () -> store.save("user-a", new TravelPlanDraft()));
    }
}
