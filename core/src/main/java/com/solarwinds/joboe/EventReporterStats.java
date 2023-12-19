package com.solarwinds.joboe;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EventReporterStats {
    long sentCount;
    long overflowedCount;
    long failedCount;
    long queueLargestCount;
    long processedCount;
}