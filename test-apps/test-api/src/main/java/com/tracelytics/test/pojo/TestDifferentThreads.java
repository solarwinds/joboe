package com.tracelytics.test.pojo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.appoptics.api.ext.AgentChecker;
import com.appoptics.api.ext.LogMethod;
import com.appoptics.api.ext.Trace;

public class TestDifferentThreads {
    private static final int RUN_COUNT = 10;
    
    public static void main(String[] args) throws InterruptedException {
        AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);
        
        Trace.startTrace("test-thread").report();

        final AtomicInteger counter = new AtomicInteger();
        ExecutorService service = Executors.newCachedThreadPool();
        
        for (int i = 0 ; i < RUN_COUNT; i ++) {
            service.submit(new Runnable() {
                public void run() {
                    try {
                        testMethod();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    if (counter.addAndGet(1) == RUN_COUNT) { //last one, close end the trace
                        Trace.endTrace("test-thread");
                    }
                }
            });
        }
        
        
        
        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);
    }
    
    @LogMethod
    public static void testMethod() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep((long) (Math.random() * 1000));
    }
}
