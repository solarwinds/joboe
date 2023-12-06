package com.solarwinds.monitor;

import java.util.List;

import com.solarwinds.logging.Logger;
import com.solarwinds.logging.LoggerFactory;
import com.solarwinds.metrics.MetricsEntry;

/**
 * Sub metrics collector used by {@link MetricsCollector}
 * @author pluk
 *
 */
abstract class AbstractMetricsEntryCollector {
    protected static Logger logger = LoggerFactory.getLogger();
    abstract List<? extends MetricsEntry<?>> collectMetricsEntries() throws Exception;
}
