package com.solarwinds.joboe.core.util;

import com.solarwinds.joboe.core.rpc.KeepAliveMonitor;
import com.solarwinds.joboe.core.rpc.ProtocolClient;
import com.solarwinds.joboe.core.util.HeartbeatSchedulerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class HeartbeatSchedulerProviderTest {

    @Mock
    private ProtocolClient protocolClientStub;

    @Test

    void testThatKeepAliveMonitorIsCreatedWhenNotLambda() {
        assertTrue(HeartbeatSchedulerProvider.createHeartbeatScheduler(() -> protocolClientStub, "some key", "locker") instanceof KeepAliveMonitor);
    }
}