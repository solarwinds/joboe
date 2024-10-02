package com.tracelytics.test.pojo;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import com.appoptics.api.ext.AgentChecker;
import com.appoptics.api.ext.Trace;

public class TestLog {
    private static Logger logger = LogManager.getLogger(TestLog.class);

    public static void main(String[] args) {
        AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);
        
        System.out.println("Before trace start");
        printThreadContextMethodOutputs();       
        
        Trace.startTrace("test-log").report();
        System.out.println("After trace start");
        printThreadContextMethodOutputs();
        
        logger.info("testing!");

        Trace.endTrace("test-log");
        
        System.out.println("After trace end");
        printThreadContextMethodOutputs();

    }

    private static void printThreadContextMethodOutputs() {
        System.out.println("containsKey " + ThreadContext.containsKey("test-ao"));
        System.out.println("get " + ThreadContext.get("test-ao"));
        System.out.println("getImmutableContext " + ThreadContext.getImmutableContext());
        System.out.println("getContext " + ThreadContext.getContext());
        //System.out.println("getThreadContextMap " + ThreadContext.getThreadContextMap());
        System.out.println("isEmpty " + ThreadContext.isEmpty());
        
    }

}
