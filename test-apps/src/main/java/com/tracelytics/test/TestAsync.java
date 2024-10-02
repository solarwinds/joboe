/**
 * Tests the public API for starting a trace and instrumenting a non-web application,
 * continuing that trace into another layer (simulated by using another thread.)
 */

package com.tracelytics.test;

import com.appoptics.api.ext.*;
import java.util.Random; 

public class TestAsync {


    /* Starts tracing, sending start/end events, calls into another layer */
    public void run() throws Exception {
        TraceEvent event = Trace.startTrace("TestAsyncApp");

        event.addInfo("URL","/async/app");
        event.report();

        event = Trace.createEntryEvent("app_layer");
        event.report();

        Thread thr = null;
        AppAsyncThread app1 = null;

        // Start some threads. Note that the parent context (XTraceID) is automatically inherited...
        for(int i=0;i<3;i++) {
            app1 = new AppAsyncThread("app_thread");
            thr = new Thread(app1);
            thr.start();
        }

        thr.join();
        Thread.sleep(2500);

        event = Trace.createExitEvent("app_layer");
        event.report();

        String xTrace = Trace.endTrace("TestAsyncApp");
        System.out.println("End trace: " + xTrace);
    }



    public static void main(String args[]) throws Exception {
        TestAsync test = new TestAsync();
        test.run();
    }
}

// Our fake app server thread: takes in an xtrace ID and returns an edge xtrace id
// Normally this would be passed through an RPC protocol, etc.
class AppAsyncThread implements Runnable {

    private String layerName;
    private String edge = null;

    public AppAsyncThread(String layerName) {
        this.layerName = layerName;
    }

    public void run() {
        try {
            TraceEvent event = Trace.createEntryEvent(layerName);
            event.setAsync();
            event.report();

            Random rand = new Random();
            Thread.sleep(rand.nextInt(3000)+100);

            // Only ends the trace for this thread (clears the context...)
            Trace.endTrace(layerName);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
