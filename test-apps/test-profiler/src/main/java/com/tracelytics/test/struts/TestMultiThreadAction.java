package com.tracelytics.test.struts;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.opensymphony.xwork2.ActionSupport;

/**
 * This is a test Struts action.  It extends ActionSupport, which implements the Action interface.
 */
public class TestMultiThreadAction extends ActionSupport {
    private static final int THREAD_COUNT = 1000;
    private static final ExecutorService SERVICE = Executors.newFixedThreadPool(THREAD_COUNT);
    public String execute() {
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0 ; i < 1000 ; i ++) {
            futures.add(SERVICE.submit(() -> { 
                RecursiveComputation recursiveComputation = new RecursiveComputation();
                recursiveComputation.start();
            }));
        }
        
        for (int i = 0 ; i < 1000 ; i ++) {
            try {
                futures.get(i).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        
        
//        System.out.println(recursiveComputation.sum.longValue());
        return SUCCESS;
    }
    
    private static final class RecursiveComputation {
        private final AtomicInteger runCount = new AtomicInteger();
        private static final int MAX_RUN_COUNT = 40;
        private static final int BIG_INTEGER_LENGTH = 1024;
        private Random random = new Random();
        private BigInteger sum = BigInteger.ZERO;

        private void start() {
            if (runCount.incrementAndGet() <= MAX_RUN_COUNT) {
                start();
            }
            BigInteger result = new BigInteger(BIG_INTEGER_LENGTH, random).multiply(new BigInteger(BIG_INTEGER_LENGTH, random));
            sum = sum.add(result);
        }
      
    }
    
}
