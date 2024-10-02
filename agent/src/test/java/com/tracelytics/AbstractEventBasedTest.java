package com.tracelytics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.tracelytics.ExpectedEvent;
import com.tracelytics.ValueValidator;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.JoboeTest;
import com.tracelytics.joboe.TestReporter.DeserializedEvent;
import com.tracelytics.joboe.span.impl.Tracer;

public abstract class AbstractEventBasedTest extends JoboeTest {
    //private static Class<?> modifiedClass;
    //private static EventReporter reporter;
//    private T instrumentation;
    
    private static final int MAX_STRING_DISPLAY_LENGTH = 1000;
    
//    protected AbstractInstrumentationTest(T instrumentation) {
//        this.instrumentation = instrumentation;
//    }
    
    @Override
    protected void setUp() throws Exception {
        //Event event = Context.startTrace(); //initialize the metadata
        //add a base span
        Tracer.INSTANCE.buildSpan("dummy").startActive();
        Context.getMetadata().randomize(); //make it valid so Span.context().isTraced() returns true
    }
    
    protected void assertEvents(List<ExpectedEvent> expectedEvents) {
        List<DeserializedEvent> sentEvents = tracingReporter.getSentEvents();
        assertEvents(expectedEvents, sentEvents);
    }
    
    protected void assertEvents(List<ExpectedEvent> expectedEvents, List<DeserializedEvent> actualEvents) {
    	assertEquals(expectedEvents.size(), actualEvents.size());
        
        for (int i = 0; i < expectedEvents.size(); i++) {
            ExpectedEvent expectedEvent = expectedEvents.get(i);
            DeserializedEvent sentEvent = actualEvents.get(i);
            
            assertEvent(expectedEvent, sentEvent);
        }
    }
    
    protected void assertEvent(ExpectedEvent expectedEvent, DeserializedEvent actualEvent) {
//        Set<Entry<String, Object>> expectedEntries = TestingUtil.getEntriesFromEvent(expectedEvent);
        
        Map<String, Object> sentEntries = actualEvent.getSentEntries();
        Map<String, Object> expectedEntries = expectedEvent.getExpectedEntries();
        
        //make sure all the expectedEntries entries have same values
        for (Entry<String, Object> expectedEntry : expectedEntries.entrySet()) {
            String key = expectedEntry.getKey();
            
            Object expectedValue = expectedEntry.getValue();
            Object sentValue = sentEntries.get(key);
//            assertEquals("Sent event value does not match the expected value of key [" + key + "] in the event with index [" + i + "]",
//                         expectedValue, 
//                         sentValue);  
            //do not use assertEqual, if the object (for example string) is too long, it might trigger output problem if the test case fails
          
            String expectedValueString;
            
            if (expectedValue == null) {
                expectedValueString = null;
            } else if (expectedValue instanceof ValueValidator) {
                expectedValueString = ((ValueValidator<?>)expectedValue).getValueString();
            } else {
                expectedValueString = expectedValue.toString();
            }
            
            String sentValueString = sentValue != null ? sentValue.toString() : "null";

            boolean isValid;
            if (expectedValue == null) {
                isValid = (sentValue == null);
            } else if (expectedValue instanceof ValueValidator) {
                isValid = ((ValueValidator)expectedValue).isValid(sentValue);
            } else {
                isValid = expectedValue.equals(sentValue);
            }
            
            assertTrue("Sent event value does not match the expected value of key [" + key + "] in the event, expected [" + truncateString(expectedValueString) + "] found [" + truncateString(sentValueString) + "]",
                       isValid);
        }
    }
    
    
    private String truncateString(String value) {
        if (value != null) {
            if (value.length() > MAX_STRING_DISPLAY_LENGTH) {
                StringBuffer truncatedString = new StringBuffer(value.substring(0, MAX_STRING_DISPLAY_LENGTH));
                truncatedString.append("(truncated " + (value.length() - MAX_STRING_DISPLAY_LENGTH) + " character(s)");
                
                return truncatedString.toString();
            } else {
                return value;
            }
        } else {
            return null;
        }
    }
    
    protected void resetReporter() {
        tracingReporter.reset();
    }
}
