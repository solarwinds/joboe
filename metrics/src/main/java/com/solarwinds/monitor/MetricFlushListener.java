package com.solarwinds.monitor;

@FunctionalInterface
public interface MetricFlushListener {
    void onFlush();
}
