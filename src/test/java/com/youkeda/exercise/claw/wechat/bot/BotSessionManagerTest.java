package com.youkeda.exercise.claw.wechat.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BotSessionManager 测试
 *
 * <p>验证机器人登录状态的持久化与跨实例读取。
 */
class BotSessionManagerTest {

    @TempDir
    Path tempDir;

    private BotSessionManager manager;
    private String dbPath;

    @BeforeEach
    void setUp() {
        dbPath = tempDir.resolve("test-bot.db").toString();
        manager = new BotSessionManager(dbPath);
        manager.init();
    }

    @Test
    void shouldStartWithNoStateOnFirstRun() {
        assertNull(manager.getLastStatus(), "首次启动应无状态");
        assertNull(manager.getLastLoginTime(), "首次启动应无登录时间");
        assertFalse(manager.wasPreviouslyConnected(), "首次启动 should be false");
    }

    @Test
    void shouldPersistConnectedState() {
        manager.markConnected();

        assertEquals("CONNECTED", manager.getLastStatus());
        assertNotNull(manager.getLastLoginTime(), "登录时间不应为空");
        assertTrue(manager.wasPreviouslyConnected());

        // 模拟重启
        BotSessionManager newManager = new BotSessionManager(dbPath);
        newManager.init();

        assertEquals("CONNECTED", newManager.getLastStatus());
        assertNotNull(newManager.getLastLoginTime());
        assertTrue(newManager.wasPreviouslyConnected());
    }

    @Test
    void shouldPersistFailedState() {
        manager.markFailed("网络超时");

        assertEquals("FAILED", manager.getLastStatus());
        assertEquals("网络超时", manager.getLastError());
        assertFalse(manager.wasPreviouslyConnected());

        // 模拟重启
        BotSessionManager newManager = new BotSessionManager(dbPath);
        newManager.init();

        assertEquals("FAILED", newManager.getLastStatus());
        assertEquals("网络超时", newManager.getLastError());
        assertFalse(newManager.wasPreviouslyConnected());
    }

    @Test
    void shouldUpdateStateOnSubsequentLogins() {
        // 第一次登录失败
        manager.markFailed("首次失败");
        assertEquals("FAILED", manager.getLastStatus());

        // 第二次登录成功
        manager.markConnected();
        assertEquals("CONNECTED", manager.getLastStatus());
        assertNull(manager.getLastError());

        // 第三次又失败
        manager.markFailed("连接断开");
        assertEquals("FAILED", manager.getLastStatus());
        assertEquals("连接断开", manager.getLastError());
    }

    @Test
    void shouldTruncateLongErrorMessages() {
        String longError = "a".repeat(1000);
        manager.markFailed(longError);

        // 从内存中读取的是原始值
        assertEquals(longError, manager.getLastError());

        // 从 SQLite 读取的是截断后的值
        BotSessionManager newManager = new BotSessionManager(dbPath);
        newManager.init();
        String loaded = newManager.getLastError();
        assertNotNull(loaded);
        assertTrue(loaded.length() <= 503, "错误信息应被截断到 500 字符以内");
    }

    @Test
    void shouldOverwriteFailedWithConnected() {
        manager.markFailed("错误信息");
        manager.markConnected();

        BotSessionManager newManager = new BotSessionManager(dbPath);
        newManager.init();

        assertEquals("CONNECTED", newManager.getLastStatus());
        assertNull(newManager.getLastError(), "连接成功时错误信息应为 null");
    }

    @Test
    void shouldHandleMultipleInstancesWithSameDb() {
        BotSessionManager instance1 = new BotSessionManager(dbPath);
        instance1.init();
        BotSessionManager instance2 = new BotSessionManager(dbPath);
        instance2.init();

        instance1.markConnected();
        instance2.markFailed("实例2失败");

        // 共享同一个文件，应该看到最新状态
        BotSessionManager reader = new BotSessionManager(dbPath);
        reader.init();
        assertEquals("FAILED", reader.getLastStatus());
        assertEquals("实例2失败", reader.getLastError());
    }
}