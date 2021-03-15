package com.tracelytics.monitor.metrics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tracelytics.joboe.span.impl.InboundMetricMeasurementSpanReporter;
import com.tracelytics.joboe.span.impl.MetricHistogramSpanReporter;
import com.tracelytics.joboe.span.impl.MetricSpanReporter;
import com.tracelytics.joboe.span.impl.TransactionNameManager;
import com.tracelytics.metrics.MetricsEntry;
import com.tracelytics.metrics.TopLevelMetricsEntry;

/**
 * Sub metrics collector that collects span metrics (span histograms and measurements)
 * @author pluk
 *
 */
public class SpanMetricsCollector extends AbstractMetricsEntryCollector {
    public static final String TRANSACTION_NAME_OVERFLOW_LABEL = "TransactionNameOverflow"; 
    private static Set<MetricSpanReporter> registeredReporters = new HashSet<MetricSpanReporter>();

    static {
        registeredReporters.add(InboundMetricMeasurementSpanReporter.REPORTER);
        registeredReporters.add(MetricHistogramSpanReporter.REPORTER);
    }
    
    SpanMetricsCollector() {

    }
    
    @Override
    List<? extends MetricsEntry<?>> collectMetricsEntries() {
        return collectMetricsEntries(registeredReporters);
    }
    
    List<? extends MetricsEntry<?>> collectMetricsEntries(Set<MetricSpanReporter> reporters) {
        List<MetricsEntry<?>> entries = new ArrayList<MetricsEntry<?>>();
        for (MetricSpanReporter spanReporter : reporters) {
            entries.addAll(spanReporter.consumeMetricEntries());
        }
        
        //add an extra entry if TransactionName limit was reached
        if (TransactionNameManager.isLimitExceeded()) {
            entries.add(new TopLevelMetricsEntry<Boolean>(TRANSACTION_NAME_OVERFLOW_LABEL, true));
        }
        
        TransactionNameManager.clearTransactionNames();
        
        return entries;
    }
}
