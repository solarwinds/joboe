/**
 * Tests the public API for starting a trace and instrumenting a non-web application,
 * passes Context between threads.
 */

package com.tracelytics.test;

import java.util.Random; 
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.appoptics.api.ext.*;

public class TestContextAPI {


    /* Starts tracing, sending start/end events, calls into another layer */
    public void run() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);

        TraceEvent event = Trace.startTrace("TestContextAPI");

        event.addInfo("URL","/test/context/api3");
        event.report();

        Thread thr = null;

        // Save the current context This is done so all of the 'app_thread_queue' events can have the
        // same edge event. 
        TraceContext beforeThreadQueueCtx = TraceContext.getDefault();

        // Kick off some threads... Note that the parent context (ctx) is accessed from within the thread.
        for(int i=0;i<16;i++) {
            beforeThreadQueueCtx.setAsDefault();
            event = Trace.createEntryEvent("app_thread_queue");
            //event.setAsync();
            event.report();

            final TraceContext ctx = TraceContext.getDefault();

            pool.execute(new Runnable() {
                public void run() {
                    try {
                        // Set context : taken from parent
                        ctx.setAsDefault();

                        // Send end event - tracks when thread actually started running
                        TraceEvent event = Trace.createExitEvent("app_thread_queue");
                        event.setAsync();
                        event.report();

                        // "Work":
                        Thread.sleep(new Random().nextInt(1000)+500);

                        // Clear context - needed for when thread is recycled:
                        TraceContext.clearDefault();
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }

                }
            });

        }

        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);
        beforeThreadQueueCtx.setAsDefault();
        String xTrace = Trace.endTrace("TestContextAPI");
        System.out.println("End trace: " + xTrace);
    }



    public static void main(String args[]) throws Exception {
        TestContextAPI test = new TestContextAPI();
        test.run();
    }
}
