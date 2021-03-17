package com.tracelytics.monitor;


/**
 * Simply sleeps for the interval defined in the constructor 
 * 
 * @author pluk
 *
 * @param <T>
 * @param <D>
 */
public abstract class SystemMonitorWithInterval<T, D> extends SystemMonitor<T, D> {
    protected static final long DEFAULT_INTERVAL = 30 * 1000; //By default, collect every 30 secs
    private static final long MIN_INTERVAL = 30 * 1000; //30 secs as minimum, avoid excessive polling.  At the moment we don't even store metrics in our backend at any more fine-grained intervals than 30 seconds.
    
    private long interval;
    
    /**
     * 
     * @param interval the polling interval of the data collection. Take note that this is just a rough estimate, as it does not factor in the actual data collection time 
     */
    public SystemMonitorWithInterval(Long interval, SystemCollector<T, D> collector, SystemReporter<T, D> reporter) {
        super(collector, reporter);
        if (interval == null) {
            this.interval = DEFAULT_INTERVAL;
        } else if (interval.compareTo(MIN_INTERVAL) < 0) {
            this.interval = MIN_INTERVAL;
            logger.warn("Interval [" + interval + "] is invalid. Using default [" + MIN_INTERVAL + "] instead");
        } else {
            this.interval = interval;
        }
    }
    
    @Override
    protected void waitForNextCycle() throws InterruptedException {
        long sleepTime = interval;
        logger.trace("Waiting for next reporting cycle of [" + getMonitorName() + "]. Sleeping for [" + sleepTime + "] ms");
        Thread.sleep(interval); //wait for next cycle
    }

    @Override
    public long getInterval() {
        return interval;
    }
    
}

