package com.solarwinds.joboe.rpc;

@FunctionalInterface
public interface HeartbeatScheduler {
    void schedule();
}