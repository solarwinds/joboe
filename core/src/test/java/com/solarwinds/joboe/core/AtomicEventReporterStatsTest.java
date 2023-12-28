package com.solarwinds.joboe.core;

import com.solarwinds.joboe.core.AtomicEventReporterStats;
import com.solarwinds.joboe.core.EventReporterStats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AtomicEventReporterStatsTest {

    private final AtomicEventReporterStats tested = new AtomicEventReporterStats(() -> 10);

    @Test
    void testConsumeStats() {
        tested.incrementFailedCount(1);
        tested.incrementProcessedCount(1);
        tested.incrementOverflowedCount(1);

        tested.incrementSentCount(1);
        tested.setQueueCount(1);

        EventReporterStats expected = new EventReporterStats(1, 1, 1, 1, 1);
        EventReporterStats actual = tested.consumeStats();
        assertEquals(expected, actual);

        expected = new EventReporterStats(0, 0, 0, 10, 0);
        actual = tested.consumeStats();
        assertEquals(expected, actual);
    }
}