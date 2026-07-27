package com.youkeda.exercise.claw.agent.memory.longterm;

import io.qdrant.client.grpc.Points.Condition;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.JsonWithInt.Value;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;

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

    @Test
    @SuppressWarnings("unchecked")
    void payloadRoundTripPreservesMillisecondTimestamps() throws Exception {
        QdrantMemoryStore store = new QdrantMemoryStore(new QdrantProperties());
        long createdMillis = 1_785_156_089_007L;
        long updatedMillis = createdMillis + 12_345L;
        MemoryItem original = new MemoryItem(
                "f06769ff-8c72-4b54-bfba-946ca4ce9b9c", "u1",
                MemoryCategory.FACT, "profile.city", "用户住在杭州", "我住在杭州",
                0.8f, 0.9f, MemorySource.AUTO,
                Instant.ofEpochMilli(createdMillis), Instant.ofEpochMilli(updatedMillis), 0);
        Method buildPayload = QdrantMemoryStore.class.getDeclaredMethod(
                "buildPayload", MemoryItem.class);
        Method parsePayload = QdrantMemoryStore.class.getDeclaredMethod(
                "payloadToMemoryItem", String.class, Map.class);
        buildPayload.setAccessible(true);
        parsePayload.setAccessible(true);

        Map<String, Value> payload = (Map<String, Value>) buildPayload.invoke(store, original);
        MemoryItem restored = (MemoryItem) parsePayload.invoke(store, original.id(), payload);

        assertNotNull(restored);
        assertEquals(original.createdAt(), restored.createdAt());
        assertEquals(original.updatedAt(), restored.updatedAt());
    }
}
