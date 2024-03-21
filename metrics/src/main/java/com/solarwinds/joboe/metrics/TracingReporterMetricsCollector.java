package com.solarwinds.joboe.metrics;

import com.solarwinds.joboe.core.EventReporter;
import com.solarwinds.joboe.core.EventReporterStats;
import com.solarwinds.joboe.core.metrics.measurement.SimpleMeasurementMetricsEntry;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Sub metrics collector that collects metrics specific to the Tracing event reporter such as number of tracing events sent,
 * the largest queue size since last collection etc
 * @author pluk
 *
 */
@RequiredArgsConstructor
public class TracingReporterMetricsCollector extends AbstractMetricsEntryCollector {
    private final EventReporter eventReporter;
    @Override
   public List<SimpleMeasurementMetricsEntry> collectMetricsEntries() throws Exception {
        EventReporterStats stats = eventReporter.consumeStats();
        
        List<SimpleMeasurementMetricsEntry> metricsEntries = new ArrayList<SimpleMeasurementMetricsEntry>();
        metricsEntries.add(new SimpleMeasurementMetricsEntry("NumSent", stats.getSentCount()));
        metricsEntries.add(new SimpleMeasurementMetricsEntry("NumOverflowed", stats.getOverflowedCount()));
        metricsEntries.add(new SimpleMeasurementMetricsEntry("NumFailed", stats.getFailedCount()));
        metricsEntries.add(new SimpleMeasurementMetricsEntry("TotalEvents", stats.getProcessedCount()));
        metricsEntries.add(new SimpleMeasurementMetricsEntry("QueueLargest", stats.getQueueLargestCount()));
        
        return metricsEntries;
    }
}
