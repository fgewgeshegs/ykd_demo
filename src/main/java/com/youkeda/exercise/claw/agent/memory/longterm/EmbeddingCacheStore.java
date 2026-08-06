package com.youkeda.exercise.claw.agent.memory.longterm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * SQLite 持久化 Embedding 向量缓存（L2 层）。
 *
 * <p>与内存级 Caffeine（L1）配合：L1 查不到时降级到本层，命中则回填 L1。
 * 向量一旦嵌入即永久缓存，进程重启、Ollama 离线均不受影响。
 *
 * <p>表结构：
 * <pre>{@code
 *   CREATE TABLE IF NOT EXISTS embedding_cache (
 *       text_hash TEXT PRIMARY KEY,   -- SHA-256 hex（64 chars）
 *       text TEXT NOT NULL,
 *       vector_blob BLOB NOT NULL,    -- float[] → byte[]（little-endian IEEE 754）
 *       model TEXT NOT NULL DEFAULT 'bge-m3',
 *       dimension INTEGER NOT NULL DEFAULT 1024,
 *       created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
 *   )
 * }</pre>
 */
@Component
public class EmbeddingCacheStore {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingCacheStore.class);

    private final JdbcTemplate jdbc;

    public EmbeddingCacheStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        ensureTable();
    }

    // ==================== 公共 API ====================

    /**
     * 根据原始文本查找缓存的 Embedding 向量。
     *
     * @param text 原始文本
     * @return 向量副本，未命中时返回 null
     */
    public float[] get(String text) {
        if (text == null || text.isBlank()) return null;
        String hash = sha256(text);
        try {
            byte[] blob = jdbc.queryForObject(
                    "SELECT vector_blob FROM embedding_cache WHERE text_hash = ?",
                    byte[].class, hash);
            if (blob == null) return null;
            float[] vector = bytesToFloats(blob);
            log.debug("Embedding L2 cache hit | textLen={} | hash={}", text.length(), hash);
            return vector;
        } catch (Exception e) {
            log.debug("Embedding L2 cache miss | textLen={} | hash={}", text.length(), hash);
            return null;
        }
    }

    /** 写入单条向量缓存。*/
    public void put(String text, float[] vector) {
        if (text == null || text.isBlank() || vector == null) return;
        String hash = sha256(text);
        byte[] blob = floatsToBytes(vector);
        try {
            jdbc.update(
                    "INSERT OR REPLACE INTO embedding_cache (text_hash, text, vector_blob, model, dimension, created_at) "
                            + "VALUES (?, ?, ?, 'bge-m3', ?, strftime('%s', 'now'))",
                    hash, text, blob, vector.length);
            log.debug("Embedding L2 cache write | textLen={} | hash={} | dim={}",
                    text.length(), hash, vector.length);
        } catch (Exception e) {
            log.warn("Embedding L2 cache write failed | textLen={} | error={}",
                    text.length(), e.getMessage());
        }
    }

    /** 批量写入向量缓存。异常安全：单条失败不影响其他。 */
    public void putBatch(Map<String, float[]> entries) {
        if (entries == null || entries.isEmpty()) return;
        int success = 0;
        for (var entry : entries.entrySet()) {
            try {
                put(entry.getKey(), entry.getValue());
                success++;
            } catch (Exception e) {
                log.warn("Embedding L2 batch write failed for one entry | textLen={}",
                        entry.getKey().length());
            }
        }
        log.debug("Embedding L2 batch write | total={} | success={}", entries.size(), success);
    }

    /** 返回缓存条目总数（诊断用）。 */
    public int count() {
        try {
            Integer c = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM embedding_cache", Integer.class);
            return c != null ? c : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // ==================== 序列化 ====================

    /** float[] → byte[]（little-endian，与 SQLite BLOB 兼容） */
    static byte[] floatsToBytes(float[] vector) {
        ByteBuffer buf = ByteBuffer.allocate(vector.length * Float.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        for (float v : vector) {
            buf.putFloat(v);
        }
        return buf.array();
    }

    /** byte[] → float[]（little-endian） */
    static float[] bytesToFloats(byte[] blob) {
        ByteBuffer buf = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN);
        float[] vector = new float[blob.length / Float.BYTES];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = buf.getFloat();
        }
        return vector;
    }

    // ==================== 内部工具 ====================

    private void ensureTable() {
        try {
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS embedding_cache (
                    text_hash TEXT PRIMARY KEY,
                    text TEXT NOT NULL,
                    vector_blob BLOB NOT NULL,
                    model TEXT NOT NULL DEFAULT 'bge-m3',
                    dimension INTEGER NOT NULL DEFAULT 1024,
                    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
                )
            """);
            log.info("Embedding L2 缓存表已就绪 | rows={}", count());
        } catch (Exception e) {
            log.warn("Embedding L2 缓存表初始化失败 | error={}", e.getMessage());
        }
    }

    static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
