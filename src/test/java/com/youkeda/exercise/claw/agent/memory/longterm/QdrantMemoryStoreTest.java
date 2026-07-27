package com.youkeda.exercise.claw.agent.memory.longterm;

import io.qdrant.client.grpc.Points.Condition;
import io.qdrant.client.grpc.Points.Filter;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class QdrantMemoryStoreTest {

    @Test
    void deleteFilterRequiresBothUserAndMemoryId() throws Exception {
        QdrantMemoryStore store = new QdrantMemoryStore(new QdrantProperties());
        Method method = QdrantMemoryStore.class.getDeclaredMethod(
                "buildUserAndIdFilter", String.class, String.class);
        method.setAccessible(true);

        Filter filter = (Filter) method.invoke(
                store, "user-a", "f06769ff-8c72-4b54-bfba-946ca4ce9b9c");

        assertEquals(2, filter.getMustCount());
        Condition userCondition = filter.getMust(0);
        Condition idCondition = filter.getMust(1);
        assertEquals("userId", userCondition.getField().getKey());
        assertEquals("user-a", userCondition.getField().getMatch().getKeyword());
        assertEquals("f06769ff-8c72-4b54-bfba-946ca4ce9b9c",
                idCondition.getHasId().getHasId(0).getUuid());
    }
}
