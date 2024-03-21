package com.solarwinds.joboe.metrics;

import com.solarwinds.joboe.core.metrics.MetricsEntry;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;

import java.util.List;

/**
 * Sub metrics collector used by {@link MetricsCollector}
 * @author pluk
 *
 */
public abstract class AbstractMetricsEntryCollector {
    protected static Logger logger = LoggerFactory.getLogger();

    public abstract List<? extends MetricsEntry<?>> collectMetricsEntries() throws Exception;
}
