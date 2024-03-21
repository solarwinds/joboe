package com.solarwinds.joboe.metrics;

import org.junit.jupiter.api.Test;

public class SystemMonitorControllerTest {
    @Test
    public void testStart() throws InterruptedException {
        SystemMonitorController.start();
        SystemMonitorController.stop();       
    }
}
