package com.solarwinds.monitor.framework;

import com.solarwinds.joboe.TestRpcClient;
import com.solarwinds.monitor.SystemReporterException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class FrameworkInfoReporterTest {

    private FrameworkInfoReporter reporter;
    private TestRpcClient testRpcClient = new TestRpcClient(0);

    @BeforeEach
    protected void setUp() throws Exception {
        reporter = new FrameworkInfoReporter(testRpcClient);
    }

    @AfterEach
    protected void tearDown() throws Exception {
        testRpcClient.reset();
    }



    public void testReportEmpty() throws SystemReporterException {
        reporter.preReportData();
        reporter.reportData(new HashMap<String, Object>(), 0);
        reporter.postReportData();

        assertTrue(testRpcClient.getPostedStatus().isEmpty());
    }

    public void testReport() throws SystemReporterException {
        Map<String, Object> info = new HashMap<String, Object>();

        info.put("Java.a.Version", "1");
        info.put("Java.b.Version", "");

        reporter.preReportData();
        reporter.reportData(info, 0);
        reporter.postReportData();

        Map<String, Object> sentStatusMessage = testRpcClient.getPostedStatus().get(0);

        assertEquals(true, sentStatusMessage.get("__Init"));

        for (Entry<String, Object> infoEntry : info.entrySet()) {
            assertEquals(infoEntry.getValue(), sentStatusMessage.get(infoEntry.getKey()));
        }


    }
}
