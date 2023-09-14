package com.tracelytics.util;

import com.tracelytics.joboe.rpc.HeartbeatScheduler;
import com.tracelytics.joboe.rpc.KeepAliveMonitor;
import com.tracelytics.joboe.rpc.ProtocolClient;

import java.util.function.Supplier;

import static com.tracelytics.util.HostTypeDetector.isLambda;

public class HeartbeatSchedulerProvider {
    public static HeartbeatScheduler createHeartbeatScheduler(Supplier<ProtocolClient> protocolClient, String serviceKey, Object lock) {
        if (isLambda()) {
            return () -> {
            };
        }
        return new KeepAliveMonitor(protocolClient, serviceKey, lock);
    }
}