/**
 * Tests the public API for starting a trace and instrumenting a non-web application,
 * continuing that trace into another layer (simulated by using another thread.)
 * Shows how we can add multiple edges to an event.
 */

package com.tracelytics.test;

import com.appoptics.api.ext.*;
 

public class TestContinue {


    /* Starts tracing, sending start/end events, calls into another layer */
    public void run() throws Exception {
        TraceEvent event = Trace.startTrace("TestApp");

        event.addInfo("URL","/first/layer",
                      "Controller", "testController",
                      "Action", "testAction");

        event.addInfo("Key1", "Val1");
        event.report();

        event = Trace.createEntryEvent("layer1");
        event.report();

        event = Trace.createExitEvent("layer1");
        // Simulates call into another layer. X-TraceID would presumably be passed over HTTP or an RPC call, etc.
        AppThread app1 = new AppThread("app1", Trace.getCurrentXTraceID());
        Thread thr = new Thread(app1);
        thr.start();
        thr.join();

        event.addEdge(app1.getEdge());

        // Simulate a call to another app server...
        AppThread app2 = new AppThread("app2", Trace.getCurrentXTraceID());
        thr = new Thread(app2);
        thr.start();
        thr.join();

        event.addEdge(app2.getEdge());
        event.report();

        String xTrace = Trace.endTrace("TestApp", "key2", "test2");
        System.out.println("End trace (layer 1): " + xTrace);
    }


    public static void main(String args[]) throws Exception {
        TestContinue test = new TestContinue();
        test.run();
    }
}

// Our fake app server thread: takes in an xtrace ID and returns an edge xtrace id
// Normally this would be passed through an RPC protocol, etc.
class AppThread implements Runnable {

    private String layerName;
    private String xTraceID;
    private String edge = null;

    public AppThread(String layerName, String xTraceID) {
        this.layerName = layerName;
        this.xTraceID = xTraceID;
    }

    public String getEdge() {
        return edge;
    }

    public void run() {
        try {
            TraceEvent event = Trace.continueTrace(layerName, xTraceID);
            event.report();

            Thread.sleep(2000);

            edge = Trace.endTrace(layerName, "key2", "test2");
            System.out.println("End trace (layer 2): " + edge);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
