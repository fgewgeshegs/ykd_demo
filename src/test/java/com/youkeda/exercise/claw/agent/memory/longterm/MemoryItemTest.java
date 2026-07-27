package com.youkeda.exercise.claw.agent.memory.longterm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryItemTest {

    @Test
    void mergedEvidenceRetainsNewestEvidenceWhenCapped() {
        MemoryItem existing = MemoryItem.ofAuto(
                "u1", MemoryCategory.PREFERENCE, "hotel.amenities",
                "旧内容", "旧证据".repeat(300), 0.8f, 0.9f);
        MemoryItem incoming = MemoryItem.ofAuto(
                "u1", MemoryCategory.PREFERENCE, "hotel.amenities",
                "新内容", "最新证据必须保留", 0.8f, 0.9f);

        MemoryItem merged = existing.withResolvedContent(
                incoming, "合并内容", MemoryMergeAction.MERGE);

        assertTrue(merged.evidence().endsWith("最新证据必须保留"));
    }
}
