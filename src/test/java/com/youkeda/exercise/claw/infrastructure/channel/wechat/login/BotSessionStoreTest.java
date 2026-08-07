package com.youkeda.exercise.claw.infrastructure.channel.wechat.login;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BotSessionStoreTest {

    @Test
    void periodicPersistWithNullNicknameDoesNotWipeStoredNickname() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource(
                "jdbc:sqlite::memory:", true);
        BotSessionStore store = new BotSessionStore(new JdbcTemplate(dataSource));
        store.init();

        store.saveBotSession("bot-1", "{\"v\":1}", "栋栋");
        store.saveBotSession("bot-1", "{\"v\":2}", null);

        List<BotSessionStore.BotSessionRow> rows = store.getActiveBotSessions();
        assertEquals(1, rows.size());
        assertEquals("栋栋", rows.get(0).wxNickname());
        assertEquals("{\"v\":2}", rows.get(0).resumeContextJson());
    }

    @Test
    void newNonNullNicknameOverwritesStoredNickname() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource(
                "jdbc:sqlite::memory:", true);
        BotSessionStore store = new BotSessionStore(new JdbcTemplate(dataSource));
        store.init();

        store.saveBotSession("bot-1", "{\"v\":1}", "栋栋");
        store.saveBotSession("bot-1", "{\"v\":2}", "新昵称");

        List<BotSessionStore.BotSessionRow> rows = store.getActiveBotSessions();
        assertEquals("新昵称", rows.get(0).wxNickname());
    }

    @Test
    void newSessionWithNullNicknameStoresNull() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource(
                "jdbc:sqlite::memory:", true);
        BotSessionStore store = new BotSessionStore(new JdbcTemplate(dataSource));
        store.init();

        store.saveBotSession("bot-2", "{\"v\":1}", null);

        List<BotSessionStore.BotSessionRow> rows = store.getActiveBotSessions();
        assertEquals(1, rows.size());
        assertNull(rows.get(0).wxNickname());
    }
}
