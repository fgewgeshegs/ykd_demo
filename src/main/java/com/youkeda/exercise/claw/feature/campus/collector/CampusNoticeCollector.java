package com.youkeda.exercise.claw.feature.campus.collector;

import com.youkeda.exercise.claw.domain.campus.NotificationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    private final CampusListPageParser parser = new CampusListPageParser();
    private final CampusPageFetcher fetcher;
    private final String noticeUrl;

    /**
     * Spring 构造器（注入共享 fetcher + 通知列表页 URL）。
     *
     * <p>URL 来自配置 {@code campus.notice-url}，默认 {@link CampusListPageParser#DEFAULT_NOTICE_URL}
     * （当前有效栏目 1594）。@Autowired 必标：有测试 seam 第二构造器时，Spring 不会自动选注入构造器。
     */
    @Autowired
    public CampusNoticeCollector(CampusPageFetcher fetcher,
                                 @Value("${campus.notice-url:" + CampusListPageParser.DEFAULT_NOTICE_URL + "}") String noticeUrl) {
        this.fetcher = fetcher;
        this.noticeUrl = noticeUrl;
    }

    /** 测试 seam：注入 HTML 提供方，绕开网络，URL 用默认值 */
    CampusNoticeCollector(Function<String, String> htmlFetcher) {
        this(htmlFetcher, CampusListPageParser.DEFAULT_NOTICE_URL);
    }

    /** 测试 seam：注入 HTML 提供方 + 显式 URL（验证配置可覆盖默认） */
    CampusNoticeCollector(Function<String, String> htmlFetcher, String noticeUrl) {
        this.fetcher = new CampusPageFetcher(htmlFetcher);
        this.noticeUrl = noticeUrl;
    }

    /** 默认南邮教务处通知公告页 URL（可被配置覆盖） */
    public String getNoticeUrl() {
        return noticeUrl;
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
