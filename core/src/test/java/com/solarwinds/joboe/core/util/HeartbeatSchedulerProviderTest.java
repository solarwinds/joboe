package com.solarwinds.joboe.core.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.solarwinds.joboe.core.rpc.KeepAliveMonitor;
import com.solarwinds.joboe.core.rpc.ProtocolClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HeartbeatSchedulerProviderTest {

  @Mock private ProtocolClient protocolClientStub;

  @Test
  void testThatKeepAliveMonitorIsCreatedWhenNotLambda() {
    assertTrue(
        HeartbeatSchedulerProvider.createHeartbeatScheduler(
                () -> protocolClientStub, "some key", "locker")
            instanceof KeepAliveMonitor);
  }
}
