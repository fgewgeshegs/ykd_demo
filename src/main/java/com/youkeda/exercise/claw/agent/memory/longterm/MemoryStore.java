package com.youkeda.exercise.claw.agent.memory.longterm;

import java.util.List;

/**
 * 长期记忆存储接口
 *
 * 底层由 Qdrant 向量数据库实现，支持语义检索和精确查询。
 */
public interface MemoryStore {

    /**
     * 保存/更新一条记忆（含向量）
     *
     * @param item   记忆数据
     * @param vector 记忆内容的向量表示
     */
    boolean upsert(MemoryItem item, float[] vector);

    /**
     * 语义检索：给定查询向量，返回当前用户 Top-K 条最相关的记忆
     *
     * @param userId     用户标识（用于过滤）
     * @param queryVector 查询向量
     * @param topK       返回条数
     * @return 按相似度降序排列的记忆列表
     */
    List<MemoryItem> search(String userId, float[] queryVector, int topK);

    /**
     * 语义检索，并过滤低于最低相关度的结果。
     */
    List<MemoryItem> search(String userId, float[] queryVector, int topK,
                            float minScore);

    /**
     * 语义检索并保留底层相关度，供服务层结合重要性和时效性重排。
     */
    List<MemorySearchResult> searchScored(String userId, float[] queryVector,
                                          int topK, float minScore);

    /** 按稳定主题键查找最近更新的记忆。 */
    MemoryItem findByTopicKey(String userId, String topicKey);

    /**
     * 语义检索 + 分类过滤
     */
    List<MemoryItem> search(String userId, float[] queryVector, int topK,
                            MemoryCategory category);

    /**
     * 获取用户全部记忆（按更新时间降序）
     */
    List<MemoryItem> getAll(String userId);

    /**
     * 删除一条记忆
     */
    boolean delete(String userId, String memoryId);

    /**
     * 清除用户全部记忆
     */
    void clear(String userId);

    /**
     * 获取用户记忆总数
     */
    int count(String userId);
}
