package com.youkeda.exercise.claw.infrastructure.channel.wechat.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 微信消息幂等去重服务测试。
 */
class MessageDeduplicationServiceTest {

    @Test
    void newMessageIsNotDuplicateAndIsRecorded() {
        MessageDeduplicationService svc = new MessageDeduplicationService();
        assertFalse(svc.isDuplicate("msg-1"), "新消息不应被判为重复");
        assertFalse(svc.isDuplicate("msg-2"), "另一条新消息也不应被判为重复");
    }

    @Test
    void sameMessageIsDuplicateOnSecondCheck() {
        MessageDeduplicationService svc = new MessageDeduplicationService();
        assertFalse(svc.isDuplicate("msg-1"));
        assertTrue(svc.isDuplicate("msg-1"), "同一 messageId 第二次出现应判为重复");
        assertTrue(svc.isDuplicate("msg-1"), "再次出现仍应判为重复");
    }

    @Test
    void sameMessageWithDifferentItemIndexIsNotDuplicate() {
        MessageDeduplicationService svc = new MessageDeduplicationService();
        // 同一 messageId 的多个 item 应分别去重（key = messageId|index）
        assertFalse(svc.isDuplicate("msg-1|0"));
        assertFalse(svc.isDuplicate("msg-1|1"), "同一 messageId 的不同 item 不应互相误判");
        assertTrue(svc.isDuplicate("msg-1|0"), "相同 item 重投时应判为重复");
    }

    @Test
    void nullOrBlankMessageIdPassesThrough() {
        MessageDeduplicationService svc = new MessageDeduplicationService();
        assertFalse(svc.isDuplicate(null), "null 去重键应放行");
        assertFalse(svc.isDuplicate(""), "空去重键应放行");
        assertFalse(svc.isDuplicate("   "), "空白去重键应放行");
    }

    @Test
    void capacityEvictsOldestEntries() {
        MessageDeduplicationService svc = new MessageDeduplicationService(100, 30 * 60 * 1000L);
        for (int i = 0; i < 1000; i++) {
            svc.isDuplicate("bulk-" + i);
        }
        assertTrue(svc.size() <= 100, "缓存条目数不应超过容量上限");
    }

    @Test
    void expiredEntryIsTreatedAsNew() throws Exception {
        MessageDeduplicationService svc = new MessageDeduplicationService(100, 50);
        assertFalse(svc.isDuplicate("expired-1"));
        Thread.sleep(80);
        assertFalse(svc.isDuplicate("expired-1"), "过期后的同一消息应视为新消息");
    }
}
