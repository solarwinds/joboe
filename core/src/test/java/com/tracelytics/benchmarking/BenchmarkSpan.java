package com.tracelytics.benchmarking;

public class BenchmarkSpan {
    private String key;
    private final long startTime;
    private long endTime;

    public BenchmarkSpan(String key) {
        this.key = key;
        this.startTime = System.nanoTime();
    }

    public long stop() {
        if (key != null) {
            endTime = System.nanoTime();
            long duration = endTime - startTime;
            BenchmarkManager.record(key, duration);
            return duration;
        } else {
            System.err.println("Key is null, skippin benchmark span");
            return 0;
        }
    }

    public long stop(String key) {
        this.key = key;
        return stop();
    }
    public static final BenchmarkSpan start() {
        return start(null);
    }

    public static final BenchmarkSpan start(String key) {
        return new BenchmarkSpan(key);
    }


}
