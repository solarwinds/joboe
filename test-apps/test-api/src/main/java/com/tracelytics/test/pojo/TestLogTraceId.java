package com.tracelytics.test.pojo;

import java.util.concurrent.TimeUnit;

import com.appoptics.api.ext.AgentChecker;
import com.appoptics.api.ext.Trace;

public class TestLogTraceId {
    private static final String SPAN_NAME = "test-log-trace-id";

    public static void main(String[] args) throws InterruptedException {
        AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);
        
        System.out.println("Before trace start, full x-trace id: " + Trace.getCurrentXTraceID());
        System.out.println("Before trace start, compact x-trace id: " + Trace.getCurrentLogTraceID());
        
        Trace.startTrace(SPAN_NAME).report();
        
        System.out.println("After trace start, full x-trace id: " + Trace.getCurrentXTraceID());
        System.out.println("After trace start, compact x-trace id: " + Trace.getCurrentLogTraceID());
        
        Trace.endTrace(SPAN_NAME);
        
        System.out.println("After trace end, full x-trace id: " + Trace.getCurrentXTraceID());
        System.out.println("After trace end, compact x-trace id: " + Trace.getCurrentLogTraceID());
    }
}
