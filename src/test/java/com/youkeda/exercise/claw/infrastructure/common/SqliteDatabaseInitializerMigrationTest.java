package com.youkeda.exercise.claw.infrastructure.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;

import java.lang.reflect.Field;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteDatabaseInitializerMigrationTest {

    @TempDir
    Path tempDir;

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void rebuildsLegacyEmptyAnimeReminderTask() throws Exception {
        SQLiteDataSource ds = new SQLiteDataSource();
        String url = "jdbc:sqlite:" + tempDir.resolve("migration.db");
        ds.setUrl(url);
        JdbcTemplate jdbc = new JdbcTemplate(ds);

        // 预置旧版空表：anime_reminder_task 缺 airing_at/唯一约束
        jdbc.execute("""
            CREATE TABLE anime_reminder_task (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                anilist_id  INTEGER NOT NULL,
                episode     INTEGER NOT NULL,
                remind_time INTEGER NOT NULL,
                status      TEXT NOT NULL DEFAULT 'PENDING',
                created_at  INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
            )
            """);

        SqliteDatabaseInitializer initializer = new SqliteDatabaseInitializer(jdbc);
        setField(initializer, "datasourceUrl", url);
        initializer.init();

        var reminderCols = jdbc.queryForList("PRAGMA table_info(anime_reminder_task)");
        assertTrue(reminderCols.stream().anyMatch(row -> "airing_at".equals(row.get("name"))),
                "anime_reminder_task 应含 airing_at 列");
        String reminderSql = jdbc.queryForObject(
                "SELECT sql FROM sqlite_master WHERE type='table' AND name='anime_reminder_task'", String.class);
        assertTrue(reminderSql != null && reminderSql.contains("UNIQUE(anilist_id, episode)"),
                "anime_reminder_task 应有唯一约束，实际=" + reminderSql);
        var reminderIndexes = jdbc.queryForList("PRAGMA index_list(anime_reminder_task)");
        assertTrue(reminderIndexes.stream().anyMatch(row -> "idx_reminder_status_time".equals(row.get("name"))),
                "重建后 anime_reminder_task 应含 idx_reminder_status_time 索引");
    }

    @Test
    void doesNotDropNonEmptyLegacyTable() throws Exception {
        SQLiteDataSource ds = new SQLiteDataSource();
        String url = "jdbc:sqlite:" + tempDir.resolve("migration-nonempty.db");
        ds.setUrl(url);
        JdbcTemplate jdbc = new JdbcTemplate(ds);

        // 预置旧版非空表：含 1 行 PENDING（模拟未来有数据时的防御场景）
        jdbc.execute("""
            CREATE TABLE anime_reminder_task (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                anilist_id  INTEGER NOT NULL,
                episode     INTEGER NOT NULL,
                remind_time INTEGER NOT NULL,
                status      TEXT NOT NULL DEFAULT 'PENDING',
                created_at  INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
            )
            """);
        jdbc.update("""
            INSERT INTO anime_reminder_task (anilist_id, episode, remind_time, status, created_at)
            VALUES (?, ?, ?, 'PENDING', ?)
            """, 210031, 5, 1000L, 1000L);

        SqliteDatabaseInitializer initializer = new SqliteDatabaseInitializer(jdbc);
        setField(initializer, "datasourceUrl", url);
        initializer.init();

        // 表未被重建：仍缺 airing_at，且原数据保留
        var reminderCols = jdbc.queryForList("PRAGMA table_info(anime_reminder_task)");
        assertFalse(reminderCols.stream().anyMatch(row -> "airing_at".equals(row.get("name"))),
                "非空旧表不应被重建");
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM anime_reminder_task", Integer.class);
        assertTrue(count != null && count == 1, "原数据应保留，实际=" + count);
    }

    @Test
    void freshDatabaseGetsCorrectSchemaWithoutMigration() throws Exception {
        SQLiteDataSource ds = new SQLiteDataSource();
        String url = "jdbc:sqlite:" + tempDir.resolve("fresh.db");
        ds.setUrl(url);
        JdbcTemplate jdbc = new JdbcTemplate(ds);

        SqliteDatabaseInitializer initializer = new SqliteDatabaseInitializer(jdbc);
        setField(initializer, "datasourceUrl", url);
        initializer.init();

        var reminderCols = jdbc.queryForList("PRAGMA table_info(anime_reminder_task)");
        assertTrue(reminderCols.stream().anyMatch(row -> "airing_at".equals(row.get("name"))));
        assertFalse(reminderCols.isEmpty());
        String reminderSql = jdbc.queryForObject(
                "SELECT sql FROM sqlite_master WHERE type='table' AND name='anime_reminder_task'", String.class);
        assertTrue(reminderSql != null && reminderSql.contains("UNIQUE(anilist_id, episode)"),
                "全新库 anime_reminder_task 应有唯一约束，实际=" + reminderSql);
    }

    @Test
    void legacySubscriptionTableGetsTitleZhColumn() throws Exception {
        SQLiteDataSource ds = new SQLiteDataSource();
        String url = "jdbc:sqlite:" + tempDir.resolve("sub-migration.db");
        ds.setUrl(url);
        JdbcTemplate jdbc = new JdbcTemplate(ds);

        // 预置旧版 anime_subscription（缺 title_zh 列），并插入一行真实数据模拟
        jdbc.execute("""
            CREATE TABLE anime_subscription (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                anilist_id  INTEGER NOT NULL UNIQUE,
                title       TEXT NOT NULL,
                title_ja    TEXT DEFAULT '',
                cover_url   TEXT DEFAULT '',
                status      TEXT DEFAULT 'RELEASING',
                genres      TEXT DEFAULT '[]',
                created_at  INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
            )
            """);
        jdbc.update("""
            INSERT INTO anime_subscription (anilist_id, title, title_ja, cover_url, status, genres)
            VALUES (?, ?, ?, ?, 'RELEASING', '[]')
            """, 210031, "Grand Blue Season 3", "ぐらんぶる", "");

        SqliteDatabaseInitializer initializer = new SqliteDatabaseInitializer(jdbc);
        setField(initializer, "datasourceUrl", url);
        initializer.init();

        var cols = jdbc.queryForList("PRAGMA table_info(anime_subscription)");
        assertTrue(cols.stream().anyMatch(row -> "title_zh".equals(row.get("name"))),
                "旧版表迁移后应含 title_zh 列");
        // 原数据保留（未 DROP）
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM anime_subscription", Integer.class);
        assertTrue(count != null && count == 1, "迁移不得丢数据，实际=" + count);
    }

    @Test
    void freshDatabaseGetsTitleZhColumn() throws Exception {
        SQLiteDataSource ds = new SQLiteDataSource();
        String url = "jdbc:sqlite:" + tempDir.resolve("sub-fresh.db");
        ds.setUrl(url);
        JdbcTemplate jdbc = new JdbcTemplate(ds);

        SqliteDatabaseInitializer initializer = new SqliteDatabaseInitializer(jdbc);
        setField(initializer, "datasourceUrl", url);
        initializer.init();

        var cols = jdbc.queryForList("PRAGMA table_info(anime_subscription)");
        assertTrue(cols.stream().anyMatch(row -> "title_zh".equals(row.get("name"))),
                "全新库 anime_subscription 应含 title_zh 列");
    }

    @Test
    void legacyCampusNoticeGetsUrlSourceUniqueConstraint() throws Exception {
        SQLiteDataSource ds = new SQLiteDataSource();
        String url = "jdbc:sqlite:" + tempDir.resolve("campus-legacy.db");
        ds.setUrl(url);
        JdbcTemplate jdbc = new JdbcTemplate(ds);

        // 预置旧版 campus_notice：单列 url UNIQUE，无 source 列（source 是后 ALTER 加的）
        jdbc.execute("""
            CREATE TABLE campus_notice (
                id                INTEGER PRIMARY KEY AUTOINCREMENT,
                title             TEXT NOT NULL,
                url               TEXT NOT NULL UNIQUE,
                publish_at        TEXT,
                content           TEXT DEFAULT '',
                type              TEXT DEFAULT 'UNKNOWN',
                confidence        REAL DEFAULT 0,
                score_source      TEXT DEFAULT 'NONE',
                classifier_reason TEXT DEFAULT '',
                status            TEXT DEFAULT 'UNPROCESSED',
                processed_at      INTEGER,
                created_at        INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
            )
            """);
        jdbc.update("""
            INSERT INTO campus_notice (title, url, publish_at, status)
            VALUES ('旧数据', 'https://jwc.njupt.edu.cn/2020/0101/c1a1/page.htm', '2020-01-01', 'UNPROCESSED')
            """);

        SqliteDatabaseInitializer initializer = new SqliteDatabaseInitializer(jdbc);
        setField(initializer, "datasourceUrl", url);
        initializer.init();

        // 复合约束存在：PRAGMA index_list 有 unique 索引且列恰为 [url, source]
        boolean hasUrlSourceUnique = hasUniqueIndexOn(jdbc, "campus_notice", "url", "source");
        assertTrue(hasUrlSourceUnique, "campus_notice 应有 UNIQUE(url, source) 复合约束");
        // 单列 url UNIQUE 已被替换：不应再有仅 [url] 的 unique 索引
        assertFalse(hasUniqueIndexOn(jdbc, "campus_notice", "url"),
                "campus_notice 不应再保留单列 url UNIQUE");
        // source 列 NOT NULL 且无默认值（迁移后新表不带 DEFAULT）
        boolean sourceNoDefault = jdbc.queryForList("PRAGMA table_info(campus_notice)").stream()
                .filter(row -> "source".equals(row.get("name")))
                .allMatch(row -> row.get("dflt_value") == null);
        assertTrue(sourceNoDefault, "source 列不应有 DEFAULT（避免默认值污染身份）");
        // 历史数据按「清空重建」决策不保留
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM campus_notice", Integer.class);
        assertEquals(0, count, "迁移按清空重建决策，不应保留旧行");
    }

    @Test
    void freshCampusNoticeGetsUrlSourceUniqueWithoutMigration() throws Exception {
        SQLiteDataSource ds = new SQLiteDataSource();
        String url = "jdbc:sqlite:" + tempDir.resolve("campus-fresh.db");
        ds.setUrl(url);
        JdbcTemplate jdbc = new JdbcTemplate(ds);

        SqliteDatabaseInitializer initializer = new SqliteDatabaseInitializer(jdbc);
        setField(initializer, "datasourceUrl", url);
        initializer.init();

        assertTrue(hasUniqueIndexOn(jdbc, "campus_notice", "url", "source"),
                "全新库 campus_notice 直接含 UNIQUE(url, source)");
        assertFalse(hasUniqueIndexOn(jdbc, "campus_notice", "url"),
                "全新库不应有单列 url UNIQUE");
    }

    @Test
    void legacyCampusNoticeWithSourceDefaultColumnIsRebuilt() throws Exception {
        // 精确复刻生产库中间态：单列 url UNIQUE + ALTER 加的 source 列（DEFAULT 'EXAM'）
        SQLiteDataSource ds = new SQLiteDataSource();
        String url = "jdbc:sqlite:" + tempDir.resolve("campus-mid.db");
        ds.setUrl(url);
        JdbcTemplate jdbc = new JdbcTemplate(ds);

        jdbc.execute("""
            CREATE TABLE campus_notice (
                id                INTEGER PRIMARY KEY AUTOINCREMENT,
                title             TEXT NOT NULL,
                url               TEXT NOT NULL UNIQUE,
                publish_at        TEXT,
                content           TEXT DEFAULT '',
                type              TEXT DEFAULT 'UNKNOWN',
                confidence        REAL DEFAULT 0,
                score_source      TEXT DEFAULT 'NONE',
                classifier_reason TEXT DEFAULT '',
                status            TEXT DEFAULT 'UNPROCESSED',
                processed_at      INTEGER,
                created_at        INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
            , source TEXT NOT NULL DEFAULT 'EXAM')
            """);

        SqliteDatabaseInitializer initializer = new SqliteDatabaseInitializer(jdbc);
        setField(initializer, "datasourceUrl", url);
        initializer.init();

        assertTrue(hasUniqueIndexOn(jdbc, "campus_notice", "url", "source"),
                "中间态（含 source DEFAULT EXAM）也应迁移为复合约束");
        boolean sourceNoDefault = jdbc.queryForList("PRAGMA table_info(campus_notice)").stream()
                .filter(row -> "source".equals(row.get("name")))
                .allMatch(row -> row.get("dflt_value") == null);
        assertTrue(sourceNoDefault, "迁移后 source 不应保留 DEFAULT 'EXAM'（避免默认值污染）");
    }

    /** PRAGMA 辅助：表上是否存在 UNIQUE 索引且其列集合恰好等于给定列 */
    private boolean hasUniqueIndexOn(JdbcTemplate jdbc, String table, String... expectedCols) {
        var indexes = jdbc.queryForList("PRAGMA index_list(" + table + ")");
        for (var idx : indexes) {
            boolean unique = "1".equals(String.valueOf(idx.get("unique")));
            if (!unique) continue;
            var cols = jdbc.queryForList("PRAGMA index_info(" + idx.get("name") + ")");
            var names = cols.stream().map(r -> String.valueOf(r.get("name"))).toList();
            if (names.equals(java.util.Arrays.asList(expectedCols))) {
                return true;
            }
        }
        return false;
    }
}
