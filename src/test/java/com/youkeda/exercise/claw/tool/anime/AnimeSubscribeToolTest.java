package com.youkeda.exercise.claw.tool.anime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.agent.runtime.ToolRegistry;
import com.youkeda.exercise.claw.domain.anime.Anime;
import com.youkeda.exercise.claw.feature.anime.client.AniListClient;
import com.youkeda.exercise.claw.feature.anime.store.AnimeSubscriptionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link AnimeSubscribeTool} 的失败语义：
 * 任何未完成订阅的情况都必须返回 ERROR，绝不能返回 SUCCESS，
 * 否则 LLM 会基于"假成功"回复"已订阅"。
 */
class AnimeSubscribeToolTest {

    @TempDir
    Path tempDir;

    private ObjectMapper objectMapper;
    private ToolRegistry registry;
    private AniListClient aniListClient;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        registry = new ToolRegistry();
        aniListClient = mock(AniListClient.class);

        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("anime.db"));
        jdbc = new JdbcTemplate(dataSource);
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
    }

    private AnimeSubscriptionStore newStore() {
        return new AnimeSubscriptionStore(jdbc, objectMapper);
    }

    private Anime grandBlue() {
        return new Anime(199111, "Grand Blue Season 3", "グランブルー", "http://cover",
                "RELEASING", 12, List.of("Comedy", "Slice of Life"), 82, 100000);
    }

    @Test
    @DisplayName("AniList 搜索为空时订阅必须返回 ERROR（Tool FAILED），且不写库")
    void searchEmptyReturnsError() throws Exception {
        AnimeSubscribeTool tool = new AnimeSubscribeTool(
                aniListClient, newStore(), registry, objectMapper);
        when(aniListClient.searchAnime(anyString())).thenReturn(List.of());

        String result = tool.execute("{\"action\":\"subscribe\",\"animeName\":\"无职转生\"}");
        JsonNode json = objectMapper.readTree(result);

        assertEquals("ERROR", json.path("status").asText(),
                "搜索不到番剧时不得返回 SUCCESS，否则 LLM 会误报订阅成功");
        assertEquals(0, jdbc.queryForList("SELECT * FROM anime_subscription").size(),
                "未完成订阅不得写入任何记录");
    }

    @Test
    @DisplayName("正常订阅 Grand Blue 时数据库新增记录")
    void subscribeInsertsRecord() throws Exception {
        AnimeSubscribeTool tool = new AnimeSubscribeTool(
                aniListClient, newStore(), registry, objectMapper);
        when(aniListClient.getAnimeById(199111)).thenReturn(grandBlue());

        String result = tool.execute("{\"action\":\"subscribe\",\"animeId\":199111}");
        JsonNode json = objectMapper.readTree(result);

        assertEquals("SUCCESS", json.path("status").asText());
        assertEquals(1, jdbc.queryForList("SELECT * FROM anime_subscription").size(),
                "订阅成功必须落库");
        String title = jdbc.queryForObject(
                "SELECT title FROM anime_subscription WHERE anilist_id = 199111", String.class);
        assertEquals("Grand Blue Season 3", title);
    }

    @Test
    @DisplayName("数据库保存失败时必须返回 ERROR，不得谎报成功")
    void storeFailureReturnsError() throws Exception {
        AnimeSubscriptionStore throwingStore = mock(AnimeSubscriptionStore.class);
        doThrow(new RuntimeException("db down")).when(throwingStore).subscribe(any(Anime.class));
        AnimeSubscribeTool tool = new AnimeSubscribeTool(
                aniListClient, throwingStore, registry, objectMapper);
        when(aniListClient.getAnimeById(1)).thenReturn(grandBlue());

        String result = tool.execute("{\"action\":\"subscribe\",\"animeId\":1}");
        JsonNode json = objectMapper.readTree(result);

        assertEquals("ERROR", json.path("status").asText(),
                "数据库保存失败必须返回 ERROR，否则用户收到「已订阅」但实际未落库");
    }
}