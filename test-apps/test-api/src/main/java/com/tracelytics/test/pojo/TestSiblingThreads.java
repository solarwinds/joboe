package com.tracelytics.test.pojo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.appoptics.api.ext.AgentChecker;
import com.appoptics.api.ext.LogMethod;
import com.appoptics.api.ext.Trace;
import com.appoptics.api.ext.TraceContext;

public class TestSiblingThreads {
    
    public static void main(String[] args) throws InterruptedException {
        AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);
        
        ExecutorService service = Executors.newCachedThreadPool();
        
        final Container container = new Container();
        
        service.submit(new Runnable() {
            @Override
            public void run() {
                container.method1();
            }
        });
        
        service.submit(new Runnable() {
            @Override
            public void run() {
                container.method2();
            }
        });
        
        service.submit(new Runnable() {
            @Override
            public void run() {
                container.method3();
            }
        });
        
        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);
    }
    
    private static class Container {
        private TraceContext context;
        private void method1() {
            Trace.startTrace("sibling-threads").report();
            context = TraceContext.getDefault();
            TraceContext.clearDefault();
            try {
                Thread.sleep(1000); //force method2 on a different thread
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        private void method2() {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            context.setAsDefault();
            Trace.createEntryEvent("m2").report();
            Trace.createExitEvent("m2").report();
            TraceContext.clearDefault();
        }
        
        private void method3() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            context.setAsDefault();
            Trace.endTrace("sibling-threads");
        }
    }
}
