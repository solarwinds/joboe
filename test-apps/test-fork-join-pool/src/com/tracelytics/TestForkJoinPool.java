package com.tracelytics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;

import com.appoptics.api.ext.AgentChecker;
import com.appoptics.api.ext.Trace;
import com.appoptics.api.ext.TraceEvent;

public class TestForkJoinPool {
    static Map<String, String> traceIds = new HashMap<String, String>();
    private static final int testCount = 3;
    private static final int setSize = 100;

    private static final Random random = new Random();

    public static void main(String[] args) throws Exception {
        AgentChecker.waitUntilAgentReady(5, TimeUnit.SECONDS);
        testContextLeak();
        testContextPropagation();
    }

    private static void testContextPropagation() throws Exception {
        testExecuteTask();
        testExecuteRunnable();
        testSubmitRunnable();
        testSubmitCallable();
        testSubmitTask();
        testInvokeTask();
        testInvokeAll();
        testInvokeAny();
    }
    
    private static interface Consumer<T1, T2> { //run on jdk 1.7
        void accept(T1 t1, T2 t2);
    }
    
    private static void testContextPropagationByFunction(final String testName, final Consumer<ForkJoinPool, ForkJoinTask<Integer>> consumer) throws Exception {
        // Create ForkJoinPool using the default constructor.
        final ForkJoinPool pool = new ForkJoinPool();
        // Create three FolderProcessor tasks. Initialize each one with a different folder path.
        // Execute the three tasks in the pool using the execute() method.

        final List<ForkJoinTask<Integer>> tasks = new ArrayList<ForkJoinTask<Integer>>();

        for (int i = 0; i < testCount; i++) {
            final int runNum = i;
            List<Integer> numbers = generateNumbers(setSize);
            final AdditionProcessor task = new AdditionProcessor(numbers, null);
            tasks.add(task);

            new Thread() {
                public void run() {
                    String spanName = testName + "-" + runNum;
                    Trace.startTrace(spanName).report();
                    task.setTraceMetadata(Trace.getCurrentXTraceID());
                    consumer.accept(pool, task);
                    System.out.println("Result: " + task.join());
                    Trace.endTrace(spanName);
                }
            }.start();
        }
        
        for (ForkJoinTask<Integer> task : tasks) {
            task.get();
        }
        
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);
    }


    private static void testExecuteTask() throws Exception {
        testContextPropagationByFunction("test-execute-task", new Consumer<ForkJoinPool, ForkJoinTask<Integer>>() {
            @Override
            public void accept(ForkJoinPool pool, ForkJoinTask<Integer> task) {
                pool.execute(task);
            }
        });
    }

    private static void testExecuteRunnable() throws Exception {
        testContextPropagationByFunction("test-execute-runnable", new Consumer<ForkJoinPool, ForkJoinTask<Integer>>() {
            @Override
            public void accept(ForkJoinPool pool, final ForkJoinTask<Integer> task) {
                pool.execute(new Runnable() {
                    public void run() {
                        task.invoke();
                    }
                });
            }
        });
    }
    
    private static void testSubmitRunnable() throws Exception {
        testContextPropagationByFunction("test-submit-runnable", new Consumer<ForkJoinPool, ForkJoinTask<Integer>>() {
            @Override
            public void accept(ForkJoinPool pool, final ForkJoinTask<Integer> task) {
                pool.submit(new Runnable() {
                    public void run() {
                        task.invoke();
                    }
                });
            }
        });
    }
    
    private static void testSubmitCallable() throws Exception {
        testContextPropagationByFunction("test-submit-callable", new Consumer<ForkJoinPool, ForkJoinTask<Integer>>() {
            @Override
            public void accept(ForkJoinPool pool, final ForkJoinTask<Integer> task) {
                pool.submit(new Callable<Integer>() {
                    public Integer call() {
                        return task.invoke();
                    }
                });
            }
        });
    }
    
  
    private static void testSubmitTask() throws Exception {
        testContextPropagationByFunction("test-submit-callable", new Consumer<ForkJoinPool, ForkJoinTask<Integer>>() {
            @Override
            public void accept(ForkJoinPool pool, final ForkJoinTask<Integer> task) {
                pool.submit(task);
            }
        });
    }
    
    private static void testInvokeTask() throws Exception {
        testContextPropagationByFunction("test-invoke-task", new Consumer<ForkJoinPool, ForkJoinTask<Integer>>() {
            @Override
            public void accept(ForkJoinPool pool, final ForkJoinTask<Integer> task) {
                pool.invoke(task);
            }
        });
    }
    
    private static void testInvokeAll() throws Exception {
        testContextPropagationByFunction("test-invoke-all", new Consumer<ForkJoinPool, ForkJoinTask<Integer>>() {
            @Override
            public void accept(ForkJoinPool pool, final ForkJoinTask<Integer> task) {
                pool.invokeAll(Collections.singleton(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        return task.invoke();
                    }
                }));
            }
        });
    }
    
    private static void testInvokeAny() throws Exception {
        testContextPropagationByFunction("test-invoke-any", new Consumer<ForkJoinPool, ForkJoinTask<Integer>>() {
            @Override
            public void accept(ForkJoinPool pool, final ForkJoinTask<Integer> task) {
                try {
                    pool.invokeAny(Collections.singleton(new Callable<Integer>() {
                        @Override
                        public Integer call() throws Exception {
                            return task.invoke();
                        }
                    }));
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }

    private static void testContextLeak() throws InterruptedException {
        final ForkJoinPool pool = new ForkJoinPool();

        AdditionProcessor initTask = new AdditionProcessor(generateNumbers(setSize), null);
        Trace.startTrace("test").report();
        initTask.setTraceMetadata(Trace.getCurrentXTraceID());
        pool.invoke(initTask); // if context is not properly handled, then it will leak to the worker threads here
        Trace.endTrace("test");

        AdditionProcessor noContextTask = new AdditionProcessor(generateNumbers(setSize), null);
        pool.invoke(noContextTask); // run another fork join task here with no context, ensure there's no left over context from previous invoke

        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

    }

    private static boolean areAllTasksDone(List<ForkJoinTask<Integer>> tasks) {
        for (ForkJoinTask<Integer> task : tasks) {
            if (!task.isDone()) {
                return false;
            }
        }
        return true;
    }

    private static List<Integer> generateNumbers(int setSize) {
        List<Integer> numbers = new ArrayList<Integer>();
        for (int i = 0; i < setSize; i++) {
            numbers.add(random.nextInt(10));
        }
        return numbers;
    }

    private static boolean isSameTrace(String sourceMetadata, String compareToMetadata) {
        return getTraceId(sourceMetadata).equals(getTraceId(compareToMetadata));
    }

    private static String getTraceId(String metadata) {
        final int COMPARE_LENGTH = 10; // simply compare the prefix up to n
        return metadata.substring(0, COMPARE_LENGTH);
    }

    private static class AdditionProcessor extends RecursiveTask<Integer> {

        private List<Integer> numbers;
        private String traceMetadata;
        private static final int BATCH_SIZE = 5;

        // Implement the constructor of the class to initialize its attributes
        public AdditionProcessor(List<Integer> numbers, String traceMetadata) {
            this.numbers = numbers;
            this.traceMetadata = traceMetadata;
        }

        public void setTraceMetadata(String traceMetadata) {
            this.traceMetadata = traceMetadata;
        }
        
        @Override
        protected Integer compute() {
            try {

                if (traceMetadata != null) { // make sure taskID is same
                    TraceEvent event = Trace.createEntryEvent("compute");
                    event.addInfo("Size", numbers.size());
                    event.addInfo("ThreadID", Thread.currentThread().getId());
                    event.report();

                    if (!TestForkJoinPool.isSameTrace(traceMetadata, Trace.getCurrentXTraceID())) {
                        System.err.println("Inconsistent trace id! expected " + TestForkJoinPool.getTraceId(traceMetadata) + " found "
                                + TestForkJoinPool.getTraceId(Trace.getCurrentXTraceID()));
                    } else {
                        // System.out.println("OK: " + Trace.getCurrentXTraceID());
                    }
                } else { // make sure no context leak
                    if (!Trace.getCurrentXTraceID().startsWith("2B0000000")) {
                        System.err.println("Context leak! Found " + Trace.getCurrentXTraceID());
                    }
                }

                if (numbers.size() > BATCH_SIZE) {
                    AdditionProcessor task1 = new AdditionProcessor(numbers.subList(0, numbers.size() / 2), traceMetadata);
                    AdditionProcessor task2 = new AdditionProcessor(numbers.subList(numbers.size() / 2, numbers.size()), traceMetadata);

                    ForkJoinTask<Integer> fork1 = task1.fork();
                    ForkJoinTask<Integer> fork2 = task2.fork();

                    return fork1.join() + fork2.join();
                } else {
                    int sum = 0;
                    for (int number : numbers) {
                        sum += number;
                    }
                    return sum;
                }
            } finally {
                if (traceMetadata != null) {
                    Trace.createExitEvent("compute").report();
                }
            }
        }
    }
}
