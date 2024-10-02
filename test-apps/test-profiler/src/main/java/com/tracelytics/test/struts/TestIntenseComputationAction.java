package com.tracelytics.test.struts;

import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.opensymphony.xwork2.ActionSupport;

/**
 * This is a test Struts action.  It extends ActionSupport, which implements the Action interface.
 */
public class TestIntenseComputationAction extends ActionSupport {
    private static final int DEFAULT_MAX_RUN_COUNT = 40;
    public int maxRunCount = DEFAULT_MAX_RUN_COUNT;
    public int threadCount = 1;

    public String execute() {
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount - 1; i ++) { //submit threads, to test impact on the snapshot taking
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    RecursiveComputation recursiveComputation = new RecursiveComputation();
                    recursiveComputation.start();
                }
            });
        }

        //submit one to the main thread - so the stacktrace is filled, this is the one that get profiled
        RecursiveComputation recursiveComputation = new RecursiveComputation();
        recursiveComputation.start();


//        System.out.println(recursiveComputation.sum.longValue());

        executorService.shutdown();
        try {
            executorService.awaitTermination(100, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return SUCCESS;
    }
    
    
    
    private final class RecursiveComputation {
        private final AtomicInteger runCount = new AtomicInteger();
        private static final int BIG_INTEGER_LENGTH = 102400;
        private Random random = new Random();
        private BigInteger sum = BigInteger.ZERO;

        private void start() {
            if (runCount.incrementAndGet() <= maxRunCount) {
                start();
            }
            BigInteger result = new BigInteger(BIG_INTEGER_LENGTH, random).multiply(new BigInteger(BIG_INTEGER_LENGTH, random));
            sum = sum.add(result);
        }
      
    }
    
}
