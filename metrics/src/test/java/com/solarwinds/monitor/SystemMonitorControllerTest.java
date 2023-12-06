package com.solarwinds.monitor;

import com.solarwinds.joboe.EventImpl;
import com.solarwinds.joboe.EventReporter;
import com.solarwinds.joboe.TestReporter;
import com.solarwinds.util.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SystemMonitorControllerTest {
    private static final TestReporter tracingReporter = TestUtils.initTraceReporter();
    private EventReporter originalReporter;

    @BeforeEach
    protected void setUp() throws Exception {
        originalReporter = EventImpl.setDefaultReporter(tracingReporter);
    }

    @AfterEach
    protected void tearDown() throws Exception {
        EventImpl.setDefaultReporter(originalReporter);
    }

    @Test
    public void testStart() throws InterruptedException {
        SystemMonitorController.start();
        SystemMonitorController.stop();       
    }
}
