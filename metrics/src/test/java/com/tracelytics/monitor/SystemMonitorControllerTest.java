package com.tracelytics.monitor;

import com.tracelytics.joboe.EventImpl;
import com.tracelytics.joboe.EventReporter;
import com.tracelytics.joboe.TestReporter;
import com.tracelytics.util.TestUtils;
import junit.framework.TestCase;

public class SystemMonitorControllerTest extends TestCase {
    private static final TestReporter tracingReporter = TestUtils.initTraceReporter();
    private EventReporter originalReporter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        originalReporter = EventImpl.setDefaultReporter(tracingReporter);
    }

    @Override
    protected void tearDown() throws Exception {
        EventImpl.setDefaultReporter(originalReporter);
        super.tearDown();
    }

    public void testStart() throws InterruptedException {
        SystemMonitorController.start();
        SystemMonitorController.stop();       
    }
}
