package com.solarwinds.joboe.metrics;

import java.util.Map;

import com.solarwinds.joboe.core.logging.Logger;
import com.solarwinds.joboe.core.logging.LoggerFactory;

/**
 * Reports data collected by {@link SystemCollector}. Take note that this is not bound to any reporting method. Though the existing implementations all
 * report {@link Event}, it does not have to be always the case.
 * 
 * @param <T>   Type of the collected data map Key
 * @param <D>   Type of the collected data map Value
 * @author Patson Luk
 *
 */
public abstract class SystemReporter<T, D> {
    protected Logger logger = LoggerFactory.getLogger();

    public void preReportData() {}
    public void postReportData() {}
    
    /**
     * Reports data based on the input collectedData
     * @param collectedData data to report
     * @param interval interval for the collected data in milliseconds
     *
     */
    public abstract void reportData(Map<T, D> collectedData, long interval) throws SystemReporterException;


}
