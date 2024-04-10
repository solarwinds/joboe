package com.solarwinds.joboe.metrics;


import com.solarwinds.joboe.core.metrics.MetricsEntry;
import com.solarwinds.joboe.core.MetricSpanReporter;
import com.solarwinds.joboe.core.metrics.TopLevelMetricsEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sub metrics collector that collects span metrics (span histograms and measurements)
 * @author pluk
 *
 */
public class SpanMetricsCollector extends AbstractMetricsEntryCollector {
    public static final String TRANSACTION_NAME_OVERFLOW_LABEL = "TransactionNameOverflow";

    private final Set<MetricSpanReporter> registeredReporters = new HashSet<MetricSpanReporter>();

    private MetricFlushListener metricFlushListener = () -> {};

    private final TransactionNameOverFlowSupplier transactionNameOverFlowSupplier;

    public SpanMetricsCollector(TransactionNameOverFlowSupplier transactionNameOverFlowSupplier, MetricSpanReporter... reporters) {
        this.transactionNameOverFlowSupplier = transactionNameOverFlowSupplier == null ? () -> false : transactionNameOverFlowSupplier;
        registeredReporters.addAll(Arrays.asList(reporters));
    }
    
    @Override
    public List<? extends MetricsEntry<?>> collectMetricsEntries() {
        return collectMetricsEntries(registeredReporters);
    }
    
    List<? extends MetricsEntry<?>> collectMetricsEntries(Set<MetricSpanReporter> reporters) {
        List<MetricsEntry<?>> entries = new ArrayList<MetricsEntry<?>>();
        for (MetricSpanReporter spanReporter : reporters) {
            entries.addAll(spanReporter.consumeMetricEntries());
        }
        
        //add an extra entry if TransactionName limit was reached
        if (transactionNameOverFlowSupplier.isLimitExceeded()) {
            entries.add(new TopLevelMetricsEntry<Boolean>(TRANSACTION_NAME_OVERFLOW_LABEL, true));
        }
        
        metricFlushListener.onFlush();
        return entries;
    }

    public void setMetricFlushListener(MetricFlushListener metricFlushListener) {
        this.metricFlushListener = metricFlushListener;
    }
}
