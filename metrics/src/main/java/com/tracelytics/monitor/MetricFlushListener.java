package com.tracelytics.monitor.metrics;

@FunctionalInterface
public interface MetricFlushListener {
    void onFlush();
}
