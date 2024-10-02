package com.tracelytics.test.pojo;

import com.appoptics.api.ext.AgentChecker;
import com.appoptics.api.ext.Trace;
import com.appoptics.api.ext.TraceContext;

import java.util.concurrent.TimeUnit;

public class TestTraceContext {
    public static void main(String[] args) {
        AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);

        Trace.startTrace("t1").report();
        TraceContext traceContext = TraceContext.getDefault();
        TraceContext.clearDefault();
        Trace.createEntryEvent("not-reported").report();
        Trace.createExitEvent("not-reported").report();
        traceContext.setAsDefault();
        System.out.println("t1 exit " + Trace.endTrace("t1"));


        Trace.startTrace("t2").report();
        Trace.createEntryEvent("child").report();
        traceContext = TraceContext.getDefault();
        TraceContext.clearDefault();
        Trace.createEntryEvent("not-reported").report();
        Trace.createExitEvent("not-reported").report();
        traceContext.setAsDefault(); //test getDefault/setAsDefault when active span is not root
        Trace.createExitEvent("child").report();
        traceContext.setAsDefault();
        System.out.println("t2 exit " + Trace.endTrace("t2"));


        Trace.startTrace("t3").report();
        TraceContext t3Context = TraceContext.getDefault();
        TraceContext.clearDefault();
        Trace.startTrace("t4").report();
        TraceContext t4Context = TraceContext.getDefault();

        t3Context.setAsDefault();
        System.out.println("t3 exit " + Trace.endTrace("t3"));
        t4Context.setAsDefault();
        System.out.println("t4 exit " + Trace.endTrace("t4"));
    }

}
