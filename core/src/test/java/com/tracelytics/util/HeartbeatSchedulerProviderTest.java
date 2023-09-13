package com.tracelytics.util;

import com.tracelytics.joboe.rpc.KeepAliveMonitor;
import com.tracelytics.joboe.rpc.ProtocolClient;
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
    @ClearEnvironmentVariable(key = "LAMBDA_TASK_ROOT")
    @ClearEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_NAME")
    void testThatKeepAliveMonitorIsCreatedWhenNotLambda() {
        assertTrue(HeartbeatSchedulerProvider.createHeartbeatScheduler(protocolClientStub, "some key", "locker") instanceof KeepAliveMonitor);
    }

    @Test
    @SetEnvironmentVariable(key = "LAMBDA_TASK_ROOT", value = "lambda eh!")
    @SetEnvironmentVariable(key = "AWS_LAMBDA_FUNCTION_NAME", value = "lambda Fn eh!")
    void testThatKeepAliveMonitorIsNotCreatedWhenLambda() {
        assertFalse(HeartbeatSchedulerProvider.createHeartbeatScheduler(protocolClientStub, "some key", "locker") instanceof KeepAliveMonitor);
    }
}