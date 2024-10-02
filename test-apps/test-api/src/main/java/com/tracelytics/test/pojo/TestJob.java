package com.tracelytics.test.pojo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.appoptics.api.ext.AgentChecker;
import com.appoptics.api.ext.Trace;
import com.appoptics.api.ext.TraceContext;
import com.appoptics.api.ext.TraceEvent;

public class TestJob {
    public static void main(String[] args) throws InterruptedException {
        AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);
        TraceEvent startTraceEvent = Trace.startTrace("test-job");
        startTraceEvent.addBackTrace();
        startTraceEvent.addInfo("start-key", 123);
        startTraceEvent.report();
        
        Trace.createEntryEvent("test-child").report();
        TraceEvent infoEvent = Trace.createInfoEvent("test-child");
        infoEvent.addBackTrace();
        infoEvent.addInfo("info-key", 0.15);
        infoEvent.report();
        TimeUnit.MILLISECONDS.sleep(100);
        Trace.createExitEvent("test-child").report();
        
        TimeUnit.MILLISECONDS.sleep(100);
        Trace.setTransactionName("test-transaction");
        
        System.out.println("Before exit x-trace ID " + Trace.getCurrentXTraceID());
        
        Map<String, Object> tags = new HashMap<String, Object>();
        
        String xTrace = Trace.endTrace("test-job", tags);
        System.out.println("Exit x-trace ID " + xTrace);
        System.out.println("After exit x-trace ID " + Trace.getCurrentXTraceID());
        
        System.out.println(TraceContext.isSampled(xTrace));
        
        //test a transaction w/o explicit transaction name
        startTraceEvent = Trace.startTrace("test-job-2");
        startTraceEvent.report();
        Trace.createEntryEvent("test-child").report();
        TimeUnit.MILLISECONDS.sleep(100);
        Trace.createExitEvent("test-child").report();
        Trace.endTrace("test-job-2");
        
        //test a transaction with status/method, it should NOT add tags to metrics nor be marked as errored
        startTraceEvent = Trace.startTrace("test-job-3");
        startTraceEvent.report();
        startTraceEvent.addInfo("Status", 500);
        startTraceEvent.addInfo("Method", "FROM-SDK");
        Trace.endTrace("test-job-3");
    }
}
