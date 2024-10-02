package com.tracelytics.test.struts;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.opensymphony.xwork2.ActionSupport;

/**
 * This is a test Struts action.  It extends ActionSupport, which implements the Action interface.
 */
public class TestThreadpoolAction extends ActionSupport {
    private static final int RUN_COUNT = 200;
    public String execute() {
        ExecutorService service = Executors.newCachedThreadPool();
        for (int i = 0 ; i < RUN_COUNT ; i ++) {
            service.submit(() -> {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });
        }
        
        service.shutdown();
        try {
            service.awaitTermination(100, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return SUCCESS;
    }
}
