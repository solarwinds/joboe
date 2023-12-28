package com.solarwinds.joboe.core.util;

import com.solarwinds.joboe.core.rpc.HeartbeatScheduler;
import com.solarwinds.joboe.core.rpc.KeepAliveMonitor;
import com.solarwinds.joboe.core.rpc.ProtocolClient;

import java.util.function.Supplier;

import static com.solarwinds.joboe.core.util.HostTypeDetector.isLambda;

public class HeartbeatSchedulerProvider {
    public static HeartbeatScheduler createHeartbeatScheduler(Supplier<ProtocolClient> protocolClient, String serviceKey, Object lock) {
        if (isLambda()) {
            return () -> {
            };
        }
        return new KeepAliveMonitor(protocolClient, serviceKey, lock);
    }
}