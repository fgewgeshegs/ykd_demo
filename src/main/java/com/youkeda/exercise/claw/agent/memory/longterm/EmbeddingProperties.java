package com.youkeda.exercise.claw.agent.memory.longterm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Embedding 模型配置
 *
 * 需要一个 OpenAI 兼容的 /v1/embeddings 端点（如 Xinference、Ollama 部署的 BGE-M3）
 */
@Component
@ConfigurationProperties(prefix = "memory.embedding")
public class EmbeddingProperties {

    /** Embedding API 基础地址 */
    private String baseUrl = "http://localhost:8082";

    /** API 密钥（本地部署通常不需要） */
    private String apiKey = "";

    /** 模型名称 */
    private String model = "bge-m3";

    /** 向量维度 */
    private int dimension = 1024;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }
}
