package com.youkeda.exercise.claw.agent.skill;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 公共 Qdrant 客户端提供者
 *
 * <p>QdrantMemoryStore 继续使用自己的客户端连接（保持兼容），
 * QdrantSkillKnowledgeStore 使用此提供者。
 */
@Component
public class QdrantClientProvider {

    private static final Logger log = LoggerFactory.getLogger(QdrantClientProvider.class);

    private static final String QDRANT_HOST = "localhost";
    private static final int QDRANT_PORT = 6334;

    private QdrantClient client;

    @PostConstruct
    public void init() {
        this.client = new QdrantClient(
            QdrantGrpcClient.newBuilder(QDRANT_HOST, QDRANT_PORT, false).build()
        );
        log.info("QdrantClientProvider initialized: {}:{}", QDRANT_HOST, QDRANT_PORT);
    }

    public QdrantClient getClient() {
        return client;
    }

    @PreDestroy
    public void close() {
        if (client != null) {
            client.close();
            log.info("QdrantClientProvider closed");
        }
    }
}
