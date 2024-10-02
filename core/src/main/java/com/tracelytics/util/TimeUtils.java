package com.tracelytics.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

/**
 *  Utils that provide timestamp in microsecond precision since the epoch time
 *  
 *  In this static initializer, we take the current millisec as the "base" time (times 1000 so we get the "base" time in micro second unit)
 *  and take the current nanotime as the "reference" point (as zero nanosecs from the "base" time). 
 *  Take note that this is what the winoboe has essentially been doing to get microsec precision timestamps on windows
 *
 *  When a timestamp is requested, we take the current nanotime minus the "reference" point, which gives us the "offset" from the "base" time. 
 *  The offset is then added to the "base" time which finally returns the timestamp in microsecond.
 * 
 *  This Utils uses a background worker to adjust the base time periodically to address Time shift and drifting problem
 * 
 *  More information in https://github.com/librato/joboe/issues/537
 * 
 * Auto-adjust the 
 * @author pluk
 *
 */
public class TimeUtils {
    private static final Logger logger = LoggerFactory.getLogger();
    private static long reference; //reference in nanosecond
    private static long base; //base in microsecond
//    private static ReadWriteLock readWriteLock = new ReentrantReadWriteLock(); removed locks as even if they are concurrency issue it will be at worse be off by the offset for a few operations, not a big deal
    
    private static final int DEFAULT_TIME_ADJUST_INTERVAL = 600; //in terms of sec
    private static final int MIN_TIME_ADJUST_INTERVAL = 10; // in terms of sec
    private static final int FLIP_SAMPLE_COUNT = 50;
    private static final int BAD_TIMESTAMP_THRESHOLD = 10; //how many bad timestamps before aborting a time adjust operation
    
    static {
        base = System.currentTimeMillis() * 1000;
        reference = System.nanoTime(); //reference in nano second, do NOT convert this to microsecond here to account for the possibility of numerical overflow

        startAdjustBaseWorker((Integer) ConfigManager.getConfig(ConfigProperty.AGENT_TIME_ADJUST_INTERVAL));
    }
    
    public static long getTimestampMicroSeconds() {
        long currentNano = System.nanoTime();
        long offsetMicro = (currentNano - reference) / 1000; //should work properly even if there's numerical overflow
        
        //readWriteLock.readLock().lock();
        long result = base + offsetMicro;
        //readWriteLock.readLock().unlock();
        return result;
    }
    
    /**
     * Starts background working to periodically adjust the base time to address Time drifting and shift problem as documented in
     * https://github.com/librato/joboe/issues/537
     * @param paramValue
     * @return
     */
    private static boolean startAdjustBaseWorker(Integer paramValue) {
        final int timeAdjustInterval;
        if (paramValue != null) {
            if (!(paramValue >= MIN_TIME_ADJUST_INTERVAL || paramValue == 0)) {
                logger.warn(ConfigProperty.AGENT_TIME_ADJUST_INTERVAL.getConfigFileKey() + " should be 0 (disabled) or positive integer value (seconds) >= " + MIN_TIME_ADJUST_INTERVAL +  ", but found [" + paramValue + "]. Disabling time adjustment");
                timeAdjustInterval = 0;
            } else {
                timeAdjustInterval = paramValue;
            }
        } else {
            timeAdjustInterval = DEFAULT_TIME_ADJUST_INTERVAL;
        }
        
        if (timeAdjustInterval > 0) {
//            executorService.scheduleAtFixedRate(new Runnable() {
//                public void run() {
//                    adjustBase();
//                }
//            }, 0, timeAdjustInterval, TimeUnit.SECONDS);
            //using simple thread instead, see deadlock problem as documented in https://github.com/librato/joboe/issues/608
            DaemonThreadFactory.newInstance("time-adjustment").newThread(new Runnable() {
                public void run() {
                    try {
                        while (true) {
                            adjustBase();
                            TimeUnit.SECONDS.sleep(timeAdjustInterval);
                        }
                    } catch (InterruptedException e) {
                        logger.debug("Adjust time worker thread interrupted, probably due to shutdown : " + e.getMessage());
                    }
                }
            }).start();
            
            return true;
        } else {
            return false;
        }
    }

    /**
     * Adjusts the base time
     */
    private static void adjustBase() {
        List<Long> dataSystem = new ArrayList<Long>();
        List<Long> badDiffs = new ArrayList<Long>();

        Statistics<Long> statsSystem;
        long newAdjustment;
        
      //take note of the currentTimeMillis "flip" time and set the micro second to align that as 0th micro second
        long lastMillisec = System.currentTimeMillis();

        while (dataSystem.size() < FLIP_SAMPLE_COUNT) {
            long systemTime = System.currentTimeMillis();
            long diff = systemTime - lastMillisec;

            if (diff == 1) { //if time just flip
                long microTime = getTimestampMicroSeconds();
                long diffSystem = microTime - systemTime * 1000;
                dataSystem.add(diffSystem);
                lastMillisec = systemTime;
            } else if (diff > 1) { //busy system? not accurate
                lastMillisec = systemTime;
                badDiffs.add(diff);
                if (badDiffs.size() >= BAD_TIMESTAMP_THRESHOLD) { //avoid endless looping if a system never manage to get 2 timestamps within a millisecond
                    logger.debug("Aborting current time adjustment as system appears to be busy. Will try again in next cycle. Bad diffs: " + badDiffs);
                    return;
                }
            } else if (diff < 0) { //something goes terribly wrong, perhaps clock reset, do not keep trying
                return;
            }
        }
        
        statsSystem = new Statistics<Long>(dataSystem);
        newAdjustment = statsSystem.getMedian().longValue();

        base -= newAdjustment;
    }

    public static class Statistics<T extends Number & Comparable<T>> {
        private List<T> data;
        private int size;
        private boolean sorted = false;

        public Statistics(T... data) {
            this.data = Arrays.asList(data);
            size = data.length;
        }

        public Statistics(List<T> data) {
            this.data = data;
            size = data.size();
        }

        public T getSum() {
            Double sum = 0.0;
            for (T a : data) {
                sum += a.doubleValue();
            }
            return (T) sum;
        }

        public double getMean() {
            return getSum().doubleValue() / size;
        }

        public double getVariance() {
            double mean = getMean();
            double temp = 0;
            for (T a : data) {
                double diff = a.doubleValue() - mean;
                temp += diff * diff;
            }
            return temp / size;
        }

        public double getStdDev() {
            return Math.sqrt(getVariance());
        }

        public Number getMedian() {
            sortIfNotSorted();
            if (size % 2 == 0) {
                return (data.get((size / 2) - 1).doubleValue() + data.get(size / 2).doubleValue()) / 2.0;
            } else {
                return data.get(size / 2);
            }
        }

        public T getPercentile(double fraction) {
            sortIfNotSorted();

            int percentileMark = (int) (size * fraction);
            if (percentileMark == size) {
                percentileMark -= 1;
            }
            return data.get(percentileMark);
        }

        public T getMax() {
            sortIfNotSorted();
            return data.get(data.size() - 1); 
        }
        
        public T getMin() {
            sortIfNotSorted();
            return data.get(0); 
        }

        private synchronized void sortIfNotSorted() {
            if (!sorted) {
                Collections.sort(data);
                sorted = true;
            }
        }

        @Override
        public String toString() {
            return "Statistics{" +
                    "min=" + getMin() +
                    ", max=" + getMax() +
                    ", mean=" + getMean() +
                    ", median=" + getPercentile(0.5) +
                    ", p90=" + getPercentile(0.9) +
                    ", p99=" + getPercentile(0.99) +
                    ", sample size=" + data.size() +
                    '}';
        }
    }
}
