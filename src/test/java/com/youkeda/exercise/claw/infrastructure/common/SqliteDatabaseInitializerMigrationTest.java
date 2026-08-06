package com.youkeda.exercise.claw.infrastructure.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;

import java.lang.reflect.Field;
import java.nio.file.Path;

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
    }
}
