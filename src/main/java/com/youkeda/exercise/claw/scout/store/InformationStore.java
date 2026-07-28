package com.youkeda.exercise.claw.scout.store;

import com.youkeda.exercise.claw.scout.processor.InformationItem;

import java.util.List;

/**
 * 信息存储接口
 */
public interface InformationStore {

    /**
     * 批量保存信息
     */
    void batchSave(List<InformationItem> items);

    /**
     * 向量检索
     */
    List<InformationItem> searchByVector(String userId, float[] vector, int topK);

    /**
     * 获取最近的信息
     */
    List<InformationItem> getRecent(String userId, int limit);

    /**
     * 删除过期信息
     */
    void deleteExpired(String userId, long beforeTimestamp);
}
