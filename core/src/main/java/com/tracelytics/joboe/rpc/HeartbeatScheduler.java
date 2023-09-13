package com.tracelytics.joboe.rpc;

@FunctionalInterface
public interface HeartbeatScheduler {
    void schedule();
}