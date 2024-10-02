package com.tracelytics.monitor;

import com.tracelytics.joboe.JoboeTest;
import com.tracelytics.joboe.config.ConfigContainer;

public class SystemMonitorControllerTest extends JoboeTest {

    public void testStart() throws InterruptedException {
        SystemMonitorController.start();
        SystemMonitorController.stop();       
    }
}
