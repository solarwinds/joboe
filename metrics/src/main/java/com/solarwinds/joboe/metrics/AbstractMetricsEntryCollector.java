package com.solarwinds.joboe.metrics;

import java.util.List;

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.core.metrics.MetricsEntry;

/**
 * Sub metrics collector used by {@link MetricsCollector}
 * @author pluk
 *
 */
abstract class AbstractMetricsEntryCollector {
    protected static Logger logger = LoggerFactory.getLogger();
    abstract List<? extends MetricsEntry<?>> collectMetricsEntries() throws Exception;
}
