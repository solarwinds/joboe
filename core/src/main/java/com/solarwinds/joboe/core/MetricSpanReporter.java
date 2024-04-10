package com.solarwinds.joboe.core;

import com.solarwinds.joboe.core.metrics.MetricsEntry;

import java.util.List;

/**
 * Records histograms and measurements metrics when span finishes
 * @author pluk
 *
 */
public abstract class MetricSpanReporter {
    public abstract List<MetricsEntry<?>> consumeMetricEntries();
}