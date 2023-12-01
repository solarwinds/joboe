package com.tracelytics.benchmarking;

import com.tracelytics.metrics.hdrHistogram.Histogram;
import com.tracelytics.util.DaemonThreadFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class BenchmarkManager {
    private static ExecutorService service = Executors.newSingleThreadExecutor(DaemonThreadFactory.newInstance("benchmarking"));
    static {
        start();
    }

    private static ConcurrentHashMap<String, Histogram> histograms = new ConcurrentHashMap<String, Histogram>();

    private static final int REPORT_INTERVAL = 10;
    private static final void start() {
        service.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        TimeUnit.SECONDS.sleep(REPORT_INTERVAL);
                        reportHistograms();
                    }
                } catch (InterruptedException e) {
                    //ok
                }
            }
        });
    }

    private static void reportHistograms() {
        for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
            String key = entry.getKey();
            Histogram histogram = entry.getValue();

            System.out.println("========" + key + "========");
            printHistogram(histogram);
        }

    }

    private static void printHistogram(Histogram histogram) {
        System.out.println("Count  " + histogram.getTotalCount());
        System.out.println("Mean   " + histogram.getMean());
        System.out.println("Median " + histogram.getValueAtPercentile(0.5));
        System.out.println("P99    " + histogram.getValueAtPercentile(0.99));
        System.out.println("P90    " + histogram.getValueAtPercentile(0.9));
        System.out.println("Max    " + histogram.getMaxValue());
        System.out.println("Min    " + histogram.getMinValue());
    }


    public static final void record(String key, long value) {
        Histogram histogram = histograms.computeIfAbsent(key, new Function<String, Histogram>() {
            @Override
            public Histogram apply(String t) {
                return new Histogram(3);
            }
        });
        histogram.recordValue(value);
    }


}
