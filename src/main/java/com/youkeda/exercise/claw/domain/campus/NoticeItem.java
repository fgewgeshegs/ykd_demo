package com.youkeda.exercise.claw.domain.campus;

public class NoticeItem {
    private Long id;
    private String title;
    private String url;
    private String publishAt;
    private String content;
    private NoticeType type;
    private double confidence;
    private String scoreSource;
    private String classifierReason;
    private String status;
    private Long processedAt;

    public NoticeItem() {}

    public NoticeItem(String title, String url, String publishAt) {
        this.title = title;
        this.url = url;
        this.publishAt = publishAt;
    }

    public boolean needsContent() {
        return content == null || content.isBlank();
    }

    // getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getPublishAt() { return publishAt; }
    public void setPublishAt(String publishAt) { this.publishAt = publishAt; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public NoticeType getType() { return type; }
    public void setType(NoticeType type) { this.type = type; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getScoreSource() { return scoreSource; }
    public void setScoreSource(String scoreSource) { this.scoreSource = scoreSource; }

    public String getClassifierReason() { return classifierReason; }
    public void setClassifierReason(String classifierReason) { this.classifierReason = classifierReason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getProcessedAt() { return processedAt; }
    public void setProcessedAt(Long processedAt) { this.processedAt = processedAt; }
}
