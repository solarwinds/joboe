package com.tracelytics.test.pojo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.appoptics.api.ext.AgentChecker;
import com.appoptics.api.ext.Trace;

public class TestProfiler {
    public static void main(String[] args) throws InterruptedException {
        AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);
        
        Trace.startTrace("test-recursive-call").report();
        testRecursiveCall(0);
        Trace.endTrace("test-recursive-call");
        
        Trace.startTrace("test-thread-pool").report();
        testThreadPool();
        Trace.endTrace("test-thread-pool");
    }
    
    private static void testRecursiveCall(int count) throws InterruptedException {
        if (count <= 10) {
            testRecursiveCall(count + 1);
        }
        TimeUnit.MILLISECONDS.sleep(100);
    }
    
    private static void testThreadPool() throws InterruptedException {
        ExecutorService service = Executors.newCachedThreadPool();
        
        for (int i = 0; i < 5; i ++) {
            service.submit(() -> testMethod());
        }
        
        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);
    }
    
    private static void testMethod() {
        try {
            long sleepTime = (long)(Math.random() * 1000);
            System.out.println("thread  " + Thread.currentThread().getId() + " sleeping for " + sleepTime +  "  ms");
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
