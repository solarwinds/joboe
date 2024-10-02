/**
 * Tests the public API for starting a trace and instrumenting a non-web application
 */

package com.tracelytics.test;

import com.appoptics.api.ext.*;
 

public class TestNonWebApp {


    /* Starts tracing, sending start/end events, calls into another layer */
    public void run() throws Exception {
        TraceEvent event = Trace.startTrace("TestApp");

        event.addInfo("URL","/this/is/a/test",
                      "Controller", "testController",
                      "Action", "testAction");

        event.addInfo("Key1", "Val1");
        event.report();

        layer1();
        error();

        String xTrace = Trace.endTrace("TestApp", "key2", "test2");
        System.out.println("End trace: " + xTrace);
    }


    /* Layer that explitly sends an entry, info, and exit event */
    private void layer1() throws Exception {

        TraceEvent event = Trace.createEntryEvent("layer1");
        event.report();

        for(int i=0;i<5;i++) {
            checkA();
            checkB();
        }
        checkC();

 
        event = Trace.createInfoEvent("layer1");
        event.addInfo("Key2", "Val2");
        event.report();

        event = Trace.createExitEvent("layer1");
        event.report();
    }

    // Test annotations:
    @LogMethod(layer="checkA")
    private void checkA() throws Exception {
        Thread.sleep(100);
    }

    @LogMethod(layer="checkB")
    private void checkB() throws Exception {
        for(int i=0;i<10;i++) {
            calc();
        }
    }

    @LogMethod(layer="checkC")
    public void checkC() throws Exception {
        calc();
    }

    @ProfileMethod(profileName="calc")
    private void calc() throws Exception {
        Thread.sleep(10);
    }


    private void error() {
        try {
            throw new RuntimeException("An error occurred.");
        } catch(Exception ex) {
            Trace.logException(ex);
        }
    }

    public static void main(String args[]) throws Exception {
        TestNonWebApp test = new TestNonWebApp();
        test.run();
    }
}
