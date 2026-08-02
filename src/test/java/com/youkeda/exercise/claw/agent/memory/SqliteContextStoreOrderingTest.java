package com.youkeda.exercise.claw.agent.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youkeda.exercise.claw.infrastructure.channel.wechat.user.WechatUserManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 回归测试：同一秒内写入的多条消息（如工具调用轮次：assistant tool_calls → tool → assistant）
 * 必须按插入顺序返回。
 *
 * <p>背景：created_at 为秒级时间戳（strftime('%s','now')），工具轮次的三条消息常在同一秒入库，
 * ORDER BY created_at 同值排序不定，导致 tool 消息可能排在它的 tool_calls 消息之前，
 * LLM 接口会以 400 拒绝该序列。修复：排序加 id 决胜键。
 */
class SqliteContextStoreOrderingTest {

    private SqliteContextStore store;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource(
                "jdbc:sqlite::memory:", true);
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
            CREATE TABLE context_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id TEXT NOT NULL,
                message_json TEXT NOT NULL,
                created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
            )
        """);

        WechatUserManager userManager = mock(WechatUserManager.class);
        when(userManager.getOwnerUserId()).thenReturn("user-1");

        store = new SqliteContextStore(jdbc, new ObjectMapper(), new StorageProperties(), userManager);
    }

    /** 直接以相同 created_at 插入，确定性复现「同一秒入库」的场景。 */
    private void insertSameSecond(String userJson) {
        jdbc.update(
                "INSERT INTO context_messages (user_id, message_json, created_at) VALUES ('user-1', ?, 1000)",
                userJson);
    }

    @Test
    void sameSecondMessagesReturnedInInsertionOrder() {
        // 模拟工具调用轮次，插入顺序 = assistant tool_calls → tool → assistant
        insertSameSecond("{\"role\":\"assistant\",\"content\":\"{\\\"action\\\":\\\"estimate\\\"}\","
                + "\"toolCallId\":\"call_1\",\"toolName\":\"didi_ride\"}");
        insertSameSecond("{\"role\":\"tool\",\"content\":\"ok\",\"toolCallId\":\"call_1\"}");
        insertSameSecond("{\"role\":\"assistant\",\"content\":\"价格如下\"}");

        List<Message> history = store.getHistory("user-1", 10);

        assertEquals(3, history.size());
        // 关键断言：tool 消息必须紧跟其 tool_calls 消息之后（即按插入顺序返回）
        assertEquals(MessageRole.ASSISTANT, history.get(0).role());
        assertEquals(MessageRole.TOOL, history.get(1).role());
        assertEquals("call_1", history.get(1).toolCallId());
        assertEquals(MessageRole.ASSISTANT, history.get(2).role());
        assertEquals("价格如下", history.get(2).content());
    }
}
