package com.youkeda.exercise.claw.scout.collector;

import com.youkeda.exercise.claw.scout.planner.SearchTask;
import com.youkeda.exercise.claw.scout.processor.InformationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 采集器注册表
 */
@Component
public class CollectorRegistry {

    private static final Logger log = LoggerFactory.getLogger(CollectorRegistry.class);

    private final Map<String, Collector> collectors = new ConcurrentHashMap<>();

    public CollectorRegistry(List<Collector> collectorList) {
        for (Collector c : collectorList) {
            collectors.put(c.getType(), c);
            log.info("采集器已注册 | type={}", c.getType());
        }
    }

    /**
     * 对所有搜索任务执行采集
     */
    public List<InformationItem> collectAll(List<SearchTask> tasks) {
        List<InformationItem> allItems = new ArrayList<>();

        for (SearchTask task : tasks) {
            for (Collector collector : collectors.values()) {
                try {
                    List<InformationItem> items = collector.collect(task);
                    allItems.addAll(items);
                } catch (Exception e) {
                    log.error("采集失败 | type={} | task={}", collector.getType(), task.query(), e);
                }
            }
        }

        log.info("采集汇总 | tasks={} | items={}", tasks.size(), allItems.size());
        return allItems;
    }
}
