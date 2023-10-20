package com.tracelytics.monitor.framework;

import com.tracelytics.joboe.TestRpcClient;
import com.tracelytics.monitor.SystemReporterException;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


public class FrameworkInfoReporterTest extends TestCase {

    private FrameworkInfoReporter reporter;
    private TestRpcClient testRpcClient = new TestRpcClient(0);

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        reporter = new FrameworkInfoReporter(testRpcClient);
    }

    @Override
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
