package com.youkeda.exercise.claw.scout.store;

import static io.qdrant.client.ValueFactory.value;

import com.youkeda.exercise.claw.agent.memory.longterm.QdrantProperties;
import com.youkeda.exercise.claw.scout.ScoutProperties;
import com.youkeda.exercise.claw.scout.processor.InformationIdentity;
import com.youkeda.exercise.claw.scout.processor.InformationItem;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.JsonWithInt.Value;
import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.Points.Condition;
import io.qdrant.client.grpc.Points.FieldCondition;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.Match;
import io.qdrant.client.grpc.Points.PointId;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.RetrievedPoint;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.ScrollPoints;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.Vector;
import io.qdrant.client.grpc.Points.Vectors;
import io.qdrant.client.grpc.Points.WithPayloadSelector;
import io.qdrant.client.grpc.Points.WithVectorsSelector;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Qdrant 信息存储实现
 *
 * 独立 collection（与长期记忆分开），复用同一 Qdrant 实例
 */
@Component
@ConditionalOnProperty(name = "scout.enabled", havingValue = "true")
public class QdrantInformationStore implements InformationStore {

    private static final Logger log = LoggerFactory.getLogger(QdrantInformationStore.class);

    private final ScoutProperties scoutProps;
    private final QdrantProperties qdrantProps;
    private QdrantClient client;
    private String collectionName;

    public QdrantInformationStore(ScoutProperties scoutProps, QdrantProperties qdrantProps) {
        this.scoutProps = scoutProps;
        this.qdrantProps = qdrantProps;
    }

    @PostConstruct
    public void init() {
        try {
            client = new QdrantClient(
                    QdrantGrpcClient.newBuilder(qdrantProps.getHost(), qdrantProps.getPort(), false)
                            .build());
            collectionName = scoutProps.getQdrant().getCollection();
            ensureCollection();
            log.info("Scout 信息库连接成功 | collection={}", collectionName);
        } catch (Exception e) {
            log.error("Scout 信息库连接失败", e);
            client = null;
        }
    }

    @PreDestroy
    public void shutdown() {
        if (client != null) {
            client.close();
        }
    }

    private void ensureCollection() {
        try {
            Boolean exists = client.collectionExistsAsync(collectionName).get();
            if (Boolean.TRUE.equals(exists)) return;
        } catch (Exception e) {
            log.warn("检查 Scout Collection 失败，尝试创建", e);
        }

        try {
            client.createCollectionAsync(collectionName,
                    VectorParams.newBuilder()
                            .setSize(scoutProps.getQdrant().getVectorDimension())
                            .setDistance(Distance.Cosine)
                            .build()
            ).get();
            log.info("Scout Collection 创建成功 | name={}", collectionName);
        } catch (Exception e) {
            log.error("Scout Collection 创建失败 | name={}", collectionName, e);
        }
    }

    @Override
    public void batchSave(List<InformationItem> items) {
        if (client == null || items.isEmpty()) return;

        List<PointStruct> points = new ArrayList<>();
        for (InformationItem item : items) {
            if (item.getVector() == null) continue;
            String pointId = InformationIdentity.pointUuid(item);
            item.setId(pointId);

            PointStruct point = PointStruct.newBuilder()
                    .setId(PointId.newBuilder().setUuid(pointId).build())
                    .setVectors(Vectors.newBuilder()
                            .setVector(Vector.newBuilder()
                                    .addAllData(toFloatList(item.getVector()))
                                    .build())
                            .build())
                    .putAllPayload(buildPayload(item))
                    .build();
            points.add(point);
        }

        if (!points.isEmpty()) {
            try {
                client.upsertAsync(collectionName, points).get();
                log.debug("Scout 信息批量写入成功 | count={}", points.size());
            } catch (Exception e) {
                log.error("Scout 信息批量写入失败", e);
            }
        }
    }

    @Override
    public List<InformationItem> searchByVector(String userId, float[] vector, int topK) {
        if (client == null) return List.of();
        try {
            Filter filter = buildUserFilter(userId);
            SearchPoints request = SearchPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .addAllVector(toFloatList(vector))
                    .setLimit(topK)
                    .setWithPayload(WithPayloadSelector.newBuilder().setEnable(true).build())
                    .setFilter(filter)
                    .build();

            List<ScoredPoint> results = client.searchAsync(request).get();
            List<InformationItem> items = new ArrayList<>();
            for (ScoredPoint sp : results) {
                InformationItem item = payloadToItem(sp.getId().getUuid(), sp.getPayloadMap());
                if (item != null) items.add(item);
            }
            return items;
        } catch (Exception e) {
            log.error("Scout 向量检索失败 | userId={}", userId, e);
            return List.of();
        }
    }

    @Override
    public List<InformationItem> getRecent(String userId, int limit) {
        if (client == null) return List.of();
        try {
            ScrollPoints request = ScrollPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .setFilter(buildUserFilter(userId))
                    .setWithPayload(WithPayloadSelector.newBuilder().setEnable(true).build())
                    .setWithVectors(WithVectorsSelector.newBuilder().setEnable(true).build())
                    .setLimit(limit)
                    .build();

            List<InformationItem> items = new ArrayList<>();
            for (RetrievedPoint rp : client.scrollAsync(request).get().getResultList()) {
                InformationItem item = retrievedPointToItem(rp);
                if (item != null) items.add(item);
            }
            items.sort(java.util.Comparator.comparingLong(InformationItem::getCollectedAt).reversed());
            return items;
        } catch (Exception e) {
            log.error("Scout 查询最近信息失败 | userId={}", userId, e);
            return List.of();
        }
    }

    @Override
    public void deleteExpired(String userId, long beforeTimestamp) {
        if (client == null) return;
        try {
            Filter filter = Filter.newBuilder()
                    .addMust(Condition.newBuilder()
                            .setField(FieldCondition.newBuilder()
                                    .setKey("userId")
                                    .setMatch(Match.newBuilder().setKeyword(userId).build())
                                    .build())
                            .build())
                    .addMust(Condition.newBuilder()
                            .setField(FieldCondition.newBuilder()
                                    .setKey("collectedAt")
                                    .setRange(Points.Range.newBuilder()
                                            .setLt(beforeTimestamp)
                                            .build())
                                    .build())
                            .build())
                    .build();

            client.deleteAsync(collectionName, filter).get();
            log.debug("Scout 过期信息已清理 | userId={}", userId);
        } catch (Exception e) {
            log.error("Scout 过期信息清理失败 | userId={}", userId, e);
        }
    }

    // ==================== 内部方法 ====================

    private Filter buildUserFilter(String userId) {
        return Filter.newBuilder()
                .addMust(Condition.newBuilder()
                        .setField(FieldCondition.newBuilder()
                                .setKey("userId")
                                .setMatch(Match.newBuilder().setKeyword(userId).build())
                                .build())
                        .build())
                .build();
    }

    private Map<String, Value> buildPayload(InformationItem item) {
        Map<String, Value> payload = new HashMap<>();
        payload.put("userId", value(item.getUserId()));
        payload.put("title", value(item.getTitle()));
        payload.put("content", value(item.getContent() != null ? item.getContent() : ""));
        payload.put("source", value(item.getSource() != null ? item.getSource() : ""));
        payload.put("sourceType", value(item.getSourceType()));
        payload.put("category", value(item.getCategory()));
        payload.put("publishedAt", value(item.getPublishedAt()));
        payload.put("collectedAt", value(item.getCollectedAt()));
        payload.put("summary", value(item.getSummary() != null ? item.getSummary() : ""));
        return payload;
    }

    private InformationItem payloadToItem(String pointId, Map<String, Value> payload) {
        try {
            InformationItem item = new InformationItem();
            item.setId(pointId);
            item.setUserId(getString(payload, "userId"));
            item.setTitle(getString(payload, "title"));
            item.setContent(getString(payload, "content"));
            item.setSource(getString(payload, "source"));
            item.setSourceType(getString(payload, "sourceType"));
            item.setCategory(getString(payload, "category"));
            item.setPublishedAt(getLong(payload, "publishedAt"));
            item.setCollectedAt(getLong(payload, "collectedAt"));
            item.setSummary(getString(payload, "summary"));
            return item;
        } catch (Exception e) {
            log.warn("Scout payload 解析失败 | pointId={}", pointId, e);
            return null;
        }
    }

    InformationItem retrievedPointToItem(RetrievedPoint point) {
        InformationItem item = payloadToItem(point.getId().getUuid(), point.getPayloadMap());
        if (item == null || !point.hasVectors() || !point.getVectors().hasVector()) {
            return item;
        }
        List<Float> values = point.getVectors().getVector().getDataList();
        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vector[i] = values.get(i);
        }
        item.setVector(vector);
        return item;
    }

    private String getString(Map<String, Value> payload, String key) {
        Value v = payload.get(key);
        if (v == null) return "";
        try { return v.getStringValue(); } catch (Exception e) { return ""; }
    }

    private long getLong(Map<String, Value> payload, String key) {
        Value v = payload.get(key);
        if (v == null) return 0L;
        try {
            if (v.hasIntegerValue()) return v.getIntegerValue();
            if (v.hasDoubleValue()) return (long) v.getDoubleValue();
        } catch (Exception ignored) {}
        return 0L;
    }

    private List<Float> toFloatList(float[] arr) {
        List<Float> list = new ArrayList<>(arr.length);
        for (float f : arr) list.add(f);
        return list;
    }
}
