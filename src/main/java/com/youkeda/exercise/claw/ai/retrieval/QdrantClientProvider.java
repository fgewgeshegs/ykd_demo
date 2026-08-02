package com.youkeda.exercise.claw.ai.retrieval;

import com.youkeda.exercise.claw.agent.memory.longterm.QdrantProperties;
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

    private final QdrantProperties properties;

    private QdrantClient client;

    public QdrantClientProvider(QdrantProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        this.client = new QdrantClient(
            QdrantGrpcClient.newBuilder(properties.getHost(), properties.getPort(), false).build()
        );
        log.info("QdrantClientProvider initialized: {}:{}", properties.getHost(), properties.getPort());
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
