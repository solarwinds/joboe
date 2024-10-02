package com.tracelytics.monitor;

import java.util.Map;

import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

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
     * @param collectedData
     * @param the interval for the collected data in millisec
     * 
     * @throws SystemReporterException
     */
    public abstract void reportData(Map<T, D> collectedData, long interval) throws SystemReporterException;


}
