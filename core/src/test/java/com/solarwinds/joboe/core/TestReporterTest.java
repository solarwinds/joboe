package com.solarwinds.joboe.core;

import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.sampling.Metadata;
import com.solarwinds.joboe.sampling.SamplingConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestReporterTest {

    @BeforeEach
    void setup(){
        Metadata.setup(SamplingConfiguration.builder().build());
    }

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
