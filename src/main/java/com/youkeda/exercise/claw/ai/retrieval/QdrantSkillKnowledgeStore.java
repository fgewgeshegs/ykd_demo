package com.youkeda.exercise.claw.ai.retrieval;

import static io.qdrant.client.ValueFactory.value;

import io.qdrant.client.grpc.Collections;
import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.Points.Condition;
import io.qdrant.client.grpc.Points.FieldCondition;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.Match;
import io.qdrant.client.grpc.Points.PointId;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.Vector;
import io.qdrant.client.grpc.Points.Vectors;
import io.qdrant.client.grpc.Points.WithPayloadSelector;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class QdrantSkillKnowledgeStore implements SkillKnowledgeStore {

    private static final Logger log = LoggerFactory.getLogger(QdrantSkillKnowledgeStore.class);

    private final QdrantClientProvider clientProvider;

    @Value("${skill.knowledge.collection:skill_knowledge}")
    private String collectionName;

    @Value("${memory.qdrant.vector-dimension:1024}")
    private int vectorDimension;

    public QdrantSkillKnowledgeStore(QdrantClientProvider clientProvider) {
        this.clientProvider = clientProvider;
    }

    @PostConstruct
    public void init() {
        try {
            var client = clientProvider.getClient();
            boolean exists = client.collectionExistsAsync(collectionName).get();
            if (!exists) {
                client.createCollectionAsync(collectionName,
                    Collections.VectorParams.newBuilder()
                        .setDistance(Collections.Distance.Cosine)
                        .setSize(vectorDimension)
                        .build()
                ).get();
                log.info("Created Qdrant collection: {}", collectionName);
            }
        } catch (Exception e) {
            log.error("Failed to initialize Qdrant collection: {}", collectionName, e);
        }
    }

    @Override
    public void upsert(SkillKnowledgeChunk chunk, float[] vector) {
        try {
            var client = clientProvider.getClient();

            java.util.Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = new java.util.HashMap<>();
            payload.put("skillName", value(chunk.skillName()));
            payload.put("documentId", value(chunk.documentId()));
            payload.put("chunkIndex", value(chunk.chunkIndex()));
            payload.put("content", value(chunk.content()));
            if (chunk.source() != null) {
                payload.put("source", value(chunk.source()));
            }
            payload.put("enabled", value(true));

            PointStruct point = PointStruct.newBuilder()
                .setId(PointId.newBuilder().setUuid(chunk.chunkId()).build())
                .setVectors(Vectors.newBuilder()
                    .setVector(Vector.newBuilder().addAllData(toFloatList(vector)).build())
                    .build())
                .putAllPayload(payload)
                .build();

            client.upsertAsync(collectionName, List.of(point)).get();
        } catch (Exception e) {
            log.error("Failed to upsert knowledge chunk: {}", chunk.chunkId(), e);
        }
    }

    @Override
    public List<SkillKnowledgeSearchResult> search(
            float[] queryVector, Set<String> skillNames, int topK, float minScore) {
        try {
            var client = clientProvider.getClient();

            var searchBuilder = Points.SearchPoints.newBuilder()
                .setCollectionName(collectionName)
                .addAllVector(toFloatList(queryVector))
                .setLimit(topK)
                .setScoreThreshold(minScore)
                .setWithPayload(WithPayloadSelector.newBuilder().setEnable(true).build());

            if (skillNames != null && !skillNames.isEmpty()) {
                Filter.Builder filter = Filter.newBuilder();
                for (String name : skillNames) {
                    filter.addShould(Condition.newBuilder()
                        .setField(FieldCondition.newBuilder()
                            .setKey("skillName")
                            .setMatch(Match.newBuilder().setKeyword(name).build())
                            .build())
                        .build());
                }
                searchBuilder.setFilter(filter.build());
            }

            List<ScoredPoint> results = client.searchAsync(searchBuilder.build()).get();
            List<SkillKnowledgeSearchResult> out = new ArrayList<>();

            for (ScoredPoint sp : results) {
                String content = "";
                String skillName = "";
                String docId = "";
                if (sp.getPayloadMap().containsKey("content")) {
                    content = sp.getPayloadMap().get("content").getStringValue();
                }
                if (sp.getPayloadMap().containsKey("skillName")) {
                    skillName = sp.getPayloadMap().get("skillName").getStringValue();
                }
                if (sp.getPayloadMap().containsKey("documentId")) {
                    docId = sp.getPayloadMap().get("documentId").getStringValue();
                }

                out.add(new SkillKnowledgeSearchResult(
                        sp.getId().getUuid(), skillName, docId, content, "", null, sp.getScore()));
            }
            return out;

        } catch (Exception e) {
            log.error("Failed to search skill knowledge", e);
            return List.of();
        }
    }

    @Override
    public void deleteByDocument(String documentId) {
        try {
            var client = clientProvider.getClient();
            var filter = Filter.newBuilder()
                .addMust(Condition.newBuilder()
                    .setField(FieldCondition.newBuilder()
                        .setKey("documentId")
                        .setMatch(Match.newBuilder().setKeyword(documentId).build())
                        .build())
                    .build())
                .build();
            client.deleteAsync(collectionName, filter).get();
            log.info("Deleted knowledge documents: {}", documentId);
        } catch (Exception e) {
            log.error("Failed to delete document: {}", documentId, e);
        }
    }

    private List<Float> toFloatList(float[] vector) {
        List<Float> list = new ArrayList<>(vector.length);
        for (float v : vector) list.add(v);
        return list;
    }
}
