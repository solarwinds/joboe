package com.solarwinds.joboe.metrics;

import java.util.Map;

import com.solarwinds.joboe.core.logging.Logger;
import com.solarwinds.joboe.core.logging.LoggerFactory;

/**
 * An abstract monitor that collects and reports system information periodically based on the interval provided in the ctor. 
 * <p>
 * Each concrete child monitor provides exactly 1 {@link SystemCollector} and 1 {@link SystemReporter}
 * <p>
 * A Monitor runs cycles as as below:
 * <ol>
 * <li>{@link SystemCollector#startCollection()} is invoked to notify the {@code SystemCollector} that the collection cycle has started</li>
 * <li>{@link SystemReporter#preReportData()} is invoked such that {@code SystemReporter} can perform tasks before data reporting</li>
 * <li>{@link SystemCollector#hasMoreData()} is invoked, if true, keep looping step 4. Otherwise goto step 5</li>
 * <li>{@link SystemCollector#collect()} is invoked, the returned value is then passed to {@link SystemReporter#reportData(Map)}</li>
 * <li>{@link SystemReporter#postReportData()} is invoked such that {@code SystemReporter} can perform tasks after data reporting</li>
 * <li>Sleep for the amount of time defined in {@code interval}</li>
 * <li>Go back to step 1 once the sleep is over</li>
 * </ol>
 * <p>
 * Take note that we want to have the {@link SystemCollector#hasMoreData()} and {@link SystemCollector#collect()} interweave with
 * {@link SystemReporter#reportData(Map)} as this allows:
 * <ul>
 * <li>Stop data collection if the {@code SystemReporter} fails to further reports events in this cycle</li>
 * <li>Operation unit on each collection, for example separate event generated for each {@link SystemCollector#collect()}
 * </ul>
 * <p>
 * This is created and managed by <code>SystemMonitorFactory</code> and <code>SystemMonitorController</code>
 * <p>
 * @param <T>   Type of the collected data map Key
 * @param <D>   Type of the collected data map Value
 * @see SystemCollector
 * @see SystemReporter
 * 
 * @author Patson Luk
 */

public abstract class SystemMonitor<T, D> implements Runnable {
    protected static final Logger logger = LoggerFactory.getLogger();
    
    private boolean stopSignalled = false;
    
    protected final SystemCollector<T, D> collector;
    protected final SystemReporter<T, D> reporter;
    
    public SystemMonitor(SystemCollector<T, D> collector, SystemReporter<T, D> reporter) {
        super();
        this.collector = collector;
        this.reporter = reporter;
    }

    /**
     * Starts the data collection/report cycle
     */
    @Override
    public final void run() {
        try {
            if (collector == null || reporter == null) {
                logger.warn("Cannot find valid collector or reporter for [ " + getClass().getName() + "]. This monitor is skipped ");
                return;
            }
            
            while (!stopSignalled) {
                waitForNextCycle();
                executeCycle();
            }
        } catch (InterruptedException e) {
            logger.debug(getMonitorName() + " interrupted. Message: " + e.getMessage());
        }
    }
    
    /**
     * Monitoring interval in millisec
     * @return
     */
    protected abstract long getInterval();

    protected abstract String getMonitorName() ;
    
    protected void executeCycle() {
        if (reporter == null || collector == null) {
            return ;
        }
        
        logger.trace("Starting reporting cycle of monitor [" + getMonitorName() + "]");
        reporter.preReportData();
            
        try {
            Map<T, D> collectedData = collector.collect();   
        
            if (collectedData != null && !collectedData.isEmpty()) {
                reporter.reportData(collectedData, getInterval()); 
            }
        } catch (SystemReporterException e) {
            logger.warn(e.getMessage()); 
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
            
        reporter.postReportData();
    }
    
    /**
     * Blocks until it's time for next collection/report cycle
     * @throws Exception
     */
    protected abstract void waitForNextCycle() throws InterruptedException;

    public void close() {
        stopSignalled = true;
    }
}

