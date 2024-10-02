package com.tracelytics.test.benchmark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.appoptics.api.ext.AgentChecker;
import com.appoptics.api.ext.Trace;

public class Benchmark {
    private static final int CALL_COUNT = 100000;
    private static final int RUN_COUNT = 1000;
    public static void main(String[] args) throws IOException {
        AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);
        List<Long> noMdcDurations = new ArrayList<Long>();
        List<Long> mdcDurations = new ArrayList<Long>();
        
        //Trace.startTrace("with-trace").report();
        for (int i = 0 ; i < RUN_COUNT ; i ++) {
            Logger noMdcLogger = Logger.getLogger("no-mdc");
            Logger mdcLogger = Logger.getLogger("mdc");
            Logger.getRootLogger().removeAllAppenders();
            FileAppender noMdcAppender = new FileAppender(new PatternLayout("%m%n"), "no-mdc", false, true, 8 * 1024);
            noMdcLogger.addAppender(noMdcAppender);
            FileAppender mdcAppender = new FileAppender(new PatternLayout("%X{ao.traceId}%m%n"), "mdc", false, true, 8 * 1024);
            mdcLogger.addAppender(mdcAppender);
            
            long noMdcStart = System.currentTimeMillis();
            for (int j = 0 ; j < CALL_COUNT; j++) {
                noMdcLogger.info("a log message here");
            }
            long noMdcEnd = System.currentTimeMillis();
            noMdcAppender.close();
            
            long mdcStart = System.currentTimeMillis();
            for (int j = 0 ; j < CALL_COUNT; j++) {
                mdcLogger.info("a log message here");
            }
            long mdcEnd = System.currentTimeMillis();
            mdcAppender.close();
            
            noMdcLogger.removeAppender(noMdcAppender);
            mdcLogger.removeAppender(mdcAppender);
            
            long noMdcDuration = noMdcEnd - noMdcStart;
            long mdcDuration = mdcEnd - mdcStart;
            noMdcDurations.add(noMdcDuration * 1000000 / CALL_COUNT); //duration per call in nanoseconds
            mdcDurations.add(mdcDuration * 1000000 / CALL_COUNT); //duration per call in nanoseconds
        
            if (i % (RUN_COUNT / 100) == 0) {
                System.out.println(noMdcDuration + " vs " + mdcDuration);
            }
        }
        //System.out.println(Trace.endTrace("with-trace"));
        
        Statistics<Long> noMdcStats = new Statistics<Long>(noMdcDurations);
        Statistics<Long> mdcStats = new Statistics<Long>(mdcDurations);
        
        System.out.println("No MDC vs MDC");
        System.out.println("Median : " + noMdcStats.getMedian() + " vs " + mdcStats.getMedian());
        System.out.println("Mean : " + noMdcStats.getMean() + " vs " + mdcStats.getMean());
        double diff = mdcStats.getMean() - noMdcStats.getMean();
        long diffPercentage = Math.round(diff * 100 / noMdcStats.getMean());
        System.out.println("Average overhead per call : " + String.format("%.2f", diff) + " nanosecond (~" + diffPercentage  + "%)");
         
        
    }
}

class Statistics<T extends Number & Comparable<T>> {
    private List<T> data;
    private int size;   

    public Statistics(T... data) 
    {
        this.data = Arrays.asList(data);
        size = data.length;
    }
    
    public Statistics(List<T> data) 
    {
        this.data = data;
        size = data.size();
    }
    
    public T getSum() {
        Double sum = 0.0;
        for(T a : data) {
            sum += a.doubleValue();
        }
        return (T)sum;
    }
    
    public double getMean()
    {
        return getSum().doubleValue() / size;
    }

    public double getVariance()
    {
        double mean = getMean();
        double temp = 0;
        for(T a : data) {
            double diff = a.doubleValue() - mean;
            temp += diff * diff;
        }
        return temp / size;
    }

    public double getStdDev()
    {
        return Math.sqrt(getVariance());
    }
    
    public Number getMedian() 
    {
       Collections.sort(data);

       if (size % 2 == 0) {
          return (data.get((size / 2) - 1).doubleValue() + data.get(size / 2).doubleValue()) / 2.0;
       } else {
           return data.get(size / 2);
       }
    }
    
    public T getPercentile(double percentage) {
        Collections.sort(data);
        
        int percentileMark = (int) (size * percentage);
        if (percentileMark == size) {
            percentileMark -= 1;
        }
        return data.get(percentileMark);
    }
}

