package com.solarwinds.monitor;

import java.util.ArrayList;
import java.util.List;

import com.solarwinds.joboe.EventImpl;
import com.solarwinds.joboe.EventReporterStats;
import com.solarwinds.metrics.measurement.SimpleMeasurementMetricsEntry;

/**
 * Sub metrics collector that collects metrics specific to the Tracing event reporter such as number of tracing events sent, the largest queue size since last collection etc
 * @author pluk
 *
 */
class TracingReporterMetricsCollector extends AbstractMetricsEntryCollector {
    @Override
    List<SimpleMeasurementMetricsEntry> collectMetricsEntries() throws Exception {
        EventReporterStats stats = EventImpl.getEventReporter().consumeStats();
        
        List<SimpleMeasurementMetricsEntry> metricsEntries = new ArrayList<SimpleMeasurementMetricsEntry>();
        metricsEntries.add(new SimpleMeasurementMetricsEntry("NumSent", stats.getSentCount()));
        metricsEntries.add(new SimpleMeasurementMetricsEntry("NumOverflowed", stats.getOverflowedCount()));
        metricsEntries.add(new SimpleMeasurementMetricsEntry("NumFailed", stats.getFailedCount()));
        metricsEntries.add(new SimpleMeasurementMetricsEntry("TotalEvents", stats.getProcessedCount()));
        metricsEntries.add(new SimpleMeasurementMetricsEntry("QueueLargest", stats.getQueueLargestCount()));
        
        return metricsEntries;
    }
}
