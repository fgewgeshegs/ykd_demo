package com.youkeda.exercise.claw.feature.anime.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimeScheduleStoreTest {

    @TempDir
    Path tempDir;

    private JdbcTemplate jdbc;
    private AnimeScheduleStore store;

    @BeforeEach
    void setUp() {
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:" + tempDir.resolve("anime.db"));
        jdbc = new JdbcTemplate(ds);
        // 复用 Task 2 的新 DDL（含 airing_at + 唯一约束）
        jdbc.execute("""
            CREATE TABLE anime_schedule (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                anilist_id  INTEGER NOT NULL,
                episode     INTEGER NOT NULL,
                airing_at   INTEGER NOT NULL,
                notified    INTEGER NOT NULL DEFAULT 0,
                created_at  INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
                UNIQUE(anilist_id, episode)
            )
            """);
        jdbc.execute("""
            CREATE TABLE anime_reminder_task (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                anilist_id  INTEGER NOT NULL,
                episode     INTEGER NOT NULL,
                remind_time INTEGER NOT NULL,
                airing_at   INTEGER NOT NULL,
                status      TEXT NOT NULL DEFAULT 'PENDING',
                created_at  INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
                UNIQUE(anilist_id, episode)
            )
            """);
        store = new AnimeScheduleStore(jdbc);
    }

    @Test
    void createReminderTaskIsIdempotent() {
        store.createReminderTask(210031, 5, 1000L, 2000L);
        store.createReminderTask(210031, 5, 1000L, 2000L); // 同一集重复 → 忽略
        store.createReminderTask(210031, 6, 3000L, 4000L); // 不同集 → 新增

        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM anime_reminder_task", Integer.class);
        assertEquals(2, count, "同集重复建任务应被唯一约束去重");
    }

    @Test
    void getPendingRemindersReturnsDueTasksWithAiringAt() {
        store.createReminderTask(210031, 5, 1000L, 2000L); // remind_time=1000 <= now=1500 → 到期
        store.createReminderTask(210031, 6, 9000L, 10000L); // remind_time=9000 > now=1500 → 未到期

        List<AnimeScheduleStore.ReminderTask> pending = store.getPendingReminders(1500L);

        assertEquals(1, pending.size());
        assertEquals(5, pending.get(0).getEpisode());
        assertEquals(2000L, pending.get(0).getAiringAt(), "airingAt 应从任务表自身读取");
        assertEquals("PENDING", pending.get(0).getStatus());
    }

    @Test
    void markReminderSentUpdatesStatus() {
        store.createReminderTask(210031, 5, 1000L, 2000L);
        var pending = store.getPendingReminders(1500L);
        assertEquals(1, pending.size());
        store.markReminderSent(pending.get(0).getId());

        assertTrue(store.getPendingReminders(1500L).isEmpty(), "SENT 后不应再出现在待执行列表");
    }

    @Test
    void getPendingRemindersOrdersByRemindTimeAscending() {
        store.createReminderTask(210031, 5, 9000L, 10000L); // remind_time 较晚
        store.createReminderTask(210031, 6, 1000L, 2000L);  // remind_time 较早（插入顺序打乱）

        List<AnimeScheduleStore.ReminderTask> pending = store.getPendingReminders(20000L);

        assertEquals(2, pending.size());
        assertEquals(1000L, pending.get(0).getRemindTime(), "应按 remind_time ASC 排序");
        assertEquals(9000L, pending.get(1).getRemindTime());
        assertEquals(6, pending.get(0).getEpisode());
        assertEquals(5, pending.get(1).getEpisode());
    }
}
