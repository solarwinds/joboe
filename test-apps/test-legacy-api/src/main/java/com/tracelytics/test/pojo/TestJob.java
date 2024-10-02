package com.tracelytics.test.pojo;

import java.util.concurrent.TimeUnit;

import com.tracelytics.api.ext.Trace;
import com.tracelytics.api.ext.TraceEvent;

public class TestJob {
    public static void main(String[] args) throws InterruptedException {
        Trace.startTrace("dummy").report();
        Trace.endTrace("dummy");
        TimeUnit.SECONDS.sleep(5); //to warm up the agent. Required for legacy agent as AppOptics agent doesn't block and has no default sample rate
        
        
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
        
        
        System.out.println("Before exit x-trace ID " + Trace.getCurrentXTraceID());
        String xTrace = Trace.endTrace("test-job");
        System.out.println("Exit x-trace ID " + xTrace);
        System.out.println("After exit x-trace ID " + Trace.getCurrentXTraceID());
    }
}
