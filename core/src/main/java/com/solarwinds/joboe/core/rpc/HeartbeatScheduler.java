package com.solarwinds.joboe.core.rpc;

@FunctionalInterface
public interface HeartbeatScheduler {
    void schedule();
}