package com.youkeda.exercise.claw.feature.campus.store;

import com.youkeda.exercise.claw.domain.campus.NotificationItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CampusNotificationStore 去重测试（真实 SQLite + @TempDir）。
 *
 * <p>修复背景：旧实现按 (url, source) SELECT→INSERT 两步查重，但表约束是单列
 * url UNIQUE——不同 Source 采集同 URL 时 INSERT 撞约束抛 SQLITE_CONSTRAINT_UNIQUE
 * （日志「去重写入失败」风暴）。现在改 INSERT OR IGNORE，由复合 UNIQUE(url, source)
 * 原子保证：同 URL 不同 Source 可共存，同 URL 同 Source 幂等。
 */
class CampusNotificationStoreTest {

    @TempDir
    Path tempDir;

    private JdbcTemplate jdbc;
    private CampusNotificationStore store;

    @BeforeEach
    void setUp() {
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:" + tempDir.resolve("campus-notice.db"));
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("""
            CREATE TABLE campus_notice (
                id                INTEGER PRIMARY KEY AUTOINCREMENT,
                title             TEXT NOT NULL,
                url               TEXT NOT NULL,
                publish_at        TEXT,
                content           TEXT DEFAULT '',
                type              TEXT DEFAULT 'UNKNOWN',
                confidence        REAL DEFAULT 0,
                score_source      TEXT DEFAULT 'NONE',
                classifier_reason TEXT DEFAULT '',
                status            TEXT DEFAULT 'UNPROCESSED',
                processed_at      INTEGER,
                created_at        INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
                source            TEXT NOT NULL,
                UNIQUE(url, source)
            )
            """);
        store = new CampusNotificationStore(jdbc);
    }

    private NotificationItem item(String source, String url, String title) {
        return new NotificationItem(source, title, url, "2026-08-07");
    }

    @Test
    void sameUrlDifferentSourcesCoexist() {
        // 同 URL 被三个 Source 采集，应插入 3 行（复合约束按 source 区分）
        List<NotificationItem> first = store.deduplicate(List.of(
                item("ACTIVITY", "https://jwc.njupt.edu.cn/2026/0807/c1a1/page.htm", "通知A")));
        List<NotificationItem> second = store.deduplicate(List.of(
                item("COMPETITION", "https://jwc.njupt.edu.cn/2026/0807/c1a1/page.htm", "通知A")));
        List<NotificationItem> third = store.deduplicate(List.of(
                item("JOB", "https://jwc.njupt.edu.cn/2026/0807/c1a1/page.htm", "通知A")));

        assertEquals(1, first.size(), "首次应作为新通知");
        assertEquals(1, second.size(), "同 URL 不同 source 应作为新通知");
        assertEquals(1, third.size(), "同 URL 不同 source 应作为新通知");

        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM campus_notice", Integer.class);
        assertEquals(3, count, "同 URL 三个 source 应共存 3 行");
    }

    @Test
    void sameUrlSameSourceIsIdempotent() {
        store.deduplicate(List.of(
                item("COMPETITION", "https://jwc.njupt.edu.cn/2026/0807/c1a1/page.htm", "通知A")));
        List<NotificationItem> again = store.deduplicate(List.of(
                item("COMPETITION", "https://jwc.njupt.edu.cn/2026/0807/c1a1/page.htm", "通知A")));

        assertEquals(0, again.size(), "同 URL 同 source 再次采集应幂等返回空");
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM campus_notice", Integer.class);
        assertEquals(1, count, "同 URL 同 source 只应插入 1 行");
    }

    @Test
    void mixedBatchOnlyInsertsNewOnes() {
        // 预插一条 COMPETITION，再批量传含重复 + 新 URL 的列表，应只插入新 URL
        store.deduplicate(List.of(
                item("COMPETITION", "https://jwc.njupt.edu.cn/2026/0807/c1a1/page.htm", "通知A")));

        List<NotificationItem> newItems = store.deduplicate(List.of(
                item("COMPETITION", "https://jwc.njupt.edu.cn/2026/0807/c1a1/page.htm", "通知A"), // 重复
                item("COMPETITION", "https://jwc.njupt.edu.cn/2026/0807/c1a2/page.htm", "通知B"), // 新
                item("EXAM", "https://jwc.njupt.edu.cn/2026/0807/c1a1/page.htm", "通知A")));       // 同 URL 新 source

        assertEquals(2, newItems.size(), "应只返回 2 条新通知（新 URL + 同 URL 新 source）");
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM campus_notice", Integer.class);
        assertEquals(3, count, "表内应为 1(预插) + 2(新插入) = 3 行");
    }
}
