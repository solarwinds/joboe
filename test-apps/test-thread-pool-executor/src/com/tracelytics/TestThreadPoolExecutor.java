package com.tracelytics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.appoptics.api.ext.AgentChecker;
import com.appoptics.api.ext.Trace;



public class TestThreadPoolExecutor {
    public static void main(String[] args) throws IOException, InterruptedException {
        AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS);
        
        runTestOnService(Executors.newCachedThreadPool(), "cached-thread-pool", Validation.PROPAGATION, Validation.NO_LEAK);
        runTestOnService(Executors.newFixedThreadPool(10), "fixed-thread-pool", Validation.PROPAGATION, Validation.NO_LEAK);
        runTestOnService(Executors.newSingleThreadExecutor(), "single-thread-thread-pool", Validation.PROPAGATION, Validation.NO_LEAK);
        runTestOnService(Executors.newScheduledThreadPool(10), "scheduled-thread-pool", Validation.NO_LEAK); // known that no context is propagated for ScheduleThreadPool, only check for leaks
        runTestOnService(Executors.newSingleThreadScheduledExecutor(), "single-thread-scheduled-thread-pool", Validation.NO_LEAK); // known that no context is propagated for ScheduleThreadPool, only check for leaks
    }
    
    public enum Validation { PROPAGATION, NO_LEAK }
    
    public static void runTestOnService(ExecutorService executor, String prefix, Validation...validationsArray) throws IOException, InterruptedException {
        List<Validation> validations = Arrays.asList(validationsArray);
        System.out.println("Starting test run on executor " + executor);
        Runnable runnable;
        Callable<Boolean> callable;
        String validationTraceId;
        List<Callable<Boolean>> callables;

        String spanName = "trace-a-" + prefix;
        Trace.startTrace(spanName).report();
        System.out.println("Submitting jobs for trace-a" + Trace.getCurrentXTraceID());
        
        if (validations.contains(Validation.PROPAGATION)) {
            validationTraceId = Trace.getCurrentXTraceID();
        } else {
            validationTraceId = null;
        }
        
        runnable = new RunnableWithTraceId(validationTraceId);
        callable = new CallableWithTraceId(validationTraceId);
        callables = new ArrayList<Callable<Boolean>>();
        
        for (int i = 0; i < 100; i ++) { //now it should reuse all the threads
            executor.submit(runnable);
            callables.add(callable);
        }
        executor.invokeAll(callables);
        Trace.endTrace(spanName);

        spanName = "trace-b-" + prefix;
        Trace.startTrace(spanName).report();
        System.out.println("Submitting jobs for trace-b" + Trace.getCurrentXTraceID());
        
        if (validations.contains(Validation.PROPAGATION)) {
            validationTraceId = Trace.getCurrentXTraceID();
        } else {
            validationTraceId = null;
        }
        
        runnable = new RunnableWithTraceId(validationTraceId);
        callable = new CallableWithTraceId(validationTraceId);
        callables = new ArrayList<Callable<Boolean>>();
        
        for (int i = 0; i < 100; i ++) { //now it should reuse all the threads
            executor.submit(runnable);
            callables.add(callable);
        }
        executor.invokeAll(callables);
        Trace.endTrace(spanName);
        
        System.out.println("Submitting jobs for no trace " + Trace.getCurrentXTraceID());
        //now check for leaks
        if (validations.contains(Validation.NO_LEAK)) {
            validationTraceId = Trace.getCurrentXTraceID(); //should be 00000000
            if (!validationTraceId.startsWith("2B00000000")) {
                System.err.println("ERROR!!!! expect empty traceid but found " + validationTraceId);
                return;
            }
        } else {
            validationTraceId = null;
        }
        
        runnable = new RunnableWithTraceId(validationTraceId);
        callable = new CallableWithTraceId(validationTraceId);
        callables = new ArrayList<Callable<Boolean>>();
        for (int i = 0; i < 100; i ++) { //now it should reuse all the threads
            executor.submit(runnable);
            callables.add(callable);
        }
        executor.invokeAll(callables);
        
        executor.shutdown();
        executor.awaitTermination(100, TimeUnit.SECONDS);
    }
    
    private static class RunnableWithTraceId implements Runnable {
        private Random random = new Random();
        private String validationTraceId;
        /**
         * Trace id to be checked against, if no traceId check is to be performed, use null
         * @param traceId
         */
        public RunnableWithTraceId(String traceId) {
            this.validationTraceId = traceId;
        }
        @Override
        public void run() {
            try {
                Trace.createEntryEvent("my-runnable").report();
                try {
                    Thread.sleep(random.nextInt(10) + 1);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Trace.createExitEvent("my-runnable").report();
                
                String compareToTraceId = Trace.getCurrentXTraceID();
                
                if (validationTraceId != null) {
                    boolean isSameTask = isSameTask(validationTraceId, compareToTraceId);
                    if (!isSameTask) {
                        System.err.println("Expecting task id " + validationTraceId + " but found " +  compareToTraceId);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private static class CallableWithTraceId implements Callable<Boolean> {
        private Random random = new Random();
        private String validationTraceId;
        
        /**
         * Trace id to be checked against, if no traceId check is to be performed, use null
         * @param traceId
         */
        public CallableWithTraceId(String traceId) {
            this.validationTraceId = traceId;
        }
        
        @Override
        public Boolean call() throws Exception {
            try {
                Trace.createEntryEvent("my-callable").report();
                try {
                    Thread.sleep(random.nextInt(10) + 1);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Trace.createExitEvent("my-callable").report();
                
                if (validationTraceId != null) {
                    String compareToTraceId = Trace.getCurrentXTraceID();
                    
                    boolean isSameTask = isSameTask(validationTraceId, compareToTraceId);
                    if (!isSameTask) {
                        System.err.println("Expecting task id " + validationTraceId + " but found " +  compareToTraceId);
                    }
                    return isSameTask;
                } else {
                    return true;
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }
    
    private static boolean isSameTask(String sourceTraceId, String compareToTraceId) {
        final int COMPARE_LENGTH = 10; //simply compare the prefix up to n
        return sourceTraceId.substring(0, COMPARE_LENGTH).equals(compareToTraceId.substring(0, COMPARE_LENGTH));
    }
}
