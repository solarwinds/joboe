package com.tracelytics.test.benchmark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

import com.appoptics.api.ext.AgentChecker;
import com.appoptics.api.ext.Trace;
import org.slf4j.MDC;

public class Benchmark {
    private static final int CALL_COUNT = 100000;
    private static final int RUN_COUNT = 1000;
    public static void main(String[] args) throws IOException {
//        Logger noMdc = getLogger("no-mdc", "%msg%n");
//        noMdc.info("abcccc");
//        
//        LogManager.shutdown();
//        
//        Logger mdc = getLogger("mdc", "%X{ao.traceId} - %msg%n");
//        mdc.info("dddddd");
//        
//        
//        
//        
//       
        
        AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);
        
        
         
        Trace.startTrace("with-trace").report();
        List<Long> mdcDurations = runTests("mdc", "%X{ao.traceId}%msg%n");
        List<Long> noMdcDurations = runTests("no-mdc", "%msg%n");
        
        System.out.println(Trace.endTrace("with-trace"));
        
        Statistics<Long> noMdcStats = new Statistics<Long>(noMdcDurations);
        Statistics<Long> mdcStats = new Statistics<Long>(mdcDurations);
        
        System.out.println("No MDC vs MDC");
        System.out.println("Median : " + noMdcStats.getMedian() + " vs " + mdcStats.getMedian());
        System.out.println("Mean : " + noMdcStats.getMean() + " vs " + mdcStats.getMean());
        double diff = mdcStats.getMean() - noMdcStats.getMean();
        long diffPercentage = Math.round(diff * 100 / noMdcStats.getMean());
        System.out.println("Average overhead per call : " + String.format("%.2f", diff) + " nanosecond (~" + diffPercentage  + "%)");
    }
    
    
    private static List<Long> runTests(String name, String pattern) {
        List<Long> durations = new ArrayList<Long>();
        configure(name, pattern);
        
        Logger logger = LogManager.getLogger(name);
        for (int i = 0 ; i < RUN_COUNT ; i ++) {
            
            Long start = System.currentTimeMillis();
            for (int j = 0 ; j < CALL_COUNT; j++) {
                logger.info("a log message here");
            }
            long end = System.currentTimeMillis();
            long duration = end - start;
            durations.add(duration * 1000000 / CALL_COUNT); //duration per call in nanoseconds
            
            if (i % (RUN_COUNT / 100) == 0) {
                System.out.println(i / (RUN_COUNT / 100) + " % " + name + " : " + duration);
            }
            
        }
        LogManager.shutdown();
        System.gc();
        return durations;
    }


    private static void configure(String name, String pattern) {
        ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        AppenderComponentBuilder appenderBuilder = builder.newAppender(name, "FILE");
        appenderBuilder.addAttribute("fileName", name);
        appenderBuilder.addAttribute("append", false);
        appenderBuilder.addAttribute("immediateFlush", false);
        
        appenderBuilder.add(builder.newLayout("PatternLayout").addAttribute("pattern", pattern));
        builder.add(appenderBuilder);
        
        builder.add(builder.newRootLogger(Level.INFO).add(builder.newAppenderRef(name)));
        
        //builder.add(builder.newRootLogger(Level.ERROR).add(builder.newAppenderRef("Stdout")));
        
        Configurator.initialize(builder.build());
    }
    
//    private static Logger getLogger2(String name, String pattern) {
//        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
//        
//        Log
//        Configuration config = ctx.getConfiguration();
//        Layout layout = PatternLayout.newBuilder().withPattern(pattern).build();
//        Appender appender = FileAppender.newBuilder().withFileName(name).withAppend(false).setLayout(layout).build();
//        appender.start();
//        config.addAppender(appender);
//        config.getRootLogger().removeAppender("STDOUT");
//        
//        ctx.updateLoggers();
//        
//        
//    }
    
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

