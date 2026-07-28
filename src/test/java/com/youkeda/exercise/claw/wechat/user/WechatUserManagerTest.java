package com.youkeda.exercise.claw.wechat.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WechatUserManager 测试
 *
 * <p>验证微信用户记录的持久化、跨用户隔离、交互次数累计。
 */
class WechatUserManagerTest {

    @TempDir
    Path tempDir;

    private WechatUserManager manager;
    private String dbPath;

    @BeforeEach
    void setUp() {
        dbPath = tempDir.resolve("test-users.db").toString();
        manager = new WechatUserManager(dbPath);
        manager.init();
    }

    @Test
    void shouldRecordFirstInteraction() {
        manager.recordInteraction("user_A");

        assertEquals(1, manager.getInteractionCount("user_A"));
    }

    @Test
    void shouldIncrementInteractionCount() {
        manager.recordInteraction("user_A");
        manager.recordInteraction("user_A");

        assertEquals(2, manager.getInteractionCount("user_A"));
    }

    @Test
    void shouldIncrementAcrossMultipleCalls() {
        for (int i = 0; i < 5; i++) {
            manager.recordInteraction("user_A");
        }

        assertEquals(5, manager.getInteractionCount("user_A"));
    }

    @Test
    void shouldPersistCountAcrossInstances() {
        manager.recordInteraction("user_A");
        manager.recordInteraction("user_A");
        manager.recordInteraction("user_A");

        // 模拟重启
        WechatUserManager newManager = new WechatUserManager(dbPath);
        newManager.init();

        assertEquals(3, newManager.getInteractionCount("user_A"));
    }

    @Test
    void shouldNotShareCountBetweenUsers() {
        manager.recordInteraction("user_A");
        manager.recordInteraction("user_A");
        manager.recordInteraction("user_B");

        assertEquals(2, manager.getInteractionCount("user_A"));
        assertEquals(1, manager.getInteractionCount("user_B"));
    }

    @Test
    void shouldReturnZeroForUnknownUser() {
        assertEquals(0, manager.getInteractionCount("nonexistent"));
    }

    @Test
    void shouldHandleNullUserId() {
        assertDoesNotThrow(() -> manager.recordInteraction(null));
        assertDoesNotThrow(() -> manager.recordInteraction(null, "nick"));
        assertEquals(0, manager.getInteractionCount(null));
    }

    @Test
    void shouldHandleEmptyUserId() {
        assertDoesNotThrow(() -> manager.recordInteraction(""));
        assertEquals(0, manager.getInteractionCount(""));
    }

    @Test
    void shouldReturnCorrectUserCount() {
        assertEquals(0, manager.getUserCount());

        manager.recordInteraction("user_A");
        manager.recordInteraction("user_B");
        manager.recordInteraction("user_C");

        assertEquals(3, manager.getUserCount());

        // 重复用户不增加计数
        manager.recordInteraction("user_A");
        assertEquals(3, manager.getUserCount());
    }

    @Test
    void shouldHandleNicknameRecording() {
        assertDoesNotThrow(() -> manager.recordInteraction("user_A", "张三"));
        assertDoesNotThrow(() -> manager.recordInteraction("user_B", null));
        assertDoesNotThrow(() -> manager.recordInteraction("user_C", ""));
    }

    @Test
    void shouldReturnPersistedOwnerAfterRestart() {
        manager.recordInteraction("owner");

        WechatUserManager restarted = new WechatUserManager(dbPath);
        restarted.init();

        assertEquals("owner", restarted.getOwnerUserId());
    }
}
