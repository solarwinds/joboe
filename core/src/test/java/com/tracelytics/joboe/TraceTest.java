package com.tracelytics.joboe;

import org.junit.jupiter.api.Test;

public class TraceTest {
    /* Make sure we can report a trace: generate several events with multiple layers */
    @Test
    public void testTrace() 
        throws Exception {
        EventReporter reporter = ReporterFactory.getInstance().buildUdpReporter();

        Event event = Context.startTrace();
        event.addInfo("Layer", "JavaTest",
                      "Label", "entry");
        event.report(reporter);

        Thread.sleep(10);

        event = Context.createEvent();
        event.addInfo("Layer", "JavaTest_2",
                      "Label", "entry");
        event.report(reporter);

        Thread.sleep(20);

        event = Context.createEvent();
        event.addInfo("Layer", "JavaTest_2",
                      "Label", "exit");
        event.report(reporter);
        
        Thread.sleep(10);
        
        event = Context.createEvent();
        event.addInfo("Layer", "JavaTest_3",
                      "Label", "entry");
        event.report(reporter);

        Thread.sleep(20);
        
        event = Context.createEvent();
        event.addInfo("Layer", "JavaTest_4",
                      "Label", "entry");
        event.report(reporter);

        Thread.sleep(20);

        event = Context.createEvent();
        event.addInfo("Layer", "JavaTest_4",
                      "Label", "exit");
        event.report(reporter);
        
        Thread.sleep(40);

        event = Context.createEvent();
        event.addInfo("Layer", "JavaTest_3",
                      "Label", "exit");
        event.report(reporter);
        
        Thread.sleep(50);

        event = Context.createEvent();
        event.addInfo("Layer", "JavaTest",
                      "Label", "exit");
        event.report(reporter);
        
        Context.clearMetadata();
    }
    // XXX: Some way to check these were actually processed
}
