package com.tracelytics.monitor.metrics;

import java.util.*;

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
    private Set<MetricSpanReporter> registeredReporters = new HashSet<MetricSpanReporter>();

    private static final Set<MetricSpanReporter> DEFAULT_REPORTERS = new HashSet<MetricSpanReporter>();
    static {
        DEFAULT_REPORTERS.add(InboundMetricMeasurementSpanReporter.REPORTER);
        DEFAULT_REPORTERS.add(MetricHistogramSpanReporter.REPORTER);
    }
    
    public SpanMetricsCollector(MetricSpanReporter... reporters) {
        if (reporters.length == 0) {
            registeredReporters = DEFAULT_REPORTERS;
        } else {
            registeredReporters.addAll(Arrays.asList(reporters));
        }
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
