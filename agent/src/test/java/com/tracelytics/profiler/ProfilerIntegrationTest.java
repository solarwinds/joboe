package com.tracelytics.profiler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tracelytics.joboe.config.ProfilerSetting;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.tracelytics.AnyValueValidator;
import com.tracelytics.ExpectedEvent;
import com.tracelytics.ext.ebson.BsonDocument;
import com.tracelytics.AbstractEventBasedTest;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.TestReporter.DeserializedEvent;


public class ProfilerIntegrationTest extends AbstractEventBasedTest {
    private MockHttpServletRequest requestWithNoHeader;
    
    private HttpServlet servlet;

    private String MOCKED_URL = "/test/1/2/3";
    private String MOCKED_HOST = "localhost:8080";
    private String MOCKED_METHOD = "GET";
    private static final int MOCKED_STATUS_CODE = 200;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        //small pause to ensure profiler is running
        TimeUnit.SECONDS.sleep(1);
    }
    
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
        
    public ProfilerIntegrationTest() throws Exception {
        requestWithNoHeader = new MockHttpServletRequest(null, MOCKED_METHOD, MOCKED_URL);
        
        requestWithNoHeader.addHeader("host", MOCKED_HOST);
        
        servlet = new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            
        };
    }
    
    private static javax.servlet.http.HttpServletResponse getResponse() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(MOCKED_STATUS_CODE);
        
        return response;
    }
    
    
    public void testServiceAlwaysInstrumentation() throws Exception {
        Context.clearMetadata(); //force a new trace
        
        testSettingsReader.put(new TestDefaultSettings());
        
        servlet.service(requestWithNoHeader, getResponse());
        
        ExpectedEvent expectedProfilingEntry = new ExpectedEvent(
                "Label", "entry", 
                "Spec", "profiling",
                "SpanRef", AnyValueValidator.INSTANCE,
                "Language", "java",
                "Interval", ProfilerSetting.DEFAULT_INTERVAL,
                "Timestamp_u",  AnyValueValidator.INSTANCE,
                "Edge", null); //profiling entry should not have an edge
        
        
        List<DeserializedEvent> sentEvents = profilingReporter.getSentEvents();
        
        List<DeserializedEvent> profilingEvents = new ArrayList<DeserializedEvent>();
        for (DeserializedEvent sentEvent : sentEvents) {
            if ("profiling".equals(sentEvent.getSentEntries().get("Spec"))) {
                profilingEvents.add(sentEvent);
            }
        }
        
        //it should have roughly 2 + ProfilerSetting.DEFAULT_INTERVAL events (at least 1)
        assert(profilingEvents.size() >= 3);
        
        DeserializedEvent entryEvent = profilingEvents.get(0);
        assertEvent(expectedProfilingEntry, entryEvent);
        
        long threadId = (Long) entryEvent.getSentEntries().get("TID");
        
        ExpectedEvent expectedSnapshot = new ExpectedEvent(
                "Label", "info",
                "Spec", "profiling",
                "TID",  threadId,
                "FramesCount", AnyValueValidator.INSTANCE,
                "Timestamp_u", AnyValueValidator.INSTANCE);
            
        int threadSleepSnapshotIndex = -1;
        boolean hasHighSnapshotsOmitted = false; //should have high value for FramesOmitted either in snapshot or exit right after Thread.sleep
        for (int i = 1; i < profilingEvents.size() - 1; i++) { 
            DeserializedEvent snapshotEvent = profilingEvents.get(i);
            assertEvent(expectedSnapshot, snapshotEvent);
            
//            Map<String, String>[] newFrames = (Map<String, String>[])snapshotEvent.getSentEntries().get("NewFrames");
            BsonDocument newFrames = (BsonDocument) snapshotEvent.getSentEntries().get("NewFrames");
            if (newFrames != null && newFrames.entrySet().size() > 0) {
                BsonDocument topFrame = (BsonDocument) newFrames.entrySet().iterator().next().getValue();
                if ("java.lang.Thread".equals(topFrame.get("C")) &&
                    "sleep".equals(topFrame.get("M"))) {
                    threadSleepSnapshotIndex = i;
                }
            }
            
            if (i == threadSleepSnapshotIndex + 1) { //then this snapshot event right after the Thread.sleep should have high # of omitted snapshots
                int snapshotOmittedSize = ((BsonDocument) snapshotEvent.getSentEntries().get("SnapshotsOmitted")).size();
                hasHighSnapshotsOmitted = snapshotOmittedSize > ((1000 / ProfilerSetting.DEFAULT_INTERVAL) / 2); //it sleeps for 1 sec, so conservatively it should be at least half of the 1 sec / interval
            }
//            for (Entry<String, Object> entry : newFrames.entrySet()) {
//                System.out.println("new frame entry: " + entry.getKey() + " - " + entry.getValue());
//            }
//            System.out.println("new frames: " + (newFrames != null ? newFrames.getClass().getName() : " null "));
            //if (newFrames != null && newFrames.
        }
        
        ExpectedEvent expectedProfilingExit = new ExpectedEvent(
                "Label", "exit",
                "Spec", "profiling",
                "Timestamp_u",  AnyValueValidator.INSTANCE);
        
        DeserializedEvent exitEvent = profilingEvents.get(profilingEvents.size() - 1);
        assertEvent(expectedProfilingExit, exitEvent);
        
        //the SnapshotsOmitted might be in span exit (instead of a snapshot event)
        if (threadSleepSnapshotIndex == profilingEvents.size() - 2) {
            BsonDocument snapshotsOmittedTimestamps = (BsonDocument) exitEvent.getSentEntries().get("SnapshotsOmitted");
            if (snapshotsOmittedTimestamps != null) {
                hasHighSnapshotsOmitted = snapshotsOmittedTimestamps.size() > ((1000 / ProfilerSetting.DEFAULT_INTERVAL) / 2); //it sleeps for 1 sec, so conservatively it should be at least half of the 1 sec / interval
            }
        }
        
        assert(hasHighSnapshotsOmitted);
        
        
        List<DeserializedEvent> tracingEvents = tracingReporter.getSentEvents();
        DeserializedEvent tracingExitEvent = tracingReporter.getSentEvents().get(tracingEvents.size() - 1);
        for (DeserializedEvent event : sentEvents) {
            if ("exit".equals(event.getSentEntries().get("Label")) && !"profiling".equals(event.getSentEntries().get("Spec"))) {
                tracingExitEvent = event;
                break;
            }
        }
        
        assertEquals(1, tracingExitEvent.getSentEntries().get("ProfileSpans"));
        assertEquals("RUNNING", tracingExitEvent.getSentEntries().get("ProfilerStatus"));
    }
}
