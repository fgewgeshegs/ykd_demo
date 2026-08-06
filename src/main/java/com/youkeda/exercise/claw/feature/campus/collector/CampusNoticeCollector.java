package com.youkeda.exercise.claw.feature.campus.collector;

import com.youkeda.exercise.claw.domain.campus.NotificationItem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@ConditionalOnProperty(name = "campus.enabled", havingValue = "true")
public class CampusNoticeCollector {

    private static final Logger log = LoggerFactory.getLogger(CampusNoticeCollector.class);
    private static final int TIMEOUT_SECONDS = 15;

    /** 南邮教务处通知公告页 */
    public static final String NJUPT_NOTICE_URL = "https://jwc.njupt.edu.cn/1622/list34.psp";

    private final CampusListPageParser parser = new CampusListPageParser();

    /**
     * 爬取通知列表，只抓 title + url + date
     *
     * @param schoolUrl 教务处通知列表页 URL
     * @return 通知列表（不含正文），source 固定为 EXAM
     */
    public List<NotificationItem> collect(String schoolUrl) {
        try {
            Document doc = Jsoup.connect(schoolUrl)
                    .timeout((int) Duration.ofSeconds(TIMEOUT_SECONDS).toMillis())
                    .userAgent("ClawBot-Campus/1.0")
                    .get();
            List<NotificationItem> items = parser.parse(doc.outerHtml(), schoolUrl, "EXAM");
            log.info("通知列表采集完成 | url={} | count={}", schoolUrl, items.size());
            return items;
        } catch (Exception e) {
            log.error("通知列表采集失败 | url={}", schoolUrl, e);
            return List.of();
        }
    }
}
