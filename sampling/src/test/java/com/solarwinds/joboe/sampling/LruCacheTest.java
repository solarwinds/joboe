package com.solarwinds.joboe.sampling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LruCacheTest {
    private final LruCache<String, String> tested = new LruCache<>(1);


    @Test
    void ensureMaximumSizeIsRespect() {
        tested.put("one", "one");
        tested.put("two", "two");

        assertEquals(1, tested.size());
        assertEquals("two", tested.get("two"));
    }
}