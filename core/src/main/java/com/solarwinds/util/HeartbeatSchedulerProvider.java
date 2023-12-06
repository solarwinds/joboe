package com.solarwinds.util;

import com.solarwinds.joboe.rpc.HeartbeatScheduler;
import com.solarwinds.joboe.rpc.KeepAliveMonitor;
import com.solarwinds.joboe.rpc.ProtocolClient;

import java.util.function.Supplier;

import static com.solarwinds.util.HostTypeDetector.isLambda;

public class HeartbeatSchedulerProvider {
    public static HeartbeatScheduler createHeartbeatScheduler(Supplier<ProtocolClient> protocolClient, String serviceKey, Object lock) {
        if (isLambda()) {
            return () -> {
            };
        }
        return new KeepAliveMonitor(protocolClient, serviceKey, lock);
    }
}