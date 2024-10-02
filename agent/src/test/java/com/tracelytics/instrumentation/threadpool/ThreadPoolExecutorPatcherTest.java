package com.tracelytics.instrumentation.threadpool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.tracelytics.instrumentation.AbstractInstrumentationTest;
import com.tracelytics.joboe.Context;

public class ThreadPoolExecutorPatcherTest extends AbstractInstrumentationTest<ThreadPoolExecutorPatcher>{
    public void testThreadPools() throws InterruptedException, ExecutionException {
        runTestOnService(Executors.newCachedThreadPool(), Validation.PROPAGATION, Validation.NO_LEAK);
        runTestOnService(Executors.newFixedThreadPool(10), Validation.PROPAGATION, Validation.NO_LEAK);
        runTestOnService(Executors.newSingleThreadExecutor(), Validation.PROPAGATION, Validation.NO_LEAK);
        runTestOnService(Executors.newScheduledThreadPool(10), Validation.NO_LEAK); // known that no context is propagated for ScheduleThreadPool, only check for leaks
        runTestOnService(Executors.newSingleThreadScheduledExecutor(), Validation.NO_LEAK); // known that no context is propagated for ScheduleThreadPool, only check for leaks
    }
    
    public enum Validation { PROPAGATION, NO_LEAK }
    
    private void runTestOnService(ExecutorService executorService, Validation... validationsArray) throws InterruptedException, ExecutionException {
        final List<Validation> validations = Arrays.asList(validationsArray);
        Context.getMetadata().randomize(); //create a valid context
        
        final String firstTask = Context.getMetadata().taskHexString();
        
        List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
        
        for (int i = 0 ; i < 100; i++) {
            futures.add(executorService.submit(new Callable<Boolean>() {
                public Boolean call() {
                    return firstTask.equals(Context.getMetadata().taskHexString());
                }
            }));
        }
        
        Context.clearMetadata();
        
        Context.getMetadata().randomize(); //create 2nd valid context
        
        final String secondTask = Context.getMetadata().taskHexString();
        for (int i = 0 ; i < 100; i++) {
            futures.add(executorService.submit(new Callable<Boolean>() {
                public Boolean call() {
                    return secondTask.equals(Context.getMetadata().taskHexString());
                }
            }));
        }
        
        Context.clearMetadata();

        if (validations.contains(Validation.PROPAGATION)) {
            for (Future<Boolean> future : futures) {
                assertTrue(future.get());
            }
        }
        
        Future<Boolean> future = executorService.submit(new Callable<Boolean>() {
            public Boolean call() {
                //this should NOT have context inside. as it is cleared. This makes sure there's no stale context lingers in the thread
                return !Context.getMetadata().isValid();
            }
        });
        
        if (validations.contains(Validation.NO_LEAK)) {
            assertTrue(future.get());
        }
        
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
    }
}
