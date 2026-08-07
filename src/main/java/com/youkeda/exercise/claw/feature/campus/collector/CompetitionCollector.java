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
 * 校园通知列表采集器。
 *
 * <p>从教务处通知列表页抓取 HTML 并解析出条目，按调用方传入的 {@link CampusNoticeSource}
 * 给每条 item 标注真实来源身份。被 Activity / Competition / Job 三个 Source 复用，
 * 因此不再硬编码 COMPETITION（旧实现会让先跑的源污染后跑的源命名空间）。
 *
 * <p>HTML 获取统一委托 {@link CampusPageFetcher}（带 30s 缓存），一次调度内同一列表页
 * 只抓一次，不再各自 Jsoup connect（旧实现同页被抓 4 次）。
 */
@Component("campusCompetitionCollector")
@ConditionalOnProperty(name = "campus.enabled", havingValue = "true")
public class CompetitionCollector {

    private static final Logger log = LoggerFactory.getLogger(CompetitionCollector.class);

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
    public CompetitionCollector(CampusPageFetcher fetcher,
                                @Value("${campus.notice-url:" + CampusListPageParser.DEFAULT_NOTICE_URL + "}") String noticeUrl) {
        this.fetcher = fetcher;
        this.noticeUrl = noticeUrl;
    }

    /** 测试 seam：注入 HTML 提供方，绕开网络（内部包一个 fetcher），URL 用默认值 */
    CompetitionCollector(Function<String, String> htmlFetcher) {
        this(htmlFetcher, CampusListPageParser.DEFAULT_NOTICE_URL);
    }

    /** 测试 seam：注入 HTML 提供方 + 显式 URL（验证配置可覆盖默认） */
    CompetitionCollector(Function<String, String> htmlFetcher, String noticeUrl) {
        this.fetcher = new CampusPageFetcher(htmlFetcher);
        this.noticeUrl = noticeUrl;
    }

    /**
     * 采集通知列表，所有标题返回，由调用方 classifier 筛选。
     *
     * @param source 调用方（Activity / Competition / Job）的真实身份
     * @return 通知条目列表，每条携带 {@code source} 身份；采集失败返回空列表
     */
    public List<NotificationItem> collect(CampusNoticeSource source) {
        try {
            String html = fetcher.fetchHtml(noticeUrl);
            List<NotificationItem> items = parser.parse(html, noticeUrl, source.name());
            log.info("比赛采集完成 | source={} | count={}", source, items.size());
            return items;
        } catch (Exception e) {
            log.error("比赛采集失败 | url={}", noticeUrl, e);
            return List.of();
        }
    }
}
