package com.tracelytics.monitor.metrics;

import java.util.List;

import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.metrics.MetricsEntry;

/**
 * Sub metrics collector used by {@link MetricsCollector}
 * @author pluk
 *
 */
abstract class AbstractMetricsEntryCollector {
    protected static Logger logger = LoggerFactory.getLogger();
    abstract List<? extends MetricsEntry<?>> collectMetricsEntries() throws Exception;
}
