package com.solarwinds.joboe.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeHostInfoReaderProviderTest {
    private final RuntimeHostInfoReaderProvider tested = new RuntimeHostInfoReaderProvider();

    @Test
    void returnServerHostInfoReader() {
        assertTrue(tested.getHostInfoReader() instanceof ServerHostInfoReader);
    }
}