package com.tracelytics.instrumentation;

import java.util.List;

import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.EventValueConverter;
import com.tracelytics.joboe.JoboeTest;
import com.tracelytics.joboe.TestReporter.DeserializedEvent;

public class ClassInstrumentationAnnotationsTest extends JoboeTest {
		
	EventValueConverter converter = new EventValueConverter();
	int MAX_VALUE_LENGTH = converter.maxValueLength;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		Context.startTrace(); //create a valid context
		tracingReporter.reset();
	}
	
	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		Context.clearMetadata();
		tracingReporter.reset();
	}
	
    public void testLogMethodReturnLongString() throws Exception {
    	String longString = new String(new byte[10000]);
        int truncateCount = longString.length() - MAX_VALUE_LENGTH;
        String finalString = longString.substring(0, MAX_VALUE_LENGTH) +  "...(" + truncateCount + " characters truncated)";
        
        SdkAnnotationInstrumentation.logMethodExit("LongString", true, longString);
        List<DeserializedEvent> events = tracingReporter.getSentEvents();

        assertEquals(1, events.size());
        
        DeserializedEvent event = events.get(0);
        assertEquals(finalString, event.getSentEntries().get("ReturnValue"));
    }
    
    public void testProfileMethodReturnLongString() throws Exception {
    	String longString = new String(new byte[10000]);
        int truncateCount = longString.length() - MAX_VALUE_LENGTH;
        String finalString = longString.substring(0, MAX_VALUE_LENGTH) +  "...(" + truncateCount + " characters truncated)";
        
        SdkAnnotationInstrumentation.profileMethodExit("LongStringProfile", true, longString);
        List<DeserializedEvent> events = tracingReporter.getSentEvents();

        assertEquals(1, events.size());
        
        DeserializedEvent event = events.get(0);
        assertEquals(finalString, event.getSentEntries().get("ReturnValue"));
    }
}
