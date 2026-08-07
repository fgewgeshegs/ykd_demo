package com.youkeda.exercise.claw.feature.campus.collector;

import com.youkeda.exercise.claw.domain.campus.NotificationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * 南邮教务处通知列表采集器（EXAM）。
 *
 * <p>抓取指定学校 URL 的通知列表，只抓 title + url + date，source 固定为 EXAM。
 * HTML 获取统一委托 {@link CampusPageFetcher}（带 30s 缓存），与
 * {@link CompetitionCollector} 共享，一次调度内同一列表页只抓一次。
 */
@Component
@ConditionalOnProperty(name = "campus.enabled", havingValue = "true")
public class CampusNoticeCollector {

    private static final Logger log = LoggerFactory.getLogger(CampusNoticeCollector.class);

    /** 南邮教务处通知公告页 */
    public static final String NJUPT_NOTICE_URL = "https://jwc.njupt.edu.cn/1622/list34.psp";

    private final CampusListPageParser parser = new CampusListPageParser();
    private final CampusPageFetcher fetcher;

    /** Spring 构造器（注入共享 fetcher）。@Autowired 必标：有测试 seam 第二构造器时，Spring 不会自动选注入构造器 */
    @Autowired
    public CampusNoticeCollector(CampusPageFetcher fetcher) {
        this.fetcher = fetcher;
    }

    /** 测试 seam：注入 HTML 提供方，绕开网络 */
    CampusNoticeCollector(Function<String, String> htmlFetcher) {
        this.fetcher = new CampusPageFetcher(htmlFetcher);
    }

    /**
     * 爬取通知列表，只抓 title + url + date
     *
     * @param schoolUrl 教务处通知列表页 URL
     * @return 通知列表（不含正文），source 固定为 EXAM
     */
    public List<NotificationItem> collect(String schoolUrl) {
        try {
            String html = fetcher.fetchHtml(schoolUrl);
            List<NotificationItem> items = parser.parse(html, schoolUrl, "EXAM");
            log.info("通知列表采集完成 | url={} | count={}", schoolUrl, items.size());
            return items;
        } catch (Exception e) {
            log.error("通知列表采集失败 | url={}", schoolUrl, e);
            return List.of();
        }
    }
}
