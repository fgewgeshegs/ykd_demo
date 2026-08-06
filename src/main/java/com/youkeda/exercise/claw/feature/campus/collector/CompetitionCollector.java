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

@Component("campusCompetitionCollector")
@ConditionalOnProperty(name = "campus.enabled", havingValue = "true")
public class CompetitionCollector {

    private static final Logger log = LoggerFactory.getLogger(CompetitionCollector.class);
    private static final int TIMEOUT_SECONDS = 15;
    private static final String NJUPT_NOTICE_URL = "https://jwc.njupt.edu.cn/1622/list34.psp";

    private final CampusListPageParser parser = new CampusListPageParser();

    /**
     * 采集通知列表，所有标题返回，由 CompetitionClassifier 筛选比赛相关
     */
    public List<NotificationItem> collect() {
        try {
            Document doc = Jsoup.connect(NJUPT_NOTICE_URL)
                    .timeout((int) Duration.ofSeconds(TIMEOUT_SECONDS).toMillis())
                    .userAgent("ClawBot-Campus/1.0")
                    .get();
            List<NotificationItem> items = parser.parse(doc.outerHtml(), NJUPT_NOTICE_URL, "COMPETITION");
            log.info("比赛采集完成 | count={}", items.size());
            return items;
        } catch (Exception e) {
            log.error("比赛采集失败 | url={}", NJUPT_NOTICE_URL, e);
            return List.of();
        }
    }
}
