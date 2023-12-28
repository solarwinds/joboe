package com.solarwinds.joboe.metrics;

@FunctionalInterface
public interface MetricFlushListener {
    void onFlush();
}
