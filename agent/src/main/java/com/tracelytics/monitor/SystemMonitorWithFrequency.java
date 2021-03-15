package com.tracelytics.monitor;

import com.tracelytics.agent.Agent;
import com.tracelytics.joboe.config.InvalidConfigException;

/**
 * Controls reporting in better time precision. 
 * 
 * Instead of sleeping for an arbitrary amount of time in between cycles as in {@link SystemMonitorWithInterval}, 
 * it calculates the exact amount of time required to reach the next "valid mark" in time.
 * 
 * A valid mark is defined by the <code>timeUnit</code> and <code>frequency</code> provided via the constructor. 
 * It is either at the beginning of the <code>timeUnit</code> or whole increments to it calculated by the <code>frequency</code>
 * 
 * For example, 
 * for <code>timeUnit</code> <code>PER_HOUR</code> with frequency 6, it will report at
 * hh:00:00, hh:10:00, hh:20:00, hh:30:00, hh:40:00, hh:50:00 of each hour
 * for <code>timeUnit</code> <code>PER_MINUTE</code> with frequency 2, it will report at
 * hh:mm:00, hh:mm:30 of each minute 
 *
 * Take note that the <code>timeUnit</code> should be divisible by the <code>frequency</code>
 * 
 * @author pluk
 *
 * @param <T>
 * @param <D>
 */
public abstract class SystemMonitorWithFrequency<T, D> extends SystemMonitor<T, D> {
    protected long interval; //in millisec
    private final long timeUnit;
    private long wakeUpTime = 0; //next wakeUpTime to prevent duplicated reporting
    
    /**
     * Constructs a System Monitor with precise timing. Take note that the <code>timeUnit</code> should be divisible by the <code>frequency</code>
     * @param timeUnit                  the unit of time for the frequency
     * @param frequency                 the frequency per timeUnit
     * @throws InvalidConfigException   timeUnit provided is not divisible by the frequency, for example PER_DAY (every 24 hours), is divisible by 6 but not 7 
     */
    public SystemMonitorWithFrequency(TimeUnit timeUnit, int frequency, SystemCollector<T, D> collector, SystemReporter<T, D> reporter) throws InvalidConfigException {
        super(collector, reporter);
        setFrequency(timeUnit, frequency);
        this.timeUnit = timeUnit.duration;
    }
    
    
    @Override
    protected void waitForNextCycle() throws InterruptedException {
        long sleepTime = getSleepTime();
        logger.trace("Waiting for next reporting cycle of [" + getMonitorName() + "]. Sleeping for [" + sleepTime + "] ms");
        Thread.sleep(sleepTime); //wait for next cycle
    }
    
    protected final void setFrequency(TimeUnit timeUnit, int frequency) throws InvalidConfigException {
        try {
            interval = timeUnit.getInterval(frequency);
        } catch (IllegalArgumentException e) {
            throw new InvalidConfigException(e);
        }
    }
    
    /**
     * Sets interval in milliseconds. Interval should be positive and either a divisor of timeUnit value or divisible by timeUnit value, 
     * which the timeUnit is provided at the constructor 
     * 
     * For example, if the timeUnit provided is PER_MINUTE then the timeUnit value is 60 * 1000 (in millisec). Therefore values such as
     * 5000 (5 secs), 15000 (15 secs), 30000 (30 secs) are valid as they are divisor of 60000, also values such as
     * 60000 (1 min), 120000 (2 mins), 600000 (10 mins) are also valid as they are divisible by 60000.
     * 
     * If the interval provided is not valid, an {@link InvalidConfigException} would be thrown
     * 
     * @param interval  intervals in milliseconds
     * @throws InvalidConfigException
     */
    protected void setInterval(long interval) throws InvalidConfigException {
        if (interval > 0 && (interval % timeUnit == 0 || timeUnit % interval == 0)) { //then it's a valid interval
            this.interval = interval;
        } else {
            throw new InvalidConfigException("interval [" + interval + "] should be positive and be either a divisor of " + timeUnit + " or divisible by " + timeUnit);
        }
    }

    protected long getSleepTime() {
        return getSleepTime(Agent.currentTimeStamp() / 1000);
    }
    
    private long getSleepTime(long currentTimeInMillisec) {
        //TODO to support PER_DAY ad PER_WEEK, an adjustment is required a Agent.currentTimeStamp reports epoch time that might not start at the 0th hour of the local date 
        long sleepTime = interval - currentTimeInMillisec % interval; //the remainder of current time modulus by interval gives how much time has passed since last mark, subtract this value from interval gives the time required to reach next mark
        
        if (currentTimeInMillisec + sleepTime == wakeUpTime) { //prevent problem with time drifting or very short operation (less than a millisec)
            sleepTime += interval; //add 1 interval to avoid repeating on same wake up time 
        }
        
        wakeUpTime = currentTimeInMillisec + sleepTime; //update wakeUpTime for check on next call
        
        return sleepTime; 
    }
    
    @Override
    public long getInterval() {
        return interval;
    }

    public enum TimeUnit {
        PER_HOUR(60 * 60 * 1000), PER_MINUTE(60 * 1000), PER_SECOND(1 * 1000);
        
        private final int duration; //in millisec 
        
        private TimeUnit(int duration) {
            this.duration = duration;
        }
        
        /**
         * Gets interval in millisecond. For example on PER_HOUR with frequency 12, interval would be 5 minutes which is 300,000 ms
         * @param frequency
         * @return
         */
        public int getInterval(int frequency) throws IllegalArgumentException {
            if (duration % frequency != 0) {
                throw new IllegalArgumentException("Invalid frequency " + frequency + ". Time unit [" + this + "] is not divisible by the frequency");
            }
            return duration / frequency;
        }
    }
}

