package com.tracelytics.monitor;

import junit.framework.TestCase;

public class SystemMonitorControllerTest extends TestCase {

    public void testStart() throws InterruptedException {
        SystemMonitorController.start();
        SystemMonitorController.stop();       
    }
}
