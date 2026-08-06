package com.youkeda.exercise.claw.feature.anime.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.domain.anime.Anime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnimeSubscriptionStoreTest {

    @TempDir
    Path tempDir;

    private JdbcTemplate jdbc;
    private AnimeSubscriptionStore store;

    @BeforeEach
    void setUp() {
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:" + tempDir.resolve("sub.db"));
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("""
            CREATE TABLE anime_subscription (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                anilist_id  INTEGER NOT NULL UNIQUE,
                title       TEXT NOT NULL,
                title_ja    TEXT DEFAULT '',
                title_zh    TEXT DEFAULT '',
                cover_url   TEXT DEFAULT '',
                status      TEXT DEFAULT 'RELEASING',
                genres      TEXT DEFAULT '[]',
                created_at  INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
            )
            """);
        store = new AnimeSubscriptionStore(jdbc, new ObjectMapper());
    }

    @Test
    void subscribePersistsTitleZh() {
        Anime anime = new Anime(210031, "Grand Blue Season 3", "ぐらんぶる", "",
                "RELEASING", 12, List.of("Comedy"), 80, 100000);
        anime.setTitleZh("碧蓝之海 第三季");
        store.subscribe(anime);

        List<Anime> all = store.listAll();
        assertEquals(1, all.size());
        assertEquals("碧蓝之海 第三季", all.get(0).getTitleZh());
        assertEquals("碧蓝之海 第三季", all.get(0).getDisplayTitle());
        assertEquals("Grand Blue Season 3", all.get(0).getTitle());
    }

    @Test
    void subscribeWithoutTitleZhDefaultsToEmptyAndDisplayFallsBack() {
        Anime anime = new Anime(210031, "Grand Blue Season 3", "ぐらんぶる", "",
                "RELEASING", 12, List.of("Comedy"), 80, 100000);
        store.subscribe(anime);

        Anime loaded = store.findByAnilistId(210031);
        assertNotNull(loaded);
        assertNull(loaded.getTitleZh());
        assertEquals("Grand Blue Season 3", loaded.getDisplayTitle());
    }

    @Test
    void resubscribeRefreshesTitleZhOnConflict() {
        store.subscribe(new Anime(210031, "Grand Blue Season 3", "ぐらんぶる", "",
                "RELEASING", 12, List.of("Comedy"), 80, 100000));

        // 重新订阅：本次翻译成功 → 刷新 title_zh，其余列不变
        Anime resub = new Anime(210031, "Grand Blue Season 3", "ぐらんぶる", "",
                "RELEASING", 12, List.of("Comedy"), 80, 100000);
        resub.setTitleZh("碧蓝之海 第三季");
        store.subscribe(resub);

        Anime loaded = store.findByAnilistId(210031);
        assertNotNull(loaded);
        assertEquals("碧蓝之海 第三季", loaded.getTitleZh(), "重复订阅成功后应刷新 title_zh");
        assertEquals(1, store.listAll().size(), "重复订阅不应新增行");
    }

    @Test
    void resubscribeWithNullTitleZhKeepsExistingTranslation() {
        Anime first = new Anime(210031, "Grand Blue Season 3", "ぐらんぶる", "",
                "RELEASING", 12, List.of("Comedy"), 80, 100000);
        first.setTitleZh("碧蓝之海 第三季");
        store.subscribe(first);

        // 再次订阅：本次翻译失败（title_zh=null）→ 不得清空已有译名
        store.subscribe(new Anime(210031, "Grand Blue Season 3", "ぐらんぶる", "",
                "RELEASING", 12, List.of("Comedy"), 80, 100000));

        assertEquals("碧蓝之海 第三季", store.findByAnilistId(210031).getTitleZh(),
                "翻译失败的重复订阅不应清空已有译名");
    }

    @Test
    void updateTitleZhOnlyFillsMatchingRow() {
        store.subscribe(new Anime(1, "A", "あ", "", "RELEASING", 12, List.of(), 70, 100000));
        store.subscribe(new Anime(2, "B", "い", "", "RELEASING", 12, List.of(), 70, 100000));

        store.updateTitleZh(1, "中文A");

        assertEquals("中文A", store.findByAnilistId(1).getTitleZh());
        assertNull(store.findByAnilistId(2).getTitleZh(), "未命中的行不应被改动");
    }
}
