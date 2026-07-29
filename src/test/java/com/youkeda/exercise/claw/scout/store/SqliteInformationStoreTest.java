package com.youkeda.exercise.claw.scout.store;

import com.youkeda.exercise.claw.scout.processor.InformationItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteInformationStoreTest {

    @TempDir
    Path tempDir;

    private SqliteInformationStore store;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:sqlite:" + tempDir.resolve("scout.db"));
        store = new SqliteInformationStore(new JdbcTemplate(dataSource));
        store.init();
    }

    @Test
    void savesAndReturnsActuallyNewestItemsWithVectors() {
        InformationItem older = item(
                "旧信息", "https://example.com/old", 100L, new float[]{1f, 0f});
        InformationItem newer = item(
                "新信息", "https://example.com/new", 200L, new float[]{0f, 1f});

        store.batchSave(List.of(older, newer));

        List<InformationItem> recent = store.getRecent("owner", 1);
        assertEquals(1, recent.size());
        assertEquals("新信息", recent.get(0).getTitle());
        assertArrayEquals(new float[]{0f, 1f}, recent.get(0).getVector());
    }

    @Test
    void vectorSearchRanksByCosineSimilarity() {
        store.batchSave(List.of(
                item("Java", "https://example.com/java", 100L, new float[]{1f, 0f}),
                item("AI", "https://example.com/ai", 200L, new float[]{0f, 1f})
        ));

        List<InformationItem> results =
                store.searchByVector("owner", new float[]{0.1f, 0.9f}, 1);

        assertEquals(1, results.size());
        assertEquals("AI", results.get(0).getTitle());
    }

    @Test
    void stableIdentityUpsertsAndExpiredRowsAreDeleted() {
        InformationItem first = item(
                "原始标题", "https://example.com/news?utm_source=rss", 100L,
                new float[]{1f});
        InformationItem updated = item(
                "更新标题", "https://example.com/news", 200L, new float[]{1f});

        store.batchSave(List.of(first));
        store.batchSave(List.of(updated));

        List<InformationItem> recent = store.getRecent("owner", 10);
        assertEquals(1, recent.size());
        assertEquals("更新标题", recent.get(0).getTitle());

        store.deleteExpired("owner", 201L);
        assertTrue(store.getRecent("owner", 10).isEmpty());
    }

    private InformationItem item(String title, String source, long collectedAt, float[] vector) {
        InformationItem item = InformationItem.create(
                "owner", title, "正文", source, "WEB_SEARCH", "NEWS");
        item.setPublishedAt(collectedAt);
        item.setCollectedAt(collectedAt);
        item.setSummary("摘要");
        item.setVector(vector);
        return item;
    }
}
