package com.solarwinds.joboe.core.util;

import com.solarwinds.joboe.core.rpc.HeartbeatScheduler;
import com.solarwinds.joboe.core.rpc.KeepAliveMonitor;
import com.solarwinds.joboe.core.rpc.ProtocolClient;

import java.util.function.Supplier;

public class HeartbeatSchedulerProvider {
    public static HeartbeatScheduler createHeartbeatScheduler(Supplier<ProtocolClient> protocolClient, String serviceKey, Object lock) {
        return new KeepAliveMonitor(protocolClient, serviceKey, lock);
    }
}