package com.youkeda.exercise.claw.feature.campus.collector;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CampusPageFetcher 快照缓存测试。
 *
 * <p>修复背景：同一次 campus 调度里 Activity/Competition/Job 三个 Source 复用
 * CompetitionCollector，加上 Exam 的 CampusNoticeCollector，同一列表页被抓 4 次。
 * 引入 fetcher 后，同一次调度（同实例生命周期内）同 URL 只抓一次，后续复用缓存。
 */
class CampusPageFetcherTest {

    @Test
    void sameUrlFetchedOncePerLifecycle() {
        AtomicInteger fetchCount = new AtomicInteger();
        CampusPageFetcher fetcher = new CampusPageFetcher(url -> {
            fetchCount.incrementAndGet();
            return "<html>content</html>";
        });

        String first = fetcher.fetchHtml("https://jwc.njupt.edu.cn/1622/list34.psp");
        String second = fetcher.fetchHtml("https://jwc.njupt.edu.cn/1622/list34.psp");

        assertEquals("<html>content</html>", first);
        assertEquals("<html>content</html>", second);
        assertEquals(1, fetchCount.get(), "同 URL 在同一 fetcher 生命周期内应只抓取一次");
    }

    @Test
    void differentUrlsFetchedSeparately() {
        AtomicInteger fetchCount = new AtomicInteger();
        CampusPageFetcher fetcher = new CampusPageFetcher(url -> {
            fetchCount.incrementAndGet();
            return "<html>" + url + "</html>";
        });

        fetcher.fetchHtml("https://a.example.com/list");
        fetcher.fetchHtml("https://b.example.com/list");

        assertEquals(2, fetchCount.get(), "不同 URL 不应共享缓存");
    }
}
