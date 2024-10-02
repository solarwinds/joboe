package com.tracelytics.joboe.span.impl;

import junit.framework.TestCase;

/**
 * Test the extra methods provided by ActiveSpan compared to Span
 * @author pluk
 *
 */
public class ScopeTest extends TestCase {
	public void testContinuation() {
//        final int START_TIME = 1;
//        Metadata metadata = new Metadata();
//        metadata.randomize();
//
//        Map<String, String> baggages = new HashMap<String, String>();
//        baggages.put("baggage1", "1");
//        baggages.put("baggage2", "2");
//
//        SpanContext spanContext = new SpanContext(metadata, 0l, 0l, 0l, (byte) 0, baggages, null);
//
//        Map<String, Object> tags = new HashMap<String, Object>();
//        tags.put("tag1", 1);
//        tags.put("tag2", "2");
//        tags.put("tag3", 3.0);
//
//
//        Span span = new Span(Tracer.INSTANCE, "test", spanContext, START_TIME, tags, Collections.EMPTY_LIST);
//        span.setTag("boolean", true);
//        span.setTag("number", 5);
//        span.setTag("string", "");
//        Object object = new Object();
//        span.setTagAsObject("object", object);
//
//
//        Scope activeScope = new Scope(span, true);
//        Scope activatedScope = activeScope.capture().activate();
//
//        //activated span should be considered equal to the original span
//        assertEquals(activeScope.span(), activatedScope.span());
//        //but they should not be the same instance
//        assertNotSame(activeScope, activatedScope);
//
//        Span activatedSpan = activatedScope.span();
//        assertEquals(1, activatedSpan.getTags().get("tag1"));
//        assertEquals("2", activatedSpan.getTags().get("tag2"));
//        assertEquals(3.0, activatedSpan.getTags().get("tag3"));
//        assertEquals(true, activatedSpan.getTags().get("boolean"));
//        assertEquals(5, activatedSpan.getTags().get("number"));
//        assertEquals("", activatedSpan.getTags().get("string"));
//        assertEquals(object, activatedSpan.getTags().get("object"));
    }
	
	
}
