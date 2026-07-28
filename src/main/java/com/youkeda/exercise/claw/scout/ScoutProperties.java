package com.youkeda.exercise.claw.scout;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 信息猎手配置
 */
@Component
@ConfigurationProperties(prefix = "scout")
public class ScoutProperties {

    /** 总开关 */
    private boolean enabled = false;

    /** 语义匹配最低分数（0.0-1.0） */
    private float minMatchScore = 0.60f;

    /** 每次最大候选信息数 */
    private int maxCandidates = 10;

    /** 每次最大推荐数 */
    private int maxRecommendations = 5;

    /** 信息保留天数 */
    private int ttlDays = 7;

    /** 推荐定时任务 cron 表达式 */
    private String cron = "0 0 8 * * *";

    /** 搜索任务数 */
    private int searchTaskCount = 5;

    /** 每个搜索任务最大结果数 */
    private int maxResultsPerTask = 5;

    /** 同一信息再次允许推送前的冷却天数 */
    private int deliveryCooldownDays = 30;

    private Qdrant qdrant = new Qdrant();
    private Rss rss = new Rss();

    // Getters & Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public float getMinMatchScore() { return minMatchScore; }
    public void setMinMatchScore(float minMatchScore) { this.minMatchScore = minMatchScore; }
    public int getMaxCandidates() { return maxCandidates; }
    public void setMaxCandidates(int maxCandidates) { this.maxCandidates = maxCandidates; }
    public int getMaxRecommendations() { return maxRecommendations; }
    public void setMaxRecommendations(int maxRecommendations) { this.maxRecommendations = maxRecommendations; }
    public int getTtlDays() { return ttlDays; }
    public void setTtlDays(int ttlDays) { this.ttlDays = ttlDays; }
    public String getCron() { return cron; }
    public void setCron(String cron) { this.cron = cron; }
    public int getSearchTaskCount() { return searchTaskCount; }
    public void setSearchTaskCount(int searchTaskCount) { this.searchTaskCount = searchTaskCount; }
    public int getMaxResultsPerTask() { return maxResultsPerTask; }
    public void setMaxResultsPerTask(int maxResultsPerTask) { this.maxResultsPerTask = maxResultsPerTask; }
    public int getDeliveryCooldownDays() { return deliveryCooldownDays; }
    public void setDeliveryCooldownDays(int deliveryCooldownDays) { this.deliveryCooldownDays = deliveryCooldownDays; }
    public Qdrant getQdrant() { return qdrant; }
    public void setQdrant(Qdrant qdrant) { this.qdrant = qdrant; }
    public Rss getRss() { return rss; }
    public void setRss(Rss rss) { this.rss = rss; }

    public static class Qdrant {
        private String collection = "scout_information";
        private int vectorDimension = 1024;

        public String getCollection() { return collection; }
        public void setCollection(String collection) { this.collection = collection; }
        public int getVectorDimension() { return vectorDimension; }
        public void setVectorDimension(int vectorDimension) { this.vectorDimension = vectorDimension; }
    }

    public static class Rss {
        private boolean enabled = false;
        private List<String> feeds = new ArrayList<>();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public List<String> getFeeds() { return feeds; }
        public void setFeeds(List<String> feeds) { this.feeds = feeds; }
    }
}
