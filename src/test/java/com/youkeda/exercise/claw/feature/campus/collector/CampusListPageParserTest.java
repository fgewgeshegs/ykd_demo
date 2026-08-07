package com.youkeda.exercise.claw.feature.campus.collector;

import com.youkeda.exercise.claw.domain.campus.NotificationItem;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class CampusListPageParserTest {

    private static final String BASE = "https://jwc.njupt.edu.cn/1622/list34.psp";

    private String loadFixture() {
        try (var in = getClass().getResourceAsStream("/fixtures/campus-njupt-notice-list.html")) {
            return new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("fixture 读取失败", e);
        }
    }

    @Test
    void parsesNjuptNoticeListStructure() {
        List<NotificationItem> items = new CampusListPageParser()
                .parse(loadFixture(), BASE, "EXAM");

        assertEquals(5, items.size(), "应解析出 5 条通知");
        for (NotificationItem item : items) {
            assertFalse(item.getTitle().isBlank(), "标题非空，实际=" + item.getTitle());
            assertTrue(item.getUrl().startsWith("https://jwc.njupt.edu.cn/"), "url 应解析为绝对地址，实际=" + item.getUrl());
            assertFalse(item.getPublishAt().isBlank(), "日期非空，实际=" + item.getPublishAt());
        }
        assertEquals("【教务科】关于2013届毕业班学生欠费学分的通知", items.get(0).getTitle());
        assertEquals("https://jwc.njupt.edu.cn/2013/0321/c1622a41183/page.htm", items.get(0).getUrl());
        assertEquals("2013-03-21", items.get(0).getPublishAt());
    }

    @Test
    void skipsMalformedEntries() {
        String html = """
                <ul class="news_list">
                  <li class="news n1 clearfix"><div class="news_title line1">无链接的通知</div></li>
                  <li class="news n2 clearfix"><a href="/2020/0101/c1a1/page.htm"></a></li>
                  <li class="news n3 clearfix">
                    <a href="/2020/0102/c1a2/page.htm"><div class="news_title line1">正常通知</div><div class="news_meta">2020-01-02</div></a>
                  </li>
                </ul>
                """;
        List<NotificationItem> items = new CampusListPageParser().parse(html, BASE, "EXAM");
        assertEquals(1, items.size(), "无链接/无标题条目应被跳过");
        assertEquals("正常通知", items.get(0).getTitle());
    }
}
