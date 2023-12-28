package com.solarwinds.joboe.metrics;

import java.util.Map;

import com.solarwinds.joboe.core.logging.Logger;
import com.solarwinds.joboe.core.logging.LoggerFactory;

/**
 * Collects information and returns as maps. Take note that this collector only collects data. 
 * How the collected data are to be reported is defined by the {@link SystemReporter}
 *
 * @param <T>   Type of the collected data map Key
 * @param <D>   Type of the collected data map Value
 * @author Patson Luk
 *
 */
public abstract class SystemCollector<T, D> {
    protected static Logger logger = LoggerFactory.getLogger();

    /**
     * Collects information and return via a Map. Take note that based on the concrete implementation, this method might only return part of the data within a collection cycle
     * 
     * @return              a Map with data collected.
     * @throws Exception
     * @see                 {@link SystemMonitor#hasMoreData()}
     */
    protected abstract Map<T, D> collect() throws Exception;
}
