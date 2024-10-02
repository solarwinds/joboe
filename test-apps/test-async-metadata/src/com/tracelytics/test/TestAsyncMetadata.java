package com.tracelytics.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import com.appoptics.api.ext.AgentChecker;
import com.appoptics.api.ext.Trace;
import com.appoptics.api.ext.TraceEvent;

public class TestAsyncMetadata {
    private static final int CONCURRENT_COUNT = 5;
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        AgentChecker.waitUntilAgentReady(5, TimeUnit.SECONDS);
        testPlainThread();
        testFutureTask();
        testExecutorService();
    }

    private static void testPlainThread() throws InterruptedException {
        Trace.startTrace("top-layer-thread").report();
        
        List<Thread> threads = new ArrayList<Thread>();
        for (int i = 0 ; i < CONCURRENT_COUNT; i++) {
            Thread thread = new Thread(new TestRunnable());
            threads.add(thread);
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        Trace.setTransactionName("thread-async-metadata-test");
        Trace.endTrace("top-layer-thread");
    }
    
    private static void testFutureTask() throws InterruptedException, ExecutionException {
        Trace.startTrace("top-layer-future-task").report();
        
        List<FutureTask<String>> tasks = new ArrayList<FutureTask<String>>();
        for (int i = 0 ; i < CONCURRENT_COUNT; i++) {
            FutureTask<String> task = new FutureTask<String>(new TestRunnable(), "");
            tasks.add(task);
            new Thread(task).start(); //FutureTask as Thread
        }
        
        ExecutorService service = Executors.newCachedThreadPool();
        for (int i = 0 ; i < CONCURRENT_COUNT; i++) {
            FutureTask<String> task = new FutureTask<String>(new TestRunnable(), "");
            service.submit(task); //FutureTask submitted to executor
        }
        
        for (FutureTask<String> task : tasks) {
            task.get();
        }
        
        service.shutdown();
        
        Trace.setTransactionName("future-task-async-metadata-test");
        Trace.endTrace("top-layer-future-task");
        
    }

    private static void testExecutorService() throws InterruptedException {
        Trace.startTrace("top-layer-executor-service").report();
        Trace.createEntryEvent("layer-0").report();
        Trace.createExitEvent("layer-0").report();
        ExecutorService service = Executors.newCachedThreadPool();
        final Random r = new Random();
        for (int i = 0; i < CONCURRENT_COUNT; i++) {
            service.submit(new Runnable() {
                @Override
                public void run() {

                    try {
                        Thread.sleep(r.nextInt(1000));
                        Trace.createEntryEvent("layer-a").report();
                        Thread.sleep(r.nextInt(1000));
                        Trace.createEntryEvent("layer-b").report();
                        Thread.sleep(r.nextInt(1000));
                        Trace.createExitEvent("layer-b").report();
                        Thread.sleep(r.nextInt(1000));
                        Trace.createExitEvent("layer-a").report();
                        
                        Trace.createEntryEvent("layer-a").report();
                        Thread.sleep(r.nextInt(1000));
                        Trace.createEntryEvent("layer-b").report();
                        Thread.sleep(r.nextInt(1000));
                        Trace.createExitEvent("layer-b").report();
                        Thread.sleep(r.nextInt(1000));
                        Trace.createExitEvent("layer-a").report();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });
        }

        service.shutdown();
        service.awaitTermination(10, TimeUnit.SECONDS);
        Trace.setTransactionName("executor-service-async-metadata-test");
        Trace.endTrace("top-layer-executor-service");
        
    }
    
    static class TestRunnable implements Runnable {
        @Override
        public void run() {
            TraceEvent event = Trace.createEntryEvent("runnable");
            event.addBackTrace();
            event.report();
            
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            Trace.createExitEvent("runnable").report();
        }
    }
}
