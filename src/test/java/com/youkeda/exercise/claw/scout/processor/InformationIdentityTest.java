package com.youkeda.exercise.claw.scout.processor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class InformationIdentityTest {

    @Test
    void ignoresTrackingParametersAndFragment() {
        String first = InformationIdentity.stableKey(
                "https://Example.com/news/1/?utm_source=wechat&a=1#details", "标题");
        String second = InformationIdentity.stableKey(
                "https://example.com/news/1?a=1", "另一个标题");

        assertEquals(first, second);
    }

    @Test
    void pointIdentityIsStablePerOwner() {
        InformationItem first = InformationItem.create(
                "owner", "标题", "内容", "https://example.com/a", "WEB_SEARCH", "NEWS");
        InformationItem second = InformationItem.create(
                "owner", "新标题", "新内容", "https://example.com/a", "WEB_SEARCH", "NEWS");
        InformationItem otherOwner = InformationItem.create(
                "other", "标题", "内容", "https://example.com/a", "WEB_SEARCH", "NEWS");

        assertEquals(InformationIdentity.pointUuid(first), InformationIdentity.pointUuid(second));
        assertNotEquals(InformationIdentity.pointUuid(first), InformationIdentity.pointUuid(otherOwner));
    }
}
