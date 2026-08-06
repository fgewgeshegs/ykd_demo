package com.youkeda.exercise.claw.domain.anime;

import java.util.List;

/**
 * 番剧实体。
 * 对应 AniList 中的一部番剧，与 AnimeEpisode 为一对多关系。
 */
public class Anime {

    private int anilistId;
    private String title;
    private String titleJa;       // 日文名（可选）
    private String titleZh;       // 中文译名（可选，LLM 翻译，空时显示回退罗马音）
    private String coverUrl;      // 封面 URL
    private String status;        // RELEASING / FINISHED / NOT_YET_RELEASED
    private int episodeCount;
    private List<String> genres;
    private int averageScore;     // AniList 评分（百分制，如 75），用于推荐预筛
    private int popularity;       // 热度，用于推荐预筛

    public Anime() {}

    public Anime(int anilistId, String title, String titleJa, String coverUrl, String status,
                 int episodeCount, List<String> genres, int averageScore, int popularity) {
        this(anilistId, title, titleJa, null, coverUrl, status, episodeCount, genres, averageScore, popularity);
    }

    public Anime(int anilistId, String title, String titleJa, String titleZh, String coverUrl, String status,
                 int episodeCount, List<String> genres, int averageScore, int popularity) {
        this.anilistId = anilistId;
        this.title = title;
        this.titleJa = titleJa;
        this.titleZh = titleZh;
        this.coverUrl = coverUrl;
        this.status = status;
        this.episodeCount = episodeCount;
        this.genres = genres;
        this.averageScore = averageScore;
        this.popularity = popularity;
    }

    public int getAnilistId() { return anilistId; }
    public void setAnilistId(int anilistId) { this.anilistId = anilistId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTitleJa() { return titleJa; }
    public void setTitleJa(String titleJa) { this.titleJa = titleJa; }

    public String getTitleZh() { return titleZh; }
    public void setTitleZh(String titleZh) { this.titleZh = titleZh; }

    /** 显示名：中文译名优先，无译名回退罗马音 */
    public String getDisplayTitle() {
        return (titleZh != null && !titleZh.isBlank()) ? titleZh : title;
    }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getEpisodeCount() { return episodeCount; }
    public void setEpisodeCount(int episodeCount) { this.episodeCount = episodeCount; }

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    public int getAverageScore() { return averageScore; }
    public void setAverageScore(int averageScore) { this.averageScore = averageScore; }

    public int getPopularity() { return popularity; }
    public void setPopularity(int popularity) { this.popularity = popularity; }
}
