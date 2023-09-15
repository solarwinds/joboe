package com.tracelytics.joboe;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Stats on reporter
 *
 * @author pluk
 */
public class AtomicEventReporterStats {
    private final AtomicLong sentCount = new AtomicLong();
    private final AtomicLong overflowedCount = new AtomicLong();
    private final AtomicLong failedCount = new AtomicLong();
    private final AtomicLong queueLargestCount = new AtomicLong();
    private final AtomicLong processedCount = new AtomicLong();

    private final Supplier<Integer> queueSizeSupplier;

    public AtomicEventReporterStats(Supplier<Integer> queueSizeSupplier) {
        this.queueSizeSupplier = queueSizeSupplier;
    }

    public void incrementSentCount(long increment) {
        sentCount.addAndGet(increment);
    }

    public void incrementOverflowedCount(long increment) {
        overflowedCount.addAndGet(increment);
    }

    public void incrementFailedCount(long increment) {
        failedCount.addAndGet(increment);
    }

    public void incrementProcessedCount(long increment) {
        processedCount.addAndGet(increment);
    }

    public void setQueueCount(long currentCount) {
        synchronized (queueLargestCount) {
            if (currentCount > queueLargestCount.get()) {
                queueLargestCount.set(currentCount);
            }
        }
    }

    public EventReporterStats consumeStats() {
        long sentCount = this.sentCount.getAndSet(0);
        long overflowedCount = this.overflowedCount.getAndSet(0);
        long failedCount = this.failedCount.getAndSet(0);

        long queueLargestCount = this.queueLargestCount.getAndSet(queueSizeSupplier.get()); //reset to current queue size as the largest
        long processedCount = this.processedCount.getAndSet(0);
        return EventReporterStats.builder()
                .sentCount(sentCount)
                .failedCount(failedCount)
                .overflowedCount(overflowedCount)
                .queueLargestCount(queueLargestCount)
                .processedCount(processedCount)
                .build();
    }
}