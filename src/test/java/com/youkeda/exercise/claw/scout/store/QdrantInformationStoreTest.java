package com.youkeda.exercise.claw.scout.store;

import com.youkeda.exercise.claw.agent.memory.longterm.QdrantProperties;
import com.youkeda.exercise.claw.scout.ScoutProperties;
import com.youkeda.exercise.claw.scout.processor.InformationItem;
import io.qdrant.client.grpc.Points.PointId;
import io.qdrant.client.grpc.Points.RetrievedPoint;
import io.qdrant.client.grpc.Points.VectorOutput;
import io.qdrant.client.grpc.Points.VectorsOutput;
import org.junit.jupiter.api.Test;

import static io.qdrant.client.ValueFactory.value;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class QdrantInformationStoreTest {

    @Test
    void restoresVectorFromScrolledPoint() {
        QdrantInformationStore store = new QdrantInformationStore(
                new ScoutProperties(), new QdrantProperties());
        RetrievedPoint point = RetrievedPoint.newBuilder()
                .setId(PointId.newBuilder()
                        .setUuid("d94e02f4-90f0-4e30-b096-12cb54f94fd1"))
                .putPayload("userId", value("owner"))
                .putPayload("title", value("标题"))
                .putPayload("content", value("内容"))
                .putPayload("source", value("https://example.com"))
                .putPayload("sourceType", value("WEB_SEARCH"))
                .putPayload("category", value("NEWS"))
                .putPayload("publishedAt", value(1L))
                .putPayload("collectedAt", value(2L))
                .putPayload("summary", value("摘要"))
                .setVectors(VectorsOutput.newBuilder()
                        .setVector(VectorOutput.newBuilder()
                                .addData(0.1f)
                                .addData(0.2f)))
                .build();

        InformationItem item = store.retrievedPointToItem(point);

        assertEquals("owner", item.getUserId());
        assertArrayEquals(new float[]{0.1f, 0.2f}, item.getVector());
    }
}
