package com.youkeda.exercise.claw.feature.campus.collector;

import com.youkeda.exercise.claw.domain.campus.NotificationItem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * 南邮 Sudy CMS 通知列表页解析器（纯函数，不访问网络）。
 *
 * <p>实测页面结构（2026-08）：{@code <ul class="news_list"><li class="news">
 * <a href="相对路径"><div class="news_title line1">标题</div>
 * <div class="news_meta">日期</div></a></li>…</ul>}。
 * 由 {@link CampusNoticeCollector}（EXAM）与 {@link CompetitionCollector}（COMPETITION）复用。
 */
public class CampusListPageParser {

    /**
     * @param html     通知列表页完整 HTML
     * @param baseUri  页面 URL（用于把相对 href 解析成绝对 URL）
     * @param source   条目的 source 标识（EXAM / COMPETITION）
     * @return 解析出的条目列表；HTML 为空或解析异常时返回空列表
     */
    public List<NotificationItem> parse(String html, String baseUri, String source) {
        List<NotificationItem> items = new ArrayList<>();
        if (html == null || html.isBlank()) {
            return items;
        }
        Document doc = Jsoup.parse(html);
        if (baseUri == null || baseUri.isBlank()) {
            baseUri = "https://jwc.njupt.edu.cn/1622/list34.psp";
        }

        for (Element li : doc.select("li.news")) {
            Element a = li.selectFirst("a");
            if (a == null) {
                continue; // 无链接条目跳过
            }
            Element titleEl = li.selectFirst("div.news_title");
            if (titleEl == null) {
                continue; // 无标题条目跳过
            }
            String title = titleEl.text().trim();
            if (title.isEmpty()) {
                continue;
            }
            String url = resolveUrl(a.attr("href"), baseUri);
            String date = findDate(li);
            items.add(new NotificationItem(source, title, url, date));
        }
        return items;
    }

    private String resolveUrl(String href, String baseUri) {
        if (href == null || href.isBlank()) {
            return "";
        }
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }
        String base = baseUri.replaceAll("/[^/]*$", "/");
        if (href.startsWith("/")) {
            base = baseUri.replaceAll("^(https?://[^/]+).*$", "$1");
        }
        return base + (href.startsWith("/") ? "/" + href.substring(1) : href);
    }

    private String findDate(Element entry) {
        Element dateEl = entry.selectFirst("div.news_meta, span.date, span.time, em");
        return dateEl != null ? dateEl.text().trim() : "";
    }
}
