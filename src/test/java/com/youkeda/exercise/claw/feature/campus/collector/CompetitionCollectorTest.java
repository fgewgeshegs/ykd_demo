package com.youkeda.exercise.claw.feature.campus.collector;

import com.youkeda.exercise.claw.domain.campus.NotificationItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CompetitionCollector 按 source 采集测试。
 *
 * <p>修复背景：旧实现硬编码 source="COMPETITION"，被 Activity/Job Source 复用时
 * 产生错误身份，先跑的源污染后跑的源命名空间。现在 collect 接收 {@link CampusNoticeSource}，
 * 由调用方传入真实身份。
 */
class CompetitionCollectorTest {

    private static final String FIXTURE_HTML = """
            <ul class="news_list">
              <li class="news n1 clearfix"><a href="/2020/0101/c1a1/page.htm"><div class="news_title line1">比赛通知A</div><div class="news_meta">2020-01-01</div></a></li>
              <li class="news n2 clearfix"><a href="/2020/0102/c1a2/page.htm"><div class="news_title line1">比赛通知B</div><div class="news_meta">2020-01-02</div></a></li>
            </ul>
            """;

    @Test
    void collectReturnsItemsWithProvidedSource() {
        // 通过可注入的 HTML 获取 seam 提供固定 HTML，绕开真实网络
        CompetitionCollector collector = new CompetitionCollector(url -> FIXTURE_HTML);

        List<NotificationItem> items = collector.collect(CampusNoticeSource.ACTIVITY);

        assertEquals(2, items.size());
        assertEquals("ACTIVITY", items.get(0).getSource(),
                "collect(CampusNoticeSource.ACTIVITY) 产出的 item 应携带 ACTIVITY 身份");
        assertEquals("ACTIVITY", items.get(1).getSource());
        assertEquals("比赛通知A", items.get(0).getTitle());
    }

    @Test
    void collectHonorsDifferentSourceForSamePage() {
        CompetitionCollector collector = new CompetitionCollector(url -> FIXTURE_HTML);

        List<NotificationItem> items = collector.collect(CampusNoticeSource.JOB);

        assertEquals(2, items.size());
        assertEquals("JOB", items.get(0).getSource(),
                "同一页面由不同 Source 采集，身份应为调用方传入的 JOB");
    }

    @Test
    void enumNameMatchesPersistedStringConstants() {
        // 落库用 name()，必须与历史字符串常量一致，否则去重/查询断档
        assertEquals("ACTIVITY", CampusNoticeSource.ACTIVITY.name());
        assertEquals("COMPETITION", CampusNoticeSource.COMPETITION.name());
        assertEquals("EXAM", CampusNoticeSource.EXAM.name());
        assertEquals("JOB", CampusNoticeSource.JOB.name());
    }
}
