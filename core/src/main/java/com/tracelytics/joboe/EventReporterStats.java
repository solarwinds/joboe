package com.tracelytics.joboe;


public class EventReporterStats {
    private long sentCount;
    private long overflowedCount;
    private long failedCount;
    private long queueLargestCount;
    private long processedCount;
    
   
    public EventReporterStats(long sentCount, long overflowedCount, long failedCount, long processedCount, long queueLargestCount) {
        super();
        this.sentCount = sentCount;
        this.overflowedCount = overflowedCount;
        this.failedCount = failedCount;
        this.processedCount = processedCount;
        this.queueLargestCount = queueLargestCount;
    }

    public long getSentCount() {
        return sentCount;
    }

    public long getOverflowedCount() {
        return overflowedCount;
    }

    public long getFailedCount() {
        return failedCount;
    }

    public long getQueueLargestCount() {
        return queueLargestCount;
    }
    
    public long getProcessedCount() {
        return processedCount;
    }
}
