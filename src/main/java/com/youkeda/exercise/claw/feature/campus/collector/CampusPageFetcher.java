package com.youkeda.exercise.claw.feature.campus.collector;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 校园列表页 HTML 抓取器，带短 TTL 缓存。
 *
 * <p>修复背景：同一次 campus 调度里 Activity/Competition/Job 三个 Source 复用
 * CompetitionCollector，加上 Exam 的 CampusNoticeCollector，同一列表页被 Jsoup
 * 抓取 4 次——浪费流量 + 反爬风险。此 fetcher 提供一次调度内同 URL 只抓一次的缓存。
 *
 * <p>职责边界：只负责 {@code fetchHtml(url) → HTML 字符串}，不含任何业务逻辑
 * （不分类、不去重、不入库、不推送）。采集/解析由各 Collector 负责。
 *
 * <p>缓存语义：TTL 30 秒（覆盖一次调度内多个 Source 的连续采集），过期自动重抓，
 * 不会跨调度返回过期页面。单例组件，跨调度存活。
 */
@Component
@ConditionalOnProperty(name = "campus.enabled", havingValue = "true")
public class CampusPageFetcher {

    private static final Logger log = LoggerFactory.getLogger(CampusPageFetcher.class);
    private static final int TIMEOUT_SECONDS = 15;
    private static final long CACHE_TTL_MILLIS = 30_000;

    /** 默认抓取：真实 Jsoup 请求 */
    private final Function<String, String> htmlFetcher;

    /** url → (html, 抓取时间戳)，并发安全 */
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /** Spring 默认构造器（真实 Jsoup 抓取） */
    public CampusPageFetcher() {
        this.htmlFetcher = this::fetchHtmlReal;
    }

    /** 测试 seam：注入 HTML 提供方，绕开网络 */
    CampusPageFetcher(Function<String, String> htmlFetcher) {
        this.htmlFetcher = htmlFetcher;
    }

    /**
     * 获取页面 HTML。TTL 缓存内同 URL 返回缓存结果；过期或未缓存则抓取。
     *
     * @param url 页面 URL
     * @return HTML 内容；抓取失败时抛异常（由调用方 Collector catch 降级）
     */
    public String fetchHtml(String url) {
        CacheEntry entry = cache.get(url);
        long now = System.currentTimeMillis();
        if (entry != null && now - entry.fetchedAt < CACHE_TTL_MILLIS) {
            return entry.html;
        }
        String html = htmlFetcher.apply(url);
        cache.put(url, new CacheEntry(html, now));
        return html;
    }

    private String fetchHtmlReal(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .timeout((int) Duration.ofSeconds(TIMEOUT_SECONDS).toMillis())
                    .userAgent("ClawBot-Campus/1.0")
                    .get();
            return doc.outerHtml();
        } catch (Exception e) {
            throw new IllegalStateException("抓取页面失败: " + url, e);
        }
    }

    private record CacheEntry(String html, long fetchedAt) {}
}
