package com.solarwinds.joboe.core;

import com.solarwinds.joboe.core.Context;
import com.solarwinds.joboe.core.Event;
import com.solarwinds.joboe.core.ReporterFactory;
import com.solarwinds.joboe.core.TestReporter;
import com.solarwinds.joboe.core.config.InvalidConfigException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestReporterTest {

    @Test
    public void testReporterSameThread() throws InvalidConfigException {
        final TestReporter threadLocalReporter = ReporterFactory.getInstance().createTestReporter(true);
        final TestReporter nonThreadLocalReporter = ReporterFactory.getInstance().createTestReporter(false);
        
        Event event;
        
        event = Context.startTrace();
        event.report(threadLocalReporter);
        
        event = Context.startTrace();
        event.report(nonThreadLocalReporter);
        
        
        assertEquals(1, threadLocalReporter.getSentEvents().size());
        assertEquals(1, nonThreadLocalReporter.getSentEvents().size());
    }

    @Test
    public void testReporterDifferentThread() throws InvalidConfigException, InterruptedException {
        final TestReporter threadLocalReporter = ReporterFactory.getInstance().createTestReporter(true);
        final TestReporter nonThreadLocalReporter = ReporterFactory.getInstance().createTestReporter(false);
        
        Thread thread = new Thread(() -> {
            Event event;

            event = Context.startTrace();
            event.report(threadLocalReporter);

            event = Context.startTrace();
            event.report(nonThreadLocalReporter);
        });
        
        thread.start();
        thread.join();
        
        
        assertEquals(0, threadLocalReporter.getSentEvents().size()); //different thread, should not get the event
        assertEquals(1, nonThreadLocalReporter.getSentEvents().size());
    }
}
